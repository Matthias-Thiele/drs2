/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.io.Connector;

/**
 * Diese Klasse kapselt die Definition und 
 * Verwendung eines Signals.
 * @author pi
 */
public class Signal implements ColorMarker, TastenEvent {

    private Connector conn;
    private String name;
    private int sigFahrt;
    private int sigHalt;
    private int vorsigFahrt;
    private int vorsigHalt;
    private int fahrwegWhite;
    private int fahrwegRed;
    
    private boolean isFahrt = false;
    private boolean isSh1 = false;
    private int fahrwegMarker = -1;
    private Doppeltaster sigTaste;
    private Doppeltaster sh1Taste;
    private Config config;
    private boolean isGestoert;
    private int sh1Lampe;
    private int sh1WPlus;
    private int sh1WMinus;
    
    /**
     * Zur Initialisierung wird der PortEpander Connector
     * und die Nummern der verwendeten Anzeigelampen übergeben.
     * 
     * @param config
     * @param name
     * @param sigTaste
     * @param sigFahrt
     * @param sigHalt
     * @param vorsigFahrt
     * @param vorsigHalt 
     * @param fahrwegWhite 
     * @param fahrwegRed 
     * @param sh1Lampe 
     * @param sh1WPlus 
     * @param sh1WMinus 
     */
    public void init(Config config, String name, int sigTaste, int sigFahrt, int sigHalt, 
            int vorsigFahrt, int vorsigHalt, int fahrwegWhite, int fahrwegRed,
            int sh1Lampe, int sh1WPlus, int sh1WMinus) {
        this.config = config;
        this.name = name;
        this.conn = config.connector;
        this.sigTaste = new Doppeltaster();
        this.sigTaste.init(config, this, Const.HaGT, sigTaste);
        this.sigFahrt = sigFahrt;
        this.sigHalt = sigHalt;
        this.vorsigFahrt = vorsigFahrt;
        this.vorsigHalt = vorsigHalt;
        this.fahrwegWhite = fahrwegWhite;
        this.fahrwegRed = fahrwegRed;
        this.sh1Lampe = sh1Lampe;
        this.sh1WPlus = sh1WPlus;
        this.sh1WMinus = sh1WMinus;
        if (sh1Lampe >= 0) {
            sh1Taste = new Doppeltaster();
            sh1Taste.init(config, this, Const.SGT, sigTaste);
        }
        halt();
    }
    
    /**
     * Stellt das Signal und Vorsignal auf Fahrt.
     * Da es sich um Licht- und nicht um mechanische
     * Signale handelt, gibt es keine Zeitverzögerung
     * bei der Anzeige.
     */
    public void halt() {
        isFahrt = false;
        updateView();
    }
    
    /**
     * Stellt das Signal und Vorsignal auf Halt.
     */
    public void fahrt() {
        isFahrt = true;
        updateView();
    }
    
    /**
     * Gibt an, ob das Signal auf Fahrt (hp1/ph2) steht.
     * @return 
     */
    public boolean isFahrt() {
        return isFahrt;
    }
    
    /**
     * Setzt das Flag welches eine Störung simuliert.
     * @param isGestoert 
     */
    public void setStoerung(boolean isGestoert) {
        this.isGestoert = isGestoert;
        updateView();
        
        if (isGestoert) {
            config.stoerungsmelder.stoerungS();
        }
    }
    
    /**
     * Meldet zurück, ob eine Störung vorliegt.
     * @return 
     */
    public boolean isGestoert() {
        return isGestoert;
    }
    
    /**
     * Gibt den aktuellen Zustand in Textform zurück.
     * @return 
     */
    @Override
    public String toString() {
        if (isGestoert) {
            return name + " (Gestört)";
        } else if (isSh1) {
            return name + ": sh1";
        } else {
            return name + ": " + (isFahrt ? "hp1/2" : "hp0");
        }
    }
    
    /**
     * Liefert den Namen des Signals zurück.
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Aktualisiert den Zustand der Anzeigelampen
     * gemäß der aktuellen Fahrt Einstellung.
     */
    private void updateView() {
        if (isGestoert) {
            conn.setOut(sigFahrt, false);
            conn.setOut(sigHalt, false);
            conn.setOut(vorsigFahrt, false);
            conn.setOut(vorsigHalt, false);            
            conn.setOut(sh1Lampe, false);            
        } else {
            conn.setOut(sh1Lampe, isSh1);
            conn.setOut(sigFahrt, isFahrt);
            conn.setOut(sigHalt, !isFahrt);
            conn.setOut(vorsigFahrt, isFahrt);
            conn.setOut(vorsigHalt, !isFahrt);
            if (vorsigFahrt == Const.WVp1) {
                // Sonderbehandlung für P1 mit zwei Vorsignalen
                conn.setOut(Const.Vp13, isFahrt);
            }
        }
        
        conn.setOut(fahrwegWhite, false);
        conn.setOut(fahrwegRed, false);
        if (fahrwegMarker >= 0) {
            conn.setOut(fahrwegMarker, true);
        }
    }

    @Override
    public boolean hasMarker() {
        return fahrwegMarker >= 0;
    }
    
    /**
     * Leuchtet den Fahrweg Marker weiss aus.
     */
    @Override
    public void white() {
        fahrwegMarker = this.fahrwegWhite;
        updateView();
    }

    /**
     * Leuchtet den Fahrweg Marker rot aus.
     */
    @Override
    public void red() {
        fahrwegMarker = this.fahrwegRed;
        updateView();
    }
    
    /**
     * Löscht den Fahrwerg Marker nach Abschluss der Fahrt.
     */
    @Override
    public void clear() {
        fahrwegMarker = -1;
        updateView();
    }

    /**
     * Falls eine Plus- oder Minusweiche hinter dem Sh1 Signal
     * definiert ist, prüfe ob diese in der richtigen Lage ist.
     * @return 
     */
    private boolean checkSh1Weiche() {
        if ((sh1WPlus >= 0) && !config.weichen[sh1WPlus].isPlus()) {
            return false;
        }
        
        if ((sh1WMinus >= 0) && config.weichen[sh1WMinus].isPlus()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Wenn die SGT Taste aktiv ist, wird das Signal auf SH1 gestellt.
     * Ist die HaGT aktiv und das Signal auf SH1, wird es auf SH0 
     * zurückgestellt. Ansonsten wird es auf HP0 zurückgestellt.
     * @param taste1
     * @param taste2 
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        if (taste1 == Const.HaGT) {
            if (isSh1) {
                isSh1 = false;
                config.alert("Signal " + name + " auf SH0 gesellt.");
            } else {
                halt();
                config.alert("Signal über HaGT auf HP0 gestellt.");
            }
        } else if (taste1 == Const.SGT) {
            if (checkSh1Weiche()) {
                isSh1 = true;
                config.alert("Signal " + name + " auf SH1 gesellt.");
            } else {
                config.alert("Die Weiche hinter dem Sh Signal " + name + " steht falsch.");
            }
        }
        
        updateView();
    }
}
