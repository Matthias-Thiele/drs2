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
public class Weiche implements TickerEvent, TastenEvent {
    private static final int BLINK_DURATION = 47;
    private Doppeltaster taste;
    private Connector drs2;
    private int firstLight;
    private int blink = 0;
    private boolean inPlusStellung = true;
    private boolean isLocked = false;
    private Config config;
    
    /**
     * Übergibt die zugeordnete Tastereingänge und
     * Lampenausgänge
     * 
     * @param config
     * @param taste1
     * @param taste2
     * @param firstLight 
     */
    public void init(Config config, int taste1, int taste2, int firstLight) {
        this.config = config;
        this.drs2 = config.connector;
        this.firstLight = firstLight;
        this.taste = new Doppeltaster();
        taste.init(config, drs2, this, taste1, taste2);
        updateOutput();
        config.ticker.add(this);
    }

    /**
     * Doppeltaster wurden gedrückt.
     */
    @Override
    public void whenPressed() {
        if (isLocked) {
            // Verrigelte Weichen können nicht umgestellt werden.
            config.alert("Eine verrigelte Weiche kann nicht umgestellt werden.");
            return;
        }
        
        inPlusStellung = !inPlusStellung;
        blink = BLINK_DURATION;
        updateOutput();
        System.err.println("Weiche umgeschaltet nach " + inPlusStellung);
    }

    /**
     * Meldet zurück, ob die Weiche in Plusstellung
     * steht.
     * 
     * @return 
     */
    public boolean isPlus() {
        return this.inPlusStellung;
    }
    
    /**
     * Verrigelt die Weiche
     */
    public void lock() {
        isLocked = true;
    }
    
    /**
     * Löst die Verrigelung
     */
    public void unlock() {
        isLocked = false;
    }
    
    /**
     * Aktualisiert die Ausgabeports der
     * Anzeigelampen
     */
    private void updateOutput() {
        boolean l1 = inPlusStellung;
        boolean l2 = !l1;
        
        if ((blink & 0x4) == 0x4) {
            l1 = false;
            l2 = false;
        }
        
        drs2.setOut(firstLight, l1);
        drs2.setOut(firstLight + 1, l2);
    }
    
    /**
     * Hier wird das Blinklicht beim Umschalten simuliert.
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (blink > 0) {
            updateOutput();
            blink--;
        }
    }
}
