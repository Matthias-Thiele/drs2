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
public class Einfachtaster implements TickerEvent {
    private static final int ACTIVATION_COUNT = 5;
    private static final int ALERT_COUNT = 160;
    
    private Connector drs2;
    private TastenEvent activateWhenPressed;
    private int taste;
    private int tasteCount;
    private Config config;
    
    /**
     * Mit dieser Funktion initialisiert das Objekt, welches
     * diese Taste verwendet, den Einfachtaster.
     * 
     * Das Zielobjekt gibt sich selber als Empfänger des
     * TastenEvents an sowie die Pin Nummer der Taste
     * 
     * @param config
     * @param activateWhenPressed
     * @param taste
     */
    public void init(Config config, TastenEvent activateWhenPressed, int taste) {
        this.config = config;
        config.ticker.add(this);
        this.drs2 = config.connector;
        this.activateWhenPressed = activateWhenPressed;
        this.taste = taste;
        tasteCount = 0;
    }
    
    /**
     * Mit jedem TickerEvent wird der Zustand
     * der Taste eingelesen. Wenn sie eine
     * voreingestellte Zeit gedrückt ist, wird
     * einmalig ein TastenEvent erzeugt.
     * 
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (drs2.isInSet(taste)) {
            tasteCount++;
        } else {
            tasteCount = 0;
        }
        
        if (tasteCount == ACTIVATION_COUNT)  {
            activateWhenPressed.whenPressed(taste, taste);
        }
        
        if (tasteCount == ALERT_COUNT) {
            config.alert("Taste " + taste + " hängt.");
        }
    }
        
    
}
