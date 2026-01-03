/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022, 2023 Matthias Thiele
 */
package de.mmth.drs2.io;

import com.fazecast.jSerialComm.SerialPort;
import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;

/**
 * Das Uart Objekt verwaltet die Kommunikation
 * zum externen Streckenblock.
 * 
 * Es ruft in regelmäßigen Abständen den aktuellen
 * Status des Streckenblocks ab und setzt die 
 * Anzeigen entsprechend.
 * 
 * Es bietet Funktionen zum Blocken und Rückblocken
 * des externen Streckenblocks.
 * 
 * @author root
 */
public final class Uart implements TickerEvent {
    private final static int PENDING_TRAIN_DURATION = 600;

    private final SerialPort comPort;
    private final Config config;
    private boolean actEinfahrt1 = false;
    private boolean actEinfahrt2 = false;
    private byte STATUS_MARKER = (byte) 'B';
    private final byte OUTPUT_MARKER = (byte) 'C';
    private final byte STATUS_END = (byte)'X';
    private byte RECEIVE_MARKER = (byte) 'B';
    private byte RECEIVE_END = (byte) 'X';
    private byte RECEIVE_END2 = (byte) 'Z';
    private int STATUS_LEN = 4;
    
    private final int IoInputStart = 56; // Input start der IO Platine(n)
    private final int IoOutputStart = 120; // Output start der IO Platine(n)
    
    /**
     * Erzeugt ein neues Uart Objekt und meldet es
     * am ticker an.
     * 
     * @param config
     * @param portName1
     * @param portName2
     * @param inputPolarity
     */
    public static void createUarts(Config config, String portName1, String portName2, int[] inputPolarity) {
        config.uart1 = new Uart(config, portName1, inputPolarity, false);
        config.ticker.add(config.uart1);
        config.uart2 = new Uart(config, portName2, inputPolarity, true);
        config.ticker.add(config.uart2);
    }
    
    private final int[] inputPolarity;
    private int statusPtr = -1;
    private final byte[] byteBuffer = new byte[1];
    private final byte[] statusMsg = new byte[6];
    private final boolean isDRS2;
    /**
     * Öffnet den Port zum Wechselstrom Block
     * 
     * @param config
     * @param portName 
     */
    private Uart(Config config, String portName, int[] inputPolarity, boolean isDRS2) {
        this.config = config;
        this.inputPolarity = inputPolarity;
        this.isDRS2 = isDRS2;
        
        if (isDRS2) {
            STATUS_MARKER = 'B';
            STATUS_LEN = 6;
        } else {
            STATUS_MARKER = 'U';
            STATUS_LEN = 3;
            RECEIVE_MARKER = 0x7;
            RECEIVE_END = 'A';
        }
        comPort = SerialPort.getCommPort(portName);
        comPort.openPort();
        
        sendCommand(UartCommand.GET_STATUS);
    }
    
    private int fromHex(byte val) {
        if (val >= '0' && val <= '9') {
            return val - '0';
        } else if (val >= 'A' && val <= 'F') {
            return val - 'A';
        } else if (val >= 'a' && val <= 'f') {
            return val - 'a';
        } else return -1;
    }
    
    private void processIoBytes(byte[] status) {
        boolean changed = false;
        int inputVals = fromHex(status[1]) * 16 + fromHex(status[2]);
        //System.err.println("Inputs: " + inputVals);
        for (var i = IoInputStart; i < IoInputStart + 8; i++) {
            var newState  = (inputVals & 1) == 1;
            if (config.connector.drs2In[i] != newState) {
                changed = true;
                System.out.print(i + " " + newState + ", ");
            }
            config.connector.drs2In[i] = newState;
            inputVals >>= 1;
        }
        if (changed) {
            System.out.println("changed IO.");
        }
    }
    
