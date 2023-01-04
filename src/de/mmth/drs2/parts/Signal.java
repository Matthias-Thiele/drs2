/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
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
    private int fahrwegMarker = -1;
    private Doppeltaster sigTaste;
    private Config config;
    
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
     */
    public void init(Config config, String name, int sigTaste, int sigFahrt, int sigHalt, int vorsigFahrt, int vorsigHalt, int fahrwegWhite, int fahrwegRed) {
        this.config = config;
        this.name = name;
        this.conn = config.connector;
        this.sigTaste = new Doppeltaster();
        this.sigTaste.init(config, this, 20, sigTaste);
        this.sigFahrt = sigFahrt;
        this.sigHalt = sigHalt;
        this.vorsigFahrt = vorsigFahrt;
        this.vorsigHalt = vorsigHalt;
        this.fahrwegWhite = fahrwegWhite;
        this.fahrwegRed = fahrwegRed;
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
     * Gibt den aktuellen Zustand in Textform zurück.
     * @return 
     */
    @Override
    public String toString() {
        return name + ": " + (isFahrt ? "hp1/2" : "hp0");
    }
    
    /**
     * Aktualisiert den Zustand der Anzeigelampen
     * gemäß der aktuellen Fahrt Einstellung.
     */
    private void updateView() {
        conn.setOut(sigFahrt, isFahrt);
        conn.setOut(sigHalt, !isFahrt);
        conn.setOut(vorsigFahrt, isFahrt);
        conn.setOut(vorsigHalt, !isFahrt);
        
        conn.setOut(fahrwegWhite, false);
        conn.setOut(fahrwegRed, false);
        if (fahrwegMarker >= 0) {
            conn.setOut(fahrwegMarker, true);
        }
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

    @Override
    public void whenPressed(int taste) {
        halt();
        config.alert("Signal über HaGT auf HP0 gestellt.");
    }
}
