/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.state.SwitchState;

/**
 *
 * @author pi
 */
public class Weiche implements TickerEvent, TastenEvent, ColorMarker {
    private static final int BLINK_DURATION = 48;
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
    private boolean pendingClearGestoert;
    private SwitchState state = SwitchState.OK;
    
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
        updateOutput();
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
     * Liefert den aktuellen Zustand der Weichenstörung zurück.
     * 
     * @return 
     */
    public SwitchState getState() {
      return state;
    }
    
    public void setState(SwitchState state) {
      this.state = state;
      
      if (state == SwitchState.OK) {
        updateOutput();
        blink = 1;
      }
    }
    
    public boolean starteUmlauf() {
      if (state == SwitchState.OK) {
        inPlusStellung = !inPlusStellung;
        blink = BLINK_DURATION;
        return true;
      } else {
        return false;
      }
    }
    
    /**
     * Stelle die Weiche direkt auf Störung da sie aufgefahren wurde.
     */
    public void setStoerung() {
        setState(SwitchState.AUFGEFAHREN);
    }
    
    /**
     * Meldet zurück, ob eine Störung vorliegt.
     * @return 
     */
    public boolean isGestoert() {
        return state == SwitchState.AUFGEFAHREN || ((blink > BLINK_DURATION) && (blink < Integer.MAX_VALUE - BLINK_DURATION));
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
        switch (state) {
          case OK:
            blink = BLINK_DURATION;
            break;
            
          case AUFGEFAHREN:
            blink = BLINK_DURATION;
            pendingClearGestoert = true;
            break;
            
          case EINFACHE_BLOCKIERUNG:
            if (pendingClearGestoert) {
              blink = BLINK_DURATION;
            } else {
              blink = Integer.MAX_VALUE;
              pendingClearGestoert = true;
            }
            break;
            
          case DAUERHAFTE_BLOCKIERUNG:
            blink = Integer.MAX_VALUE;
            break;
        }
        
        updateOutput();
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
     * Meldet zurück ob die Weiche gerade befahren wird.
     * @return 
     */
    public boolean isRed() {
      return isActive;
    }
    
    /**
     * Weichenabschnitt wurde vom Zug befahren.
     */
    @Override
    public void red() {
      config.alert("Weiche " + name + " befahren.");
        isActive = true;
        updateOutput();
    }
    
    /**
     * Zug hat Weichenabschnitt verlassen.
     */
    @Override
    public void white() {
        isActive = false;
        updateOutput();
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
    private void updateOutput() {
      if (state == SwitchState.AUFGEFAHREN) {
        updateOutputAufgefahren();
      } else {
          updateOutputNormal();
      }
    }
    
    private void updateOutputAufgefahren() {
        boolean l1 = (isActive || inPlusStellung) ? config.blinklicht.getBlink(): false;
        boolean l2 = (isActive || !inPlusStellung) ? config.blinklicht.getBlink(): false;
        boolean l3 = (!isActive && inPlusStellung) ? false : config.blinklicht.getBlink();
        boolean l4 = (!isActive && !inPlusStellung) ? false : config.blinklicht.getBlink();
        config.connector.setOut(firstRed, l1);
        config.connector.setOut(sndRed, l2);
        config.connector.setOut(firstWhite, l3);
        config.connector.setOut(sndWhite, l4);
    }
    
    private void updateOutputNormal() {
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
        updateOutput();
        if (blink > 0) {
            blink--;
            if (blink == 0) {
                if (pendingClearGestoert) {
                    pendingClearGestoert = false;
                    state = SwitchState.OK;
                }
                updateOutput();
            }
        }
    }
    
    @Override
    public boolean hasMarker() {
        return true;
    }
}
