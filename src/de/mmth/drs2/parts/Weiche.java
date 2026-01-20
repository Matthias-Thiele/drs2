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
public class Weiche implements TickerEvent, TastenEvent, ColorMarker {
    private static final int BLINK_DURATION = 72;
    private Doppeltaster taste;
    private int firstWhite;
    private int sndWhite;
    private int firstRed;
    private int sndRed;
    private int blink = 0;
    private boolean inPlusStellung = true;
    private int lockCount = 0;
    private Config config;
    private String name;
    private boolean isActive;
    private boolean isGestoert = false;
    private boolean pendingClearGestoert;
    
    /**
     * Übergibt die zugeordnete Tastereingänge und
     * Lampenausgänge
     * 
     * @param config
     * @param name
     * @param taste1
     * @param taste2 
     * @param firstWhite 
     * @param sndWhite 
     * @param firstRed 
     * @param sndRed 
     */
    public void init(Config config, String name, int taste1, int taste2, int firstWhite, int sndWhite, int firstRed, int sndRed) {
        this.config = config;
        this.name = name;
        this.firstWhite = firstWhite;
        this.sndWhite = sndWhite;
        this.firstRed = firstRed;
        this.sndRed = sndRed;
        this.taste = new Doppeltaster();
        if (taste1 >= 0) {
            taste.init(config, this, taste1, taste2);
        }
        updateOutput(0);
        config.ticker.add(this);
    }

    /**
     * Liefert den Namen der Weiche zurück.
     * 
     * @return 
     */
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Stellt ein, ob die Weiche bei der nächsten Bestätigung
     * eine Störung anzeigt.
     * 
     * @param stoerung 
     */
    public void setStoerung(boolean stoerung) {
        isGestoert = stoerung;
        
        if (isGestoert) {
            config.stoerungsmelder.stoerungW();
        } else {
            blink = 0;
            updateOutput(0);
        }
    }
    
    /**
     * Stelle die Weiche direkt auf Störung da sie aufgefahren wurde.
     */
    public void setStoerung() {
        isGestoert = true;
        config.stoerungsmelder.stoerungW();
        blink = Integer.MAX_VALUE - 10 * BLINK_DURATION;
    }
    
    /**
     * Meldet zurück, ob eine Störung vorliegt.
     * @return 
     */
    public boolean isGestoert() {
        return isGestoert;
    }
    
    /**
     * Doppeltaster wurden gedrückt.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        if (lockCount > 0) {
            // Verrigelte Weichen können nicht umgestellt werden.
            config.alert("Eine verrigelte Weiche kann nicht umgestellt werden: " + name);
            return;
        }
        
        if (isActive) {
          config.alert("Aktuell befahrene Weichen können nicht umgestellt werden: " + name);
          return;
        }
        
        inPlusStellung = !inPlusStellung;
        if (blink > BLINK_DURATION) {
            pendingClearGestoert = true;
            blink = BLINK_DURATION;
        } else {
            blink = isGestoert ? Integer.MAX_VALUE : BLINK_DURATION;
        }
        
        updateOutput(0);
        config.alert("Weiche " + name + " umgeschaltet nach " + (inPlusStellung ? "Plus" : "Minus"));
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
     * Weichenabschnitt wurde vom Zug befahren.
     */
    @Override
    public void red() {
        isActive = true;
        updateOutput(0);
    }
    
    /**
     * Zug hat Weichenabschnitt verlassen.
     */
    @Override
    public void white() {
        isActive = false;
        updateOutput(0);
    }
    
    @Override
    public void clear() {
        // Weichenfahrwege bleiben immer ausgeleuchtet
        white();
    }
    
    /**
     * Meldet zurück ob die Weiche verschlossen ist.
     * @return 
     */
    public boolean isLocked() {
        return lockCount > 0;
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
    private void updateOutput(int count) {
        if (isGestoert) {
            updateOutputGestoert(count);
        } else {
            updateOutputNormal(count);
        }
    }
    
    private void updateOutputGestoert(int count) {
        boolean l1 = true;
        
        if ((count & 0x8) == 0x8) {
            l1 = false;
        }
        
        config.connector.setOut(firstRed, l1);
        config.connector.setOut(sndRed, l1);
        config.connector.setOut(firstWhite, false);
        config.connector.setOut(sndWhite, false);
    }
    
    private void updateOutputNormal(int count) {
        if (firstWhite < 0) {
            return; // nicht angeschlossen.
        }
        
        boolean l1 = inPlusStellung;
        boolean l2 = !l1;
        
        if ((blink > 0) && !config.blinklicht.getBlink()) {
            l1 = false;
            l2 = false;
        }
        
        if (isActive) {
            config.connector.setOut(firstRed, l1);
            config.connector.setOut(sndRed, l2);
            config.connector.setOut(firstWhite, false);
            config.connector.setOut(sndWhite, false);
            
        } else {
            config.connector.setOut(firstWhite, l1);
            config.connector.setOut(sndWhite, l2);
            config.connector.setOut(firstRed, false);
            config.connector.setOut(sndRed, false);
        }
    }
    
    /**
     * Hier wird das Blinklicht beim Umschalten simuliert.
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (blink == (Integer.MAX_VALUE - 2 * BLINK_DURATION)) {
            config.stoerungsmelder.stoerungW();
            config.alert("Weichenstörung " + name);
        }
        
        if (blink > 0) {
            updateOutput(count);
            blink--;
            if (blink == 0) {
                if (pendingClearGestoert) {
                    isGestoert = false;
                    pendingClearGestoert = false;
                }
                updateOutput(0);
            }
        }
    }
    
    @Override
    public boolean hasMarker() {
        return true;
    }
}