    private void processStatusByte(byte[] status) {
        byte b = status[0];
        if (b < 0) {
            //System.out.print(b & 0xf);
            if ((b & 1) == 1) {
                if (!actEinfahrt1) {
                    // Streckenblock M, Einfahrt von Weiß nach Rot
                    actEinfahrt1 = true;
                    config.stoerungsmelder.meldung();
                    config.pendingTrainM = PENDING_TRAIN_DURATION;
                }
            } else {
                actEinfahrt1 = false;
            }

            if ((b & 4) == 4) {
                if (!actEinfahrt2) {
                    // Streckenblock H, Einfahrt von Weiß nach Rot
                    actEinfahrt2 = true;
                    config.stoerungsmelder.meldung();
                }
            } else {
                actEinfahrt2 = false;
            }

            config.strecken[1].updateStreckenblock((b & 1) == 1);
            config.strecken[3].updateStreckenblock((b & 2) == 2);
            config.strecken[0].updateStreckenblock((b & 4) == 4);
            config.strecken[2].updateStreckenblock((b & 8) == 8);
        }
        
        boolean changed = false;
        b = status[1];
        var pol = 3;
        for (var j = 0; j < 8; j++) {
            var newState = (b & 1) != (pol & 1);
            if (config.connector.drs2In[32 + j] != newState) {
                changed = true;
                System.out.print((32 + j) + " " + newState + ", ");
            }
            config.connector.drs2In[32 + j] = newState;

            b >>= 1;
            pol >>= 1;
        }
        
        if (changed) {
            System.out.println("changed.");
        }
    }
    
    private void processInputBytes(byte[] buffer) {
        var pos = 0;
        boolean changed = false;
        for (var i = 0; i < STATUS_LEN; i++) {
            var b = buffer[i];
            var pol = inputPolarity[i];
            for (var j = 0; j < 8; j++) {
                if (pos == 32) {
                    pos += 8; // 8 Bit vom Block Status überspringen
                }
            
                var newState = (b & 1) != (pol & 1);
                if (config.connector.drs2In[pos] != newState) {
                    changed = true;
                    System.out.print(pos + " " + newState + ", ");
                }
                config.connector.drs2In[pos++] = newState;
                
                b >>= 1;
                pol >>= 1;
            }
        }
        
        if (changed) {
            System.out.println("changed.");
        }
    }
    
    /**
     * Füllt den Sendepuffer mit den Statuswerten aus drs2Out.
     * Der Bereich von 32 bis 39 gehört zum Block und nicht
     * zum DRS2, er wird deshalb übersprungen.
     * @param buffer 
     */
    private void fillupOutputBuffer(byte[] buffer) {
        var outputs = config.connector.drs2Out;
        var pos = 0;
        for (var i = 1; i < 15; i++) {
            if (i == 13) {
                pos += 8; // Status vom Block
            }
            var b = 0; 
            for (var j = 0; j < 8; j++) {
                b >>= 1;
                
                if (outputs[pos++]) {
                    b |= 0x80;
                }
            }
            
            buffer[i] = (byte) b;
        }
    }
    
    /**
     * Füllt die Status-Flags vom Block in den Sendepuffer.
     * drs2Out[96...103] liegen im Block und nicht im DRS2.
     * 
     * @param buffer 
     */
    private void fillupStatusBuffer(byte[] buffer) {
        var outputs = config.connector.drs2Out;
        var pos = 96;
        var b = 0; 
        for (var j = 0; j < 8; j++) {
            b >>= 1;

            if (outputs[pos++]) {
                b |= 0x80;
            }
        }

        buffer[1] = (byte) (b | 0x80);
    }
    
    /**
     * Füllt die Ausgabe-Bits in den Buffer für die serielle
     * Schnittstelle. Die zusätzlichen neuen Outputs liegen
     * im Bereich von 120 bis 128 (1 Platine) bzw. bis 136
     * (2 Platinen).
     * 
     * @param buffer 
     */
    private void fillupIOBuffer(byte[] buffer) {
        var outputs = config.connector.drs2Out;
        var pos = IoOutputStart;
        var ix = 0;
        
        buffer[ix++] = 'X';
        for (var i = 0; i < 8; i++) {
            buffer[ix++] = (byte) (outputs[pos ++] ? ('a' + i) : ('A' + i));
        }
        buffer[ix] = 'y';
        System.out.println(new String(buffer));
    }
    
