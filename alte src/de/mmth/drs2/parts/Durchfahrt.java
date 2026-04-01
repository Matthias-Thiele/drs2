/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;

/**
 *
 * @author root
 */
public class Durchfahrt implements TickerEvent {

    private Config config;
    public void init(Config config) {
        this.config = config;
        config.ticker.add(this);
    }
    
    @Override
    public void tick(int count) {
        boolean vsigFahrt = false;
        if (config.signale[1].isFahrt() && (config.signale[2].isFahrt() || config.signale[3].isFahrt())) {
            vsigFahrt = true;
        }
        config.connector.setOut(Const.Vp13, vsigFahrt);
        
        vsigFahrt = false;
        if (config.signale[0].isFahrt() && (config.signale[4].isFahrt() || config.signale[5].isFahrt())) {
            vsigFahrt = true;
        }
        config.connector.setOut(Const.Vn23, vsigFahrt);
    }
    
}
