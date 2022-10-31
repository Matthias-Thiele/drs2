/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.io;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;

/**
 *
 * @author Matthias Thiele
 */
public class Mcp23017 {
    
    /**
     * Basisadresse der MCP23017 Portexpander Bausteine.
     * 0x20 ist die Adresse 0, die sieben weiteren liegen
     * dann auf 0x21, 0x22...
     */
    public static final int MCP23017_ADDRESS = 0x20;

    private static final int IODIRA_REGISTER = 0x00; //IODIRA Register. Responsible for input or output
    private static final int IODIRB_REGISTER = 0x01; //IODIRB Register. Responsible for input or output
    
    private static final int GPIOA_REGISTER = 0x12; //GPIOA Register. Write or read value
    private static final int GPIOB_REGISTER = 0x13; //GPIOB Register. Write or read value
    
    private static final int GPPUA_REGISTER = 0x0C; //PORT A Pull-up value. If set configures the internal pull-ups
    private static final int GPPUB_REGISTER = 0x0D; ///PORT B Pull-up value. If set configures the internal pull-ups
    
    private I2CBus i2c;
    private I2CDevice device;
    
    /**
     * Initialisiert alle MCP23017 Bausteine
     * 
     * TODO Parameter für Input und Output Kanäle
     * @throws Exception 
     */
    public void init() throws Exception {
        i2c = I2CFactory.getInstance(I2CBus.BUS_1);
        device = i2c.getDevice(MCP23017_ADDRESS);
        
        // Test mit nur einem Baustein A = Out, B = In
        device.write(IODIRA_REGISTER, (byte) 0x00);
        device.write(GPIOA_REGISTER, (byte)0);
        device.write(IODIRB_REGISTER, (byte) 0xFF);
        device.write(GPPUB_REGISTER, (byte) 0xFF);
    }
    
    /**
     * Schreibt die unteren 16 Bit des int Wertes value
     * in den angegebenen Kanal cardNo.
     * 
     * TODO - zum Test nur 8 Bit in A des einzigen
     *        Bausteins schreiben.
     * @param cardNo
     * @param value
     * @throws IOException 
     */
    public void write16(int cardNo, int value) throws IOException {
        device.write(GPIOA_REGISTER, (byte) value); 
    }
    
    /**
     * Liest 16 Bit aus dem Kanal cardNo und gibt sie als
     * int Wert zurück.
     * 
     * TODO - zum Test nur die 8 Bit aus B des einzigen
     *        Bausteins lesen.
     * @param cardNo
     * @return
     * @throws IOException 
     */
    public int read16(int cardNo) throws IOException {
        int result = device.read(GPIOB_REGISTER);
        
        return result;
    }
}
