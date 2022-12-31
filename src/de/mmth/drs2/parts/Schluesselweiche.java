/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;

/**
 *
 * @author pi
 */
public class Schluesselweiche implements TastenEvent {
    private int state = 0;
    private Config config;
    private int rot;
    private int weiss;
    private Doppeltaster taste;
    
    public void init(Config config, int taste, int rot, int weiss) {
        this.config = config;
        this.taste = new Doppeltaster();
        this.taste.init(config, this, 0, taste);
        this.rot = rot;
        this.weiss = weiss;
        
        config.connector.setOut(rot, false);
        config.connector.setOut(weiss, false);
    }
    
    @Override
    public void whenPressed() {
        state++;
        switch (state) {
            case 1:
                config.connector.setOut(rot, false);
                config.connector.setOut(weiss, true);
                break;
                
            case 2:
                config.connector.setOut(rot, true);
                config.connector.setOut(weiss, false);
                break;
                
                
            case 3:
                config.connector.setOut(rot, false);
                config.connector.setOut(weiss, false);
                break;
                
            default:
                state = 0;
                break;
        }
    }
    
}
