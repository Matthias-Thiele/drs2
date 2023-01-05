/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.io.Connector;

/**
 * Aktionen im DRS 2 Stellpult erfolgen normalerweise
 * durch das gemeinsame drücken zweier Tasten um
 * versehentliche Fehlbedienungen zu vermeiden.
 * 
 * Diese Klasse prüft den Zustand eines Tastenpaares
 * und gibt ein einmaliges TastenEvent nach knapp
 * einer Sekunde ab.
 * 
 * @author pi
 */
public class Doppeltaster implements TickerEvent {
    private static final int ACTIVATION_COUNT = 10;
    private static final int ALERT_COUNT = 160;
    
    private Connector drs2;
    private TastenEvent activateWhenPressed;
    private int taste1;
    private int taste2;
    private int taste1Count;
    private int taste2Count;
    private Config config;
    
    /**
     * Mit jedem TickerEvent wird der Zustand der
     * beiden Taster eingelesen. Wenn beide eine
     * voreingestellte Zeit gedrückt sind, wird
     * einmalig ein TastenEvent erzeugt.
     * @param count 
     */
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
            activateWhenPressed.whenPressed(taste1, taste2);
        }
        
        if (taste1Count == ALERT_COUNT) {
            config.alert("Taste " + taste1 + " hängt.");
        }
        
        if (taste2Count == ALERT_COUNT) {
            config.alert("Taste " + taste2 + " hängt.");
        }
        
        if ((taste1Count >= ALERT_COUNT) || (taste2Count >= ALERT_COUNT)) {
            config.stoerungsmelder.stoerungT();
        }
    }
    
    /**
     * Mit dieser Funktion initialisiert das Objekt, welches
     * diese Tastenkombination verwendet, den Doppeltaster.
     * 
     * Das Zielobjekt gibt sich selber als Empfänger des
     * TastenEvents an sowie die Pin Nummern der beiden
     * Tasten.
     * 
     * @param config
     * @param activateWhenPressed
     * @param taste1
     * @param taste2 
     */
    public void init(Config config, TastenEvent activateWhenPressed, int taste1, int taste2) {
        this.config = config;
        config.ticker.add(this);
        this.drs2 = config.connector;
        this.activateWhenPressed = activateWhenPressed;
        this.taste1 = taste1;
        taste1Count = 0;
        this.taste2 = taste2;
        taste2Count = 0;
    }
}
