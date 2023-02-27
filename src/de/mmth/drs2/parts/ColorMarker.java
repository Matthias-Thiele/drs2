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
    
    /**
     * Für logging und debugging
     * @return 
     */
    public String getName();
    
    /**
     * Nicht alle Signale haben einen Fahrwegmarker.
     * 
     * @return 
     */
    public boolean hasMarker();
}
