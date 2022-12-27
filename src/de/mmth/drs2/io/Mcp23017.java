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
    private int outputOffset;
    private int[] polarity;
    
    
    /**
     * Initialisiert alle MCP23017 Bausteine.Der Parameter outputCount gibt an,
 wie viele Ausgabekarten vorhanden
 sind, inputCount gibt die Anzahl
 der Eingabekarten an.
     * 
     * Beide Arten
 müssen direkt nacheinander und
 ohne Lücken belegt werden.
     * 
     * @param outputCount
     * @param inputCount
     * @param polarity
     * @throws Exception 
     */
    public void init(int outputCount, int inputCount, int[] polarity) throws Exception {
        this.outputOffset = 3;
        this.polarity = polarity;
        
        devices = new I2CDevice[outputCount + outputOffset];
        
        i2c = I2CFactory.getInstance(I2CBus.BUS_1);
        for (int numDevice = 0; numDevice < (outputCount + outputOffset); numDevice++) {
            if (numDevice != 0 && numDevice != 1 && numDevice != 3 && numDevice != 4 && numDevice != 5 && numDevice != 6 && numDevice != 7) continue;
            
            I2CDevice device;
        
            if (numDevice < outputOffset) {
                // Eingabe konfigurieren
                device = initInput(numDevice);
            } else {
                // Ausgabe konfigurieren
                device = initOutput(numDevice);
            }
            
            devices[numDevice] = device;
        }
    }
    
    /**
     * Konfiguriert die MCP Register für 16 Ausgabelinien.
     * 
     * Stellt alle Ausgänge auf 0 (Aus).
     * 
     * @param numDevice
     * @return
     * @throws IOException 
     */
    public I2CDevice initOutput(int numDevice) throws IOException {
        I2CDevice device = i2c.getDevice(MCP23017_ADDRESS + numDevice);
        device.write(IODIRA_REGISTER, (byte) 0x00);
        device.write(GPIOA_REGISTER, (byte) 0x00);
        device.write(IODIRB_REGISTER, (byte) 0x00);
        device.write(GPIOB_REGISTER, (byte) 0x00);
        
        return device;
    }
    
    /**
     * Konfiguriert die MCP Register für 16 Eingabelinien.
     * 
     * Aktiviert die PullUp Widerstände im Portbaustein.
     * Die Taster schalten gegen Masse.
     * 
     * @param numDevice
     * @return
     * @throws IOException 
     */
    public I2CDevice initInput(int numDevice) throws IOException {
        I2CDevice device = i2c.getDevice(MCP23017_ADDRESS + numDevice);
        device.write(IODIRA_REGISTER, (byte) 0xFF);
        device.write(GPPUA_REGISTER, (byte) 0xFF);
        device.write(IODIRB_REGISTER, (byte) 0xFF);
        device.write(GPPUB_REGISTER, (byte) 0xFF);
        
        return device;
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
        if (devices[cardNo + outputOffset] != null) {
            devices[cardNo + outputOffset].write(GPIOA_REGISTER, (byte) value); 
            devices[cardNo + outputOffset].write(GPIOB_REGISTER, (byte) (value >> 8)); 
        }
    }
    
    private int testIn[] = new int[3];
    
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
        if (devices[cardNo] != null) {
            int result = devices[cardNo].read(GPIOA_REGISTER);
            result |= ((devices[cardNo].read(GPIOB_REGISTER) << 8) & 0xff00);

            result = result ^ polarity[cardNo];
            
            // nur zum Testen
            if (result != testIn[cardNo]) {
                System.out.println("Card: " + cardNo + " - " + Integer.toHexString(result) + " - " + Integer.toBinaryString(result));
                testIn[cardNo] = result;
            }
            
            return result;
        } else {
            return 0;
        }
    }
}
