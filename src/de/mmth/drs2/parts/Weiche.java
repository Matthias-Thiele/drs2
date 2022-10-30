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
public class Weiche implements TickerEvent, TastenEvent {
    private static final int BLINK_DURATION = 47;
    private Doppeltaster taste;
    private int firstLight;
    private int blink = 0;
    private boolean inPlusStellung = true;
    private int lockCount = 0;
    private Config config;
    private String name;
    
    /**
     * Übergibt die zugeordnete Tastereingänge und
     * Lampenausgänge
     * 
     * @param config
     * @param name
     * @param taste1
     * @param taste2
     * @param firstLight 
     */
    public void init(Config config, String name, int taste1, int taste2, int firstLight) {
        this.config = config;
        this.name = name;
        this.firstLight = firstLight;
        this.taste = new Doppeltaster();
        taste.init(config, this, taste1, taste2);
        updateOutput();
        config.ticker.add(this);
    }

    /**
     * Liefert den Namen der Weiche zurück.
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Doppeltaster wurden gedrückt.
     */
    @Override
    public void whenPressed() {
        if (lockCount > 0) {
            // Verrigelte Weichen können nicht umgestellt werden.
            config.alert("Eine verrigelte Weiche kann nicht umgestellt werden: " + name);
            return;
        }
        
        inPlusStellung = !inPlusStellung;
        blink = BLINK_DURATION;
        updateOutput();
        config.alert("Weiche " + name + " umgeschaltet nach " + inPlusStellung);
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
     * Verrigelt die Weiche.Wenn sie noch 
     * umfährt oder gestört ist, kann sie
     * nicht gesperrt werden. Eine bereits
     * gesperrte Weiche kann zusätzlich noch
     * einmal gesperrt werden.
     * 
     * @return Meldet zurück, ob die Sperre erfolgt ist.
     */
    public boolean lock() {
        if (blink == 0) {
            lockCount++;
            return true;
        }
        
        return false;
    }
    
    /**
     * Löst die Verrigelung
     */
    public void unlock() {
        if (lockCount == 0) {
            config.alert("Fehler - die Weiche " + name + " war nicht gesperrt.");
        } else {
            lockCount--;
        }
    }
    
    /**
     * Wenn eine Weiche gestört ist, blinkt
     * sie dauerhaft.
     * 
     * @param stoerung 
     */
    public void setStoerung(boolean stoerung) {
        if (stoerung) {
            blink = Integer.MAX_VALUE;
        } else {
            blink = 0;
        }
    }
    
    /**
     * Meldet, ob die Weich noch umläuft oder
     * gestört ist.
     * 
     * @return 
     */
    public boolean isRunning() {
        return blink != 0;
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
        
        config.connector.setOut(firstLight, l1);
        config.connector.setOut(firstLight + 1, l2);
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
