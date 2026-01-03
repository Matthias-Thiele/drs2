/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2;

/**
 * Das Interface TickerEvent muss von allen Klassen
 * implementiert werden, die sich regelmäßig selbst
 * aktualisieren.
 * 
 * @author pi
 */
public interface TickerEvent {
    /**
     * Diese Callbackfunktion wird 16mal pro
     * Sekunde aufgerufen.
     * 
     * @param count 
     */
    public void tick(int count);
}
