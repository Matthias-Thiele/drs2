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
    private I2CDevice[] devices;
    private int inputOffset;
    
    /**
     * Initialisiert alle MCP23017 Bausteine.
     * 
     * Der Parameter outputCount gibt an,
     * wie viele Ausgabekarten vorhanden
     * sind, inputCount gibt die Anzahl
     * der Eingabekarten an. Beide Arten
     * müssen direkt nacheinander und
     * ohne Lücken belegt werden.
     * 
     * @param outputCount
     * @param inputCount
     * @throws Exception 
     */
    public void init(int outputCount, int inputCount) throws Exception {
        this.inputOffset = outputCount;
        devices = new I2CDevice[outputCount + inputCount];
        
        i2c = I2CFactory.getInstance(I2CBus.BUS_1);
        for (int numDevice = 0; numDevice < (outputCount + inputCount); numDevice++) {
            I2CDevice device = i2c.getDevice(MCP23017_ADDRESS + numDevice);
        
            if (numDevice < outputCount) {
                // Ausgabe konfigurieren
                device.write(IODIRA_REGISTER, (byte) 0x00);
                device.write(GPIOA_REGISTER, (byte)0);
                device.write(IODIRB_REGISTER, (byte) 0x00);
                device.write(GPIOB_REGISTER, (byte)0);
                
            } else {
                // Eingabe konfigurieren
                device.write(IODIRA_REGISTER, (byte) 0xFF);
                device.write(GPPUA_REGISTER, (byte) 0xFF);
                device.write(IODIRB_REGISTER, (byte) 0xFF);
                device.write(GPPUB_REGISTER, (byte) 0xFF);
            }
            
            devices[numDevice] = device;
        }
    }
    
    /**
     * Schreibt die unteren 16 Bit des int Wertes value
     * in den angegebenen Kanal cardNo.
     * 
     * @param cardNo
     * @param value
     * @throws IOException 
     */
    public void write16(int cardNo, int value) throws IOException {
        devices[cardNo].write(GPIOA_REGISTER, (byte) value); 
        devices[cardNo].write(GPIOB_REGISTER, (byte) (value >> 8)); 
    }
    
    /**
     * Liest 16 Bit aus dem Kanal cardNo und gibt sie als
     * int Wert zurück.
     * 
     * Die Eingabekarten werden wieder von 0 an durchgezählt
     * und liegen im Anschluss an alle Ausgabekarten.
     * 
     * @param cardNo
     * @return
     * @throws IOException 
     */
    public int read16(int cardNo) throws IOException {
        int result = devices[cardNo + inputOffset].read(GPIOA_REGISTER);
        result |= ((devices[cardNo + inputOffset].read(GPIOB_REGISTER) << 8) & 0xff00);
        
        return result;
    }
}
