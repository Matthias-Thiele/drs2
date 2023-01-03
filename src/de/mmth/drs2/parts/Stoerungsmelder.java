/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;

/**
 *
 * @author pi
 */
public class Stoerungsmelder implements TickerEvent, TastenEvent {

    private Config config;
    private Einfachtaster tasteS;
    private Einfachtaster tasteW;
    private int tasteSint;
    private int lampeS;
    private int lampeW;

    private boolean strgS = false;
    private boolean strgW = false;
    private int klingel;
    
    public void init(Config config, int tasteS, int tasteW, int lampeS, int lampeW, int klingel) {
        this.config = config;
        tasteSint = tasteS;
        this.tasteS = new Einfachtaster();
        this.tasteS.init(config, this, tasteS);
        this.tasteW = new Einfachtaster();
        this.tasteW.init(config, this, tasteW);
        this.lampeS = lampeS;
        this.lampeW = lampeW;
        this.klingel = klingel;
        config.ticker.add(this);
    }
    
    public void stoerungS() {
        strgS = true;
    }
    
    public void stoerungW() {
        strgW = true;
    }
    
    @Override
    public void tick(int count) {
        if (strgS) {
            config.connector.setOut(lampeS, (count & 0x10) != 0);
        }
        if (strgW) {
            config.connector.setOut(lampeW, (count & 0x10) == 0);
        }
        
        if (strgS || strgW) {
            config.connector.setOut(klingel, (count & 0x10) == 0);
        }
    }

    @Override
    public void whenPressed(int taste) {
        if (taste == tasteSint) {
            strgS = false;
            config.connector.setOut(lampeS, false);
        } else {
            strgW = false;
            config.connector.setOut(lampeW, false);
        }
        
        config.connector.setOut(klingel, false);
    }
    
}
