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
    public final static int ANZAHL_WEICHEN = 6;
    
    public final Ticker ticker = new Ticker();
    public final Connector connector = new Connector();
    public final Weiche[] weichen = new Weiche[ANZAHL_WEICHEN];
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
