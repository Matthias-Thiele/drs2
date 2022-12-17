/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

/**
 *
 * @author pi
 */
public interface ColorMarker {
    /**
     * Schaltet den Fahrwegmarker auf weiß.
     */
    public void white();
    
    /**
     * Schaltet den Fahrwegmarker auf rot.
     */
    public void red();
    
    /**
     * Löscht den Fahrwegmarker.
     */
    public void clear();
}
