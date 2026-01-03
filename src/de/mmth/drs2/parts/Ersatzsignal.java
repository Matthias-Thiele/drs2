/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;

/**
 * Diese Klasse verwaltet die Ansteuerung der Ersatzsignale.
 * Diese werden durch einen Tastendruck gesetzt und nach 
 * 90 Sekunden automatisch wieder zurückgesetzt. Es gibt keine
 * weiteren Abhängigkeiten zu Fahrstraßen oder Signalen.
 * 
 * @author Matthias Thiele
 */
public class Ersatzsignal implements TastenEvent, TickerEvent {
    private final int FAHRT_DURATION = 600;
    
    private String name;
    private Doppeltaster taste;
    private Config conf;
    private boolean isFahrt = false;
    private int signalLampe;
    private int fahrtBis = 0;
    private Ersatzsignal lock1;
    private Ersatzsignal lock2;
    private Doppeltaster loeschtaste;
    
    /**
     * Initialisiert das Ersatzsignal.
     * 
     * @param conf
     * @param name
     * @param signalT
     * @param signalLampe 
     * @param lock1 
     * @param lock2 
     */
    public void init(Config conf, String name, int signalT, int signalLampe, Ersatzsignal lock1, Ersatzsignal lock2) {
        this.conf = conf;
        this.name = name;
        this.signalLampe = signalLampe;
        this.lock1 = lock1;
        this.lock2 = lock2;
        
        taste = new Doppeltaster();
        taste.init(conf, this, Const.ErsGT, signalT);
        
        loeschtaste = new Doppeltaster();
        loeschtaste.init(conf, this, Const.HaGT, signalT);
        conf.ticker.add(this);
    }

    /**
     * Setzt das Ersatzsignal auf Fahrt.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        switch (taste1) {
            case Const.ErsGT:
                if (lock1.isFahrt() || lock2.isFahrt()) {
                    String otherName = (lock1.isFahrt() ? lock1.name : lock2.name);
                    conf.alert(name + ": Es ist bereits eine Fahrt freigegeben - " + otherName);
                } else {
                    isFahrt = true;
                }
                break;
                
            case Const.HaGT:
                isFahrt = false;
                hp0();
                break;
        }
    }

    /**
     * Simuliert das Zeitrelais welches das Signal
     * nach 90 Sekunden wieder abschaltet.
     * 
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (isFahrt) {
            if (fahrtBis == 0) {
                // Ersatzsignal einschalten
                conf.connector.setOut(signalLampe, isFahrt);
                fahrtBis = count + FAHRT_DURATION;
                conf.alert("Ersatzsignal " + this.toString());
                conf.ersatzsignalCounter.whenPressed(0, 0);
            }
            
            if (count >= fahrtBis) {
                // nach 90 Sekunden Ersatzsignal abschalten
                hp0();
            }
        }
    }
    
    /**
     * Erzeugt eine Textdarstellung des aktuellen Zustands.
     * 
     * @return 
     */
    @Override
    public String toString() {
        return name + " : " + (isFahrt ? "Fahrt" : "Halt");
    }
    
    /**
     * Meldet zurück, ob das Ersatzsignal auf Fahrt steht.
     * Die drei Signale in jede Richtung sind untereinander
     * gesperrt, es können also nicht zwei Züge in die gleiche
     * Richtung losgeschickt werden.
     * @return 
     */
    public boolean isFahrt() {
        return isFahrt;
    }

    /**
     * Nachdem der Zug das Bahnhofsgleis verlassen hat, fällt
     * das Ersatzsignal sofort auf hp0 zurück (§1, Abschnitt 8).
     */
    void hp0() {
        isFahrt = false;
        conf.connector.setOut(signalLampe, isFahrt);
        fahrtBis = 0;
        conf.alert("Ersatzsignal " + this.toString());
    }
}
