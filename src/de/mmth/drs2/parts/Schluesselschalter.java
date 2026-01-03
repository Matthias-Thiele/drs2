/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;

/**
 *
 * @author root
 */
public class Schluesselschalter implements TickerEvent {

    private final Config config;
    private final int schalter;
    private final int fahrstrasse1;
    private final int fahrstrasse2;

    /**
     * Überwacht die Schlüsselschalter A und F.
     * Wenn der Schalter betätigt wird, wird die
     * aktive Fahrstraße aufgelöst.
     * 
     * @param config
     * @param schalter
     * @param fahrstrasse1
     * @param fahrstrasse2 
     */
    public Schluesselschalter(Config config, int schalter, int fahrstrasse1, int fahrstrasse2) {
        this.config = config;
        this.schalter = schalter;
        this.fahrstrasse1 = fahrstrasse1;
        this.fahrstrasse2 = fahrstrasse2;
        
        config.ticker.add(this);
    }
    
    /**
     * Prüft ob der Fahrstraßenschalter betätigt wurde
     * und gibt dann die aktive Fahrstraße wieder frei.
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (config.connector.isInSet(schalter)) {
            condReleaseFahrstrasse(fahrstrasse1);
            condReleaseFahrstrasse(fahrstrasse2);
        }
    }
    
    /**
     * Prüft, ob die angegebene Fahrstrasse verrigelt
     * ist und gibt sie bei Bedarf frei.
     * 
     * @param fsNum 
     */
    private void condReleaseFahrstrasse(int fsNum) {
        if (config.fahrstrassen[fsNum].isLocked()) {
            config.fahrstrassen[fsNum].unlock();
        }
    }
    
}
