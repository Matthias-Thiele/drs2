/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.io;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;
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
    
    private I2C[] devices;
    private int outputOffset;
    private int[] polarity;
    
    
    /**
     * Initialisiert alle MCP23017 Bausteine.Der Parameter outputCount gibt an,
     * wie viele Ausgabekarten vorhanden sind, 
     * inputCount gibt die Anzahl der Eingabekarten an.
     * 
     * Beide Arten müssen direkt nacheinander und ohne Lücken belegt werden.
     * 
     * @param outputCount
     * @param inputCount
     * @param polarity
     * @throws Exception 
     */
    public void init(int outputCount, int inputCount, int[] polarity) throws Exception {
        this.outputOffset = 3;
        this.polarity = polarity;
        
        Context pi4j = Pi4J.newAutoContext();
        I2CProvider i2CProvider = pi4j.provider("linuxfs-i2c");
        devices = new I2C[outputCount + outputOffset];
        
        for (int numDevice = 0; numDevice < (outputCount + outputOffset); numDevice++) {
            if (numDevice == 2) {
                // Input 3 wurde zu Output 6
                continue;
            }
            
            I2C device;
        
            if (numDevice < outputOffset) {
                // Eingabe konfigurieren
                device = initInput(pi4j, i2CProvider, numDevice);
            } else {
                // Ausgabe konfigurieren (Out 8 auf 2 mappen)
                device = initOutput(pi4j, i2CProvider, (numDevice == 8) ? 2 : numDevice);
            }
            
            devices[numDevice] = device;
        }
    }
    
    /**
     * Konfiguriert die MCP Register für 16 Ausgabelinien.Stellt alle Ausgänge auf 0 (Aus).
     * 
     *
     * @param pi4j 
     * @param i2CProvider 
     * @param numDevice
     * @return
     * @throws IOException 
     */
    public I2C initOutput(Context pi4j, I2CProvider i2CProvider, int numDevice) throws IOException {
        I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
            .id("MCP23017" + numDevice)
            .bus(1)
            .device(MCP23017_ADDRESS + numDevice)
            .build();
        try (I2C device = i2CProvider.create(i2cConfig)) {
            device.writeRegister(IODIRA_REGISTER, (byte) 0x00);
            device.writeRegister(GPIOA_REGISTER, (byte) 0x00);
            device.writeRegister(IODIRB_REGISTER, (byte) 0x00);
            device.writeRegister(GPIOB_REGISTER, (byte) 0x00);
        
            return device;
        }
    }
    
    /**
     * Konfiguriert die MCP Register für 16 Eingabelinien.Aktiviert die PullUp Widerstände im Portbaustein.Die Taster schalten gegen Masse.
     * 
     * 
     * @param pi4j
     * @param i2CProvider
     * @param numDevice
     * @return
     * @throws IOException 
     */
    public I2C initInput(Context pi4j, I2CProvider i2CProvider, int numDevice) throws IOException {
        I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
            .id("MCP23017" + numDevice)
            .bus(1)
            .device(MCP23017_ADDRESS + numDevice)
            .build();
        try (I2C device = i2CProvider.create(i2cConfig)) {
            device.writeRegister(IODIRA_REGISTER, (byte) 0xFF);
            device.writeRegister(IODIRB_REGISTER, (byte) 0xFF);
        
            return device;
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
        if (cardNo == 8) {
            // Output 8 auf 2 mappen (Output statt Input)
            cardNo = 2;
        }
        
        if (devices[cardNo + outputOffset] != null) {
            devices[cardNo + outputOffset].writeRegister(GPIOA_REGISTER, (byte) value); 
            devices[cardNo + outputOffset].writeRegister(GPIOB_REGISTER, (byte) (value >> 8)); 
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
            int result = devices[cardNo].readRegister(GPIOA_REGISTER);
            result |= ((devices[cardNo].readRegister(GPIOB_REGISTER) << 8) & 0xff00);

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
