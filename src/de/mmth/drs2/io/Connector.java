/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.io;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.PullResistance;

import de.mmth.drs2.Ticker;
import de.mmth.drs2.TickerEvent;
import java.io.IOException;
/**
 *
 * @author pi
 */
public class Connector implements TickerEvent {
    public final Mcp23017 mcp = new Mcp23017();
    
    /**
     * Anzahl der Eingänge von der DRS2, muss ein vielfaches von 16 sein.
     */
    public final static int INPUT_COUNT = 32;
    private final static int LOCAL_INPUT_COUNT = 4;
    private final static int LOCAL_TA = 0;
    private final static int LOCAL_NC = 1;
    private final static int LOCAL_SlFT = 2;
    private final static int LOCAL_NC2 = 3;
    /**
     * Anzahl der Ausgänge von der DRS2, muss ein vielfaches von 16 sein.
     */
    public final static int OUTPUT_COUNT = 96;
    public final static int LOCAL_OUTPUT_COUNT = 2;
    public final static int LOCAL_REL1 = OUTPUT_COUNT + 0;
    public final static int LOCAL_REL2 = OUTPUT_COUNT + 1;
    
    private final boolean[] drs2In = new boolean[INPUT_COUNT + LOCAL_INPUT_COUNT];
    private final boolean[] drs2Out = new boolean[OUTPUT_COUNT + LOCAL_OUTPUT_COUNT];
    private final int[] polarity = {0xff80, 0x647e, 0};
    
    private DigitalInput tastenanschalter;
    private DigitalInput nc;
    private DigitalInput weichenschluessel;
    private DigitalInput nc2;
    
    private DigitalOutput relais1;
    private DigitalOutput relais2;
    
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
            
            // Lokale Eingänge lesen
            drs2In[INPUT_COUNT + LOCAL_TA] = tastenanschalter.isLow();
            drs2In[INPUT_COUNT + LOCAL_NC] = nc.isHigh();
            drs2In[INPUT_COUNT + LOCAL_SlFT] = weichenschluessel.isHigh();
            drs2In[INPUT_COUNT + LOCAL_NC2] = nc2.isHigh();
            
            // Lokale Ausgänge schreiben
            relais1.setState(drs2Out[LOCAL_REL1]);
            relais2.setState(drs2Out[LOCAL_REL2]);
            
            //long duration = System.nanoTime() - start;
            //System.out.println("Time: " + duration);
        } catch (com.pi4j.io.exception.IOException | IOException ex) {
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
        var pi4j = Pi4J.newAutoContext();
        tastenanschalter =  createInput(pi4j, "TA", 18);
        nc = createInput(pi4j, "NC", 23);
        weichenschluessel = createInput(pi4j, "SlFT", 24);
        nc2 = createInput(pi4j, "NC2", 25);
        
        relais1 = createOutput(pi4j, "REL1", 20);
        relais2 = createOutput(pi4j, "REL2", 21);
        
        mcp.init(6, 2, polarity);
        ticker.add(this);
        
        for (int i = 0; i < drs2In.length; i++) {
            drs2In[i] = false;
        }
    }
    
    /**
     * Erzeugt einen lokalen Eingabeport auf dem Raspberry Pi
     * 
     * @param pi4j
     * @param id
     * @param address
     * @return 
     */
    private DigitalInput createInput(Context pi4j, String id, int address) {
        var cfg = DigitalInput.newConfigBuilder(pi4j)
                .id(id)
                .address(address)
                .pull(PullResistance.PULL_UP)
                .provider("pigpio-digital-input");
        var input = pi4j.create(cfg);
        return input;
    }
    
    /**
     * Erzeugt einen lokalen Ausgang.
     * 
     * @param pi4j
     * @param id
     * @param address
     * @return 
     */
    private DigitalOutput createOutput(Context pi4j, String id, int address) {
        var cfg = DigitalOutput.newConfigBuilder(pi4j)
                .id(id)
                .address(address)
                .provider("pigpio-digital-output");
        var output = pi4j.create(cfg);
        return output;
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
    
    /**
     * Schreibt das Ausgabearry zum DRS2 raus.
     * 
     * @throws IOException 
     */
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