    /**
     * Liest Rückmeldungen vom Block ein.
     * 
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (!isDRS2 && (statusPtr < 0) && ((count & 0x7) == 0x7)) {
            sendCommand(UartCommand.GET_STATUS);
        }
        
        while (comPort.bytesAvailable() > 0) {
            comPort.readBytes(byteBuffer, 1);
            //System.out.println("Read: " + Integer.toHexString(byteBuffer[0] & 0xff) + ", ptr: " + statusPtr);
            if ((statusPtr == -1) && (byteBuffer[0] == RECEIVE_MARKER)) {
                statusPtr = 0;
            } else {
                if (statusPtr < 0) {
                    //System.out.write(byteBuffer[0]);
                } else if (statusPtr >= STATUS_LEN) {
                    if (isDRS2) {
                        if (byteBuffer[0] == RECEIVE_END) {
                        // process status
                            processInputBytes(statusMsg);
                            statusPtr = -1;
                        } else {
                            // invalid status message
                            System.out.println("Invalid DRS2 status package dropped.");
                            statusPtr = -1;
                        }
                    } else {
                    }
                } else {
                    statusMsg[statusPtr] = byteBuffer[0];
                    statusPtr++;
                    //if (!isDRS2) System.out.println("cnt: " + statusPtr + ", read: " + byteBuffer[0] + ", exp: " + STATUS_LEN);
                    if (!isDRS2 && (statusPtr >= STATUS_LEN)) {
                        if (statusMsg[0] == RECEIVE_END2) {
                            processIoBytes(statusMsg);
                            statusPtr = -1;
                        } else {
                            // invalid status message
                            System.out.println("Invalid IO status package dropped.");
                            statusPtr = -1;
                        }
                    }
                }
            }
            
        }
    }
    
    public void sendCommand(UartCommand cmdNo) {
        byte[] buffer = new byte[20];
        var bytesToSend = 1;
        
        switch (cmdNo) {
            case TICKER:
                buffer[0] = 't';
                break;
                
            case GET_STATUS:
                while (comPort.bytesAvailable() > 0) {
                    comPort.readBytes(buffer, 1);
                }
                
                if (isDRS2) {
                    buffer[0] = 'Q';
                } else {
                    buffer[0] = 'X';
                    buffer[1] = 'z';
                    bytesToSend = 2;
                    statusPtr = 0;
                }
                break;
                
            case UPDATE_OUTPUTS:
                if (isDRS2) {
                    buffer[0] = OUTPUT_MARKER;
                    buffer[15] = STATUS_END;
                    fillupOutputBuffer(buffer);

                    bytesToSend = 16;
                } else {
                    fillupIOBuffer(buffer);

                    bytesToSend = 10;
                }
                break;
                
            case BLOCK1:
                buffer[0] = 'i';
                break;
                
            case BLOCK2:
                buffer[0] = 'o';
                break;
                
            case BLOCK3:
                buffer[0] = 'j';
                break;
                
            case BLOCK4:
                buffer[0] = 'p';
                break;
                
            case FLIP1:
                buffer[0] = 'I';
                break;
                
            case FLIP2:
                buffer[0] = 'O';
                break;
                
            case FLIP3:
                buffer[0] = 'J';
                break;
                
            case FLIP4:
                buffer[0] = 'P';
                break;
        }
        
        comPort.writeBytes(buffer, bytesToSend);
        /*System.out.print("Sent: ");
        for (var i = 0; i < bytesToSend; i++) {
            System.out.print(Integer.toHexString(buffer[i] & 0xff) + " ");
        }
        System.out.println();*/
    }
    
}
