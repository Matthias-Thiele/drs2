/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Connector;
import de.mmth.drs2.parts.Weiche;

/**
 *
 * @author pi
 */
public class Config {
    /**
     * Anzahl der Weichen im DRS 2 Stellpult die
     * über das System verwaltet werden.
     */
    public final static int ANZAHL_WEICHEN = 6;
    
    /**
     * Ticker für die Weiterschaltung und
     * Aktualisierung der verschiedenen
     * Systeme.
     */
    public final Ticker ticker = new Ticker();
    
    /**
     * Anbindung der DRS 2 Hardware. Das Modul stellt
     * die DRS 2 Ein- und Ausgänge als boolean Array
     * dar und kümmert sich um die Synchronisierung.
     * Mit jedem TickerEvent werden die Eingänge 
     * eingelesen und die Ausgänge aktualisiert.
     */
    public final Connector connector = new Connector();
    
    /**
     * Liste der Weichen aus dem Stellpult.
     */
    public final Weiche[] weichen = new Weiche[ANZAHL_WEICHEN];
    
    /**
     * JavaFX Anzeige des Systemzustands.
     */
    public MainPane mainPane;
    
    /**
     * Initialisiert die Systemkonfiguration
     */
    public void init() {
        for (int i = 0; i < ANZAHL_WEICHEN; i++) {
            weichen[i] = new Weiche();
        }
    }
    
    /**
     * Gibt eine Benachrichtigung aus
     * @param message 
     */
    public void alert(String message) {
        System.out.println(message);
        mainPane.addMessage(message);
    }
}
