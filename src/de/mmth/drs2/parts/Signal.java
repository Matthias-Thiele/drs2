/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.io.Connector;

/**
 * Diese Klasse kapselt die Definition und 
 * Verwendung eines Signals.
 * @author pi
 */
public class Signal {

    private Connector conn;
    private String name;
    private int sigFahrt;
    private int sigHalt;
    private int vorsigFahrt;
    private int vorsigHalt;
    private boolean isFahrt = false;
    
    /**
     * Zur Initialisierung wird der PortEpander Connector
     * und die Nummern der verwendeten Anzeigelampen übergeben.
     * 
     * @param conn
     * @param name
     * @param sigFahrt
     * @param sigHalt
     * @param vorsigFahrt
     * @param vorsigHalt 
     */
    public void init(Connector conn, String name, int sigFahrt, int sigHalt, int vorsigFahrt, int vorsigHalt) {
        this.name = name;
        this.conn = conn;
        this.sigFahrt = sigFahrt;
        this.sigHalt = sigHalt;
        this.vorsigFahrt = vorsigFahrt;
        this.vorsigHalt = vorsigHalt;
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
    }
}
