/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;

/**
 * Diese Klasse bindet die Schlüsselfreigabe an das
 * DRS2 Stellpult.
 * 
 * Solange noch keine echte Schlüsselfreigabe integriert
 * ist, wird diese durch Zeitverzögerungen simuliert.
 * Nach Tastendruck wird eine kurze Zeit gewartet und
 * die Schlüsselentnahme signalisiert. Danach wird eine
 * längere Zeit gewartet und die Schlüsselrückgabe
 * signalisiert.
 * 
 * @author Matthias Thiele
 */
public class Schluesselweiche implements TastenEvent, TickerEvent {
    private static final int BLINK_DURATION = 128;
    private static final int RED_DURATION = 300;
    private int state = 0;
    private Config config;
    private int rot;
    private int weiss;
    private Doppeltaster taste;
    private int blinkUntil;
    private int redUntil;
    
    /**
     * Initialisiert das Objekt mit der Tastennummer und den
     * Lampennummern.
     * 
     * @param config
     * @param taste
     * @param rot
     * @param weiss 
     */
    public void init(Config config, int taste, int rot, int weiss) {
        this.config = config;
        this.taste = new Doppeltaster();
        this.taste.init(config, this, 0, taste);
        this.rot = rot;
        this.weiss = weiss;
        
        config.connector.setOut(rot, false);
        config.connector.setOut(weiss, true);
        config.ticker.add(this);
    }
    
    /**
     * Anwender hat die WGT und die Schlüsselweichentaste gedrückt.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        state = 1;
    }

    /**
     * State-Machine zur Simulation der Anwenderaktionen.
     * @param count 
     */
    @Override
    public void tick(int count) {
        switch (state) {
            case 0:
                // Ruhezustand, Schlüssel im Stellwerk.
                config.connector.setOut(rot, false);
                config.connector.setOut(weiss, true);
                break;
             
            case 1:
                // Schlüsselfreigabe vom DRS2 Stellpult.
                blinkUntil = BLINK_DURATION;
                state = 2;
                config.alert("Schlüssel freigegeben.");
                break;
                
            case 2:
                // Waretet bis der Schlüssel entnommen wird, blinkt solange rot.
                config.connector.setOut(rot, (blinkUntil & 8) != 0);
                config.connector.setOut(weiss, false);
                if (blinkUntil-- < 1) {
                    // Schlüssel entnommen.
                    state = 3;
                    redUntil = count + RED_DURATION;
                    config.alert("Schlüssel entnommen.");
                }
                break;
                
            case 3:
                // Warte auf die Schlüsselrückgabe, statisches rotes Licht.
                config.connector.setOut(rot, true);
                config.connector.setOut(weiss, false);
                if (redUntil < count) {
                    // Schlüsselrückgabe.
                    state = 0;
                    config.alert("Schlüssel zurückgegeben.");
                }
                break;
                
            default:
                state = 0;
                break;
        }
    }
    
}
