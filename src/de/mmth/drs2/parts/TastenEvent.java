/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2.parts;

/**
 * Das TastenEvent wird von allen Objekten implementiert,
 * die eine Benachrichtigung bei der Betätigung zweier
 * Tasten erhalten wollen.
 * 
 * @author pi
 */
public interface TastenEvent {
    /**
     * Diese Callbackfunktion wird bei Aktivierung
     * einmalig aufgerufen.Beim Doppeltaster enthält die erste Taste im
 Allgemeinen die Gruppentaste, welche wenig
 Aussagekraft enthält.
     * 
     * Dort wird deshalb die
 zweite Tastennummer als Parameter übergeben.
     * @param taste
     */
    public void whenPressed(int taste);
}
