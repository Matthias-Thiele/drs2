/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;
import static de.mmth.drs2.io.Connector.WEICHE_IV_OUT;

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
    private int state = 0;
    private Config config;
    private int rot;
    private int weiss;
    private Doppeltaster tasteSlFT;
    private Doppeltaster tasteSlFLT;
    private int wsRelais;
    private int wsCheck;
    private String name;
    private boolean verriegelt;
    
    /**
     * Initialisiert das Objekt mit der Tastennummer und den
     * Lampennummern.
     * 
     * @param config
     * @param name
     * @param tasteSlFT
     * @param rot
     * @param weiss 
     * @param wsRelais 
     * @param wsCheck 
     */
    public void init(Config config, String name, int tasteSlFT, int rot, int weiss, int wsRelais, int wsCheck) {
        this.config = config;
        this.name = name;
        this.tasteSlFT = new Doppeltaster();
        this.tasteSlFT.init(config, this, Const.WGT, tasteSlFT);
        this.tasteSlFLT = new Doppeltaster();
        this.tasteSlFLT.init(config, this, Const.SlFLT, tasteSlFT);
        this.rot = rot;
        this.weiss = weiss;
        this.wsRelais = wsRelais;
        this.wsCheck = wsCheck;
        
        config.connector.setOut(rot, false);
        config.connector.setOut(weiss, true);
        config.ticker.add(this);
    }
    
    /**
     * Liefert den Namen der Schlüsselweiche zurück
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    public void fsVerriegelt(boolean aktiv) {
        this.verriegelt = aktiv;
    }
    
    /**
     * Anwender hat die WGT und die Schlüsselweichentaste gedrückt.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        if (taste1 == Const.WGT) {
            if (verriegelt) {
                config.alert("Weiche ist durch eine Fahrstraße verriegelt.");
            } else {
                state = 1;
            }
        } else {
            // Löschtaste wurde betätigt
            switch (state) {
                case 0:
                    config.alert("Schlüssel war noch nicht freigegeben.");
                    break;
                    
                case 1:
                case 2:
                    state = 0;
                    config.alert("Schlüsselfreigabe zurückgenommen.");
                    config.connector.setOut(wsRelais, false);
                    break;
                    
                case 3:
                    config.alert("Schlüssel wurde bereits entnommen.");
                    break;
            } 
        }
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
                state = 2;
                config.connector.setOut(wsRelais, true);
                config.alert("Schlüssel freigegeben.");
                break;
                
            case 2:
                // Waretet bis der Schlüssel entnommen wird, blinkt solange rot.
                config.connector.setOut(rot, (count & 8) != 0);
                config.connector.setOut(weiss, false);
                if (!config.connector.isInSet(wsCheck)) {
                    // Schlüssel entnommen.
                    state = 3;
                    config.alert("Schlüssel entnommen.");
                    config.connector.setOut(wsRelais, false);
                }
                break;
                
            case 3:
                // wartet bis die Schlüsselentnahme beendet ist.
                if (!config.connector.isInSet(wsCheck)) {
                    state = 4;
                }
                break;
                
            case 4:
                // Warte auf die Schlüsselrückgabe, statisches rotes Licht.
                config.connector.setOut(rot, true);
                config.connector.setOut(weiss, false);
                config.connector.setOut(WEICHE_IV_OUT, false);
                if (config.connector.isInSet(wsCheck)) {
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

    /**
     * Meldet zurück, ob der Schlüssel verrigelt ist.
     * @return 
     */
    boolean isLocked() {
        return state != 0;
    }
    
}
