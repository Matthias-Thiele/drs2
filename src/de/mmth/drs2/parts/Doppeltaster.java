/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.io.Connector;

/**
 *
 * @author pi
 */
public class Doppeltaster implements TickerEvent {
    private static final int ACTIVATION_COUNT = 10;
    private static final int ALERT_COUNT = 80;
    
    private Connector drs2;
    private TastenEvent activateWhenPressed;
    private int taste1;
    private int taste2;
    private int taste1Count;
    private int taste2Count;
    private Config config;
    
    @Override
    public void tick(int count) {
        if (drs2.isInSet(taste1)) {
            taste1Count++;
        } else {
            taste1Count = 0;
        }
        
        if (drs2.isInSet(taste2)) {
            taste2Count++;
        } else {
            taste2Count = 0;
        }
        
        if (((taste1Count == ACTIVATION_COUNT) && (taste2Count >= ACTIVATION_COUNT))
         || ((taste2Count == ACTIVATION_COUNT) && (taste1Count >= ACTIVATION_COUNT))) {
            activateWhenPressed.whenPressed();
        }
        
        if (taste1Count == ALERT_COUNT) {
            config.alert("Taste " + taste1 + " hängt.");
        }
        
        if (taste2Count == ALERT_COUNT) {
            config.alert("Taste " + taste2 + " hängt.");
        }
    }
    
    public void init(Config config, Connector drs2, TastenEvent activateWhenPressed, int taste1, int taste2) {
        this.config = config;
        config.ticker.add(this);
        this.drs2 = drs2;
        this.activateWhenPressed = activateWhenPressed;
        this.taste1 = taste1;
        taste1Count = 0;
        this.taste2 = taste2;
        taste2Count = 0;
    }
}
