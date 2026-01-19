/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.io;

import de.mmth.drs2.Config;

import de.mmth.drs2.TickerEvent;
/**
 *
 * @author pi
 */
public class Connector implements TickerEvent {
    
    /**
     * Anzahl der Eingänge von der DRS2, muss ein vielfaches von 16 sein.
     */
    public final static int INPUT_COUNT = 64;
    private final static int LOCAL_INPUT_COUNT = 8;
    
    /**
     * Anzahl der Ausgänge von der DRS2, muss ein vielfaches von 16 sein.
     */
    public final static int OUTPUT_COUNT = 136;
    public final static int LOCAL_OUTPUT_COUNT = 2;
    public final static int WEICHE_IV_OUT = OUTPUT_COUNT - 8;
    public final static int WECKER = OUTPUT_COUNT - 7;
    public final static int V24_OUT = OUTPUT_COUNT - 1;
    
    protected final boolean[] drs2In = new boolean[INPUT_COUNT + LOCAL_INPUT_COUNT];
    protected final boolean[] drs2Out = new boolean[OUTPUT_COUNT + LOCAL_OUTPUT_COUNT];
    private boolean outputChanged = false;
    private boolean blockChanged = false;
    
    public final int[] polarity = {0x80, 0xff, 0x7e, 0x64, 0xf, 0xff};
    
    private Config config;

    /**
     * Das Tickerevent löst das Lesen der DRS 2 Tastereingänge
     * sowie das Schreiben der DRS 2 Lampenausgänge aus.
     * 
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (outputChanged) {
            outputChanged = false;
            config.uart2.sendCommand(UartCommand.UPDATE_OUTPUTS);
        }
        if (blockChanged) {
            blockChanged = false;
            config.uart1.sendCommand(UartCommand.UPDATE_OUTPUTS);
        }
    }
    
    /**
     * Initialisiert die Verbindung zur DRS2 und
     * registriert sich beim Ticker um in regelmäßigen
     * Abständen die Taster zu lesen und die
     * Glühlampen zu schreiben.
     * 
     * @param config
     * @throws Exception 
     */
    public void init(Config config) throws Exception {
        this.config = config;
        config.ticker.add(this);
        
        for (int i = 0; i < drs2In.length; i++) {
            drs2In[i] = false;
        }
        
        blockChanged = true;
    }
    
    /**
     * Gibt den Inhalt des Connector Objekts für
     * debugging Zwecke aus.
     * 
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Input Liste\r\n");
        for (int i = 0; i < INPUT_COUNT; i++) {
            buf.append(i);
            buf.append(": ");
            buf.append(drs2In[i]);
            buf.append("\r\n");
        }
        
        buf.append("\r\nOutput Liste\r\n");
        for (int i = 0; i < INPUT_COUNT; i++) {
            buf.append(i);
            buf.append(": ");
            buf.append(drs2Out[i]);
            buf.append("\r\n");
        }
        
        return buf.toString();
    }
    
    /**
     * Meldet zurück, ob der Eingang aktiv (low) ist.
     * 
     * @param portNo
     * @return 
     */
    public boolean isInSet(int portNo) {
        return drs2In[portNo];
    }
    
    /**
     * Setzt den Wert eines Ausgangs.
     * 
     * @param portNo
     * @param value 
     */
    public void setOut(int portNo, boolean value) {
        if (portNo >= 0 && portNo < drs2Out.length) {
            if ((portNo >= 96 && portNo <= 103) || (portNo >= 120)) {
                blockChanged |= drs2Out[portNo] != value;
            } else {
                if (drs2Out[portNo] != value) {
                    System.out.println("Changed by " + portNo + " to " + value);
                }
                outputChanged |= drs2Out[portNo] != value; 
            }
            
            drs2Out[portNo] = value;
        }
    }
    
    /**
     * Setzt den Wert eines Ausgangs.
     * 
     * @param portNo
     */
    public void toggleOut(int portNo) {
        if (portNo >= 0 && portNo < drs2Out.length) {
            if ((portNo >= 96 && portNo <= 103) || (portNo >= 120)) {
                blockChanged = true;
            } else {
                outputChanged = true; 
            }
            
            System.out.println("1Port " + portNo + " is " + drs2Out[portNo]);
            drs2Out[portNo] = !drs2Out[portNo];
            System.out.println("2Port " + portNo + " is " + drs2Out[portNo]);
        }
    }
    
}
