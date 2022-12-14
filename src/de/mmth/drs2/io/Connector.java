/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.io;

import de.mmth.drs2.Ticker;
import de.mmth.drs2.TickerEvent;
import java.io.IOException;

/**
 *
 * @author pi
 */
public class Connector implements TickerEvent {
    /**
     * Anzahl der Eingänge von der DRS2, muss ein vielfaches von 16 sein.
     */
    public final static int INPUT_COUNT = 32;
    
    /**
     * Anzahl der Ausgänge von der DRS2, muss ein vielfaches von 16 sein.
     */
    public final static int OUTPUT_COUNT = 96;
    
    private final boolean[] drs2In = new boolean[INPUT_COUNT];
    private final boolean[] drs2Out = new boolean[OUTPUT_COUNT];
    private final int[] polarity = {0xff80, 0x7e, 0};
    
    public final Mcp23017 mcp = new Mcp23017();
    
    /**
     * Das Tickerevent löst das Lesen der DRS 2 Tastereingänge
     * sowie das Schreiben der DRS 2 Lampenausgänge aus.
     * 
     * @param count 
     */
    @Override
    public void tick(int count) {
        try {
            //long start = System.nanoTime();
            readInputs();
            writeOutputs();
            //long duration = System.nanoTime() - start;
            //System.out.println("Time: " + duration);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    /**
     * Initialisiert die Verbindung zur DRS2 und
     * registriert sich beim Ticker um in regelmäßigen
     * Abständen die Taster zu lesen und die
     * Glühlampen zu schreiben.
     * 
     * @param ticker
     * @throws Exception 
     */
    public void init(Ticker ticker) throws Exception {
        mcp.init(6, 2, polarity);
        ticker.add(this);
        
        for (int i = 0; i < drs2In.length; i++) {
            drs2In[i] = false;
        }
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
            drs2Out[portNo] = value;
        }
    }
    
    /**
     * Liest alle Eingangssignale von der DRS2 aus
     * und schreibt sie in das interne drs2In Array.
     * 
     * @throws IOException 
     */
    private void readInputs() throws IOException {
        int portCount = INPUT_COUNT >> 4;
        int portNo = 0;
        
        for (int i = 0; i < portCount; i++) {
            int portValues = mcp.read16(i);
            for (int vi = 0; vi < 16; vi++) {
                drs2In[portNo] = (portValues & 1) == 1;
                portNo++;
                portValues = portValues >> 1;
            }
        }
    }
    
    private void writeOutputs() throws IOException {
        int portCount = OUTPUT_COUNT >> 4;
        int portNo = 0;
        
        for (int i = 0; i < portCount; i++) {
            int portValues = 0;
            for (int vo = 0; vo < 16; vo++) {
                portValues = portValues >> 1;
                if (drs2Out[portNo]) {
                    portValues |= 0x8000;
                }
                portNo++;
            }
            
            mcp.write16(i, portValues);
        }
    }
    
}
