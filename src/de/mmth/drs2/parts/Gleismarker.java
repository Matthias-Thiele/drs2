/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;

/**
 * Diese Klasse kapselt die Gleisanzeige dunkel/ rot/ weiss 
 * für die Gleise 1 bis 3.
 * 
 * @author pi
 */
public class Gleismarker implements ColorMarker {

    private Config config;
    private int markerWhite;
    private int markerRed;

    public void init(Config config, int markerWhite, int markerRed) {
        this.config = config;
        this.markerWhite = markerWhite;
        this.markerRed = markerRed;
    }
    
    @Override
    public void white() {
        config.connector.setOut(markerRed, false);
        config.connector.setOut(markerWhite, true);
    }

    @Override
    public void red() {
        config.connector.setOut(markerRed, true);
        config.connector.setOut(markerWhite, false);
    }
    
    public void clear() {
        config.connector.setOut(markerRed, false);
        config.connector.setOut(markerWhite, false);
    };
}
