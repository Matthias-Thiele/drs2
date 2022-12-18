/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;

/**
 *
 * @author pi
 */
public class Drs2Test {

    public static final int MCP23017_ADDRESS = 0x20;

    private static final int IODIRA_REGISTER = 0x00; //IODIRA Register. Responsible for input or output
    private static final int IODIRB_REGISTER = 0x01; //IODIRB Register. Responsible for input or output
    
    private static final int GPIOA_REGISTER = 0x12; //GPIOA Register. Write or read value
    private static final int GPIOB_REGISTER = 0x13; //GPIOB Register. Write or read value
    
    private static final int GPPUA_REGISTER = 0x0C; //PORT A Pull-up value. If set configures the internal pull-ups
    private static final int GPPUB_REGISTER = 0x0D; ///PORT B Pull-up value. If set configures the internal pull-ups
    private static I2CBus i2c;
    

    /**
     * Test of start method, of class Drs2.
     * @throws java.lang.Exception
     */
    public static void testStart() throws Exception {
        i2c = I2CFactory.getInstance(I2CBus.BUS_1);
        I2CDevice device = i2c.getDevice(MCP23017_ADDRESS);
        device.write(IODIRA_REGISTER, (byte) 0x00);
        device.write(GPIOA_REGISTER, (byte) 0x00);
        device.write(IODIRB_REGISTER, (byte) 0x00);
        device.write(GPIOB_REGISTER, (byte) 0x00);
        
        System.out.println("start");
        
        int mask = 1;
        while (mask < 0x10000) {
            System.out.println("Test " + Integer.toBinaryString(mask));
            device.write(GPIOA_REGISTER, (byte) mask); 
            device.write(GPIOB_REGISTER, (byte) (mask >> 8));
            mask = mask << 1;
        }
    }

 
}
