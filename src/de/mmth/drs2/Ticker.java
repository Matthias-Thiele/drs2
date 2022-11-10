/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pi
 */
public class Ticker extends Thread {
    final List<TickerEvent> callbackList = new ArrayList<>();
    final private long TICKER_DELAY = 62;
    private int tickCount = 0;
    
    /**
     * Ruft 16 mal pro Sekunde das Ticker Event
     * für alle registrierten Objekte auf.
     */
    @Override
    public void run() {
        for (;;) {
            try {
                Thread.sleep(TICKER_DELAY);
            } catch (InterruptedException ex) {
                break;
            }
            
            for (TickerEvent ev: callbackList) {
                ev.tick(tickCount);
            }
            
            tickCount++;
        }
    }
    
    /**
     * Fügt ein Objekt in die List der Objekte ein,
     * die Ticker Events erhalten wollen.
     * 
     * @param ev 
     */
    public void add(TickerEvent ev) {
        callbackList.add(ev);
    }
}
