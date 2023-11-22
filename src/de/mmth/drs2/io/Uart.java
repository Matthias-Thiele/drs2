/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022, 2023 Matthias Thiele
 */
package de.mmth.drs2.io;

import com.fazecast.jSerialComm.SerialPort;

/**
 *
 * @author root
 */
public class Uart {
    public Uart(String portName) {
        SerialPort comPort = SerialPort.getCommPort(portName);
        comPort.openPort();
    }
    
}
