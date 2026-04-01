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
public class Counter implements TastenEvent, TickerEvent {
    private final int NOT_ACTIVE = -1;
    private final int ACTIVATE = -2;
    private final int ACTIVE_CYCLES = 5;
    
    private int shutdown = NOT_ACTIVE;
    private Config config;
    private int port;
    private String name;
    
    public void init(Config config, String name, int outputPort) {
        this.config = config;
        this.name = name;
        this.port = outputPort;
        config.ticker.add(this);
    }
    
    @Override
    public void whenPressed(int taste1, int taste2) {
        shutdown = ACTIVATE;
    }

    @Override
    public void tick(int count) {
        switch (shutdown) {
            case NOT_ACTIVE:
                // do nothing
                break;
                
            case ACTIVATE:
                shutdown = count + ACTIVE_CYCLES;
                config.connector.setOut(port, true);
                config.alert("Zähler " + name + " aktiviert.");
                break;
                
            default:
                if (count > shutdown) {
                    shutdown = NOT_ACTIVE;
                    config.connector.setOut(port, false);
                    config.alert("Zähler " + name + " deaktiviert.");
                }
        }
    }
    
}
