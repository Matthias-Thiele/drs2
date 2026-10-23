/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022, 2023 Matthias Thiele
 */
package de.mmth.drs2.io;

import com.fazecast.jSerialComm.SerialPort;
import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final static int PENDING_TRAIN_DURATION = 10000;
    private final static byte ESTW_QUERY_TRACK_STATE = (byte) 0x90;
    private final static byte ESTW_LOCK_OUTBOUND = (byte) 0x91;
    private final static byte ESTW_UNLOCK_INBOUND = (byte) 0x92;
    private final static byte ESTW_TRACK_STATE_CHANGED0 = (byte) 0xa0;
    private final static byte ESTW_TRACK_STATE_CHANGED1 = (byte) 0xa1;
    private final static byte ESTW_TRACK_STATE_CHANGED2 = (byte) 0xa2;
    private final static byte ESTW_TRACK_STATE_CHANGED3 = (byte) 0xa3;
    private final static byte ESTW_ZUGNUMMER = (byte) 0xb8;
    
    private final SerialPort comPort;
    private final Config config;
    private boolean actEinfahrt1 = false;
    private byte STATUS_MARKER = (byte) 'B';
    private final byte OUTPUT_MARKER = (byte) 'C';
    private final byte STATUS_END = (byte)'X';
    private byte RECEIVE_MARKER = (byte) 'B';
    private byte RECEIVE_END = (byte) 'X';
    private int STATUS_LEN;
    
    private final int IoInputStart = 56; // Input start der IO Platine(n)
    private final int IoOutputStart = 120; // Output start der IO Platine(n)
    private final ConnectionType connectionType;
    
    enum ConnectionType {
      DRS2,
      IO,
      ESTW
    }
    
    /**
     * Erzeugt ein neues Uart Objekt und meldet es
     * am ticker an.
     * 
     * @param config
     * @param portName1 // DRS 2
     * @param portName2 // IO Karte
     * @param portName3 // ESTW
     * @param inputPolarity
     */
    public static void createUarts(Config config, String portName1, String portName2, String portName3, int[] inputPolarity) {
        config.uart1 = new Uart(config, portName1, inputPolarity, ConnectionType.IO);
        config.ticker.add(config.uart1);
        config.uart2 = new Uart(config, portName2, inputPolarity, ConnectionType.DRS2);
        config.ticker.add(config.uart2);
        config.uart3 = new Uart(config, portName3, inputPolarity, ConnectionType.ESTW);
        config.ticker.add(config.uart3);
    }
    
    private final int[] inputPolarity;
    private int statusPtr = -1;
    private final byte[] byteBuffer = new byte[1];
    private final byte[] statusMsg = new byte[32];
    
    private StringBuilder zugnummer = new StringBuilder(8);
    private boolean readZugnummer = false;
    private boolean inboundState = false;
    private boolean outboundState = false;
    
    /**
     * Öffnet den Port zum Wechselstrom Block
     * 
     * @param config
     * @param portName 
     */
    private Uart(Config config, String portName, int[] inputPolarity, ConnectionType conType) {
        this.config = config;
        this.inputPolarity = inputPolarity;
        this.connectionType = conType;
        
        switch (connectionType) {
          case DRS2:
            STATUS_MARKER = 'B';
            STATUS_LEN = 6;
          break;
            
          case IO:
            STATUS_MARKER = 'U';
            STATUS_LEN = 8;
            RECEIVE_MARKER = 'Z';
            RECEIVE_END = 'T';
          break;
            
          case ESTW:
            break;
        }
       
        comPort = SerialPort.getCommPort(portName);
        System.out.println("Open Port " + portName);
        comPort.openPort();
        
        clearUART();
        System.out.println("Get status.");
        sendCommand(UartCommand.GET_STATUS);
    }
    
    private int fromHex(byte val) {
        if (val >= '0' && val <= '9') {
            return val - '0';
        } else if (val >= 'A' && val <= 'F') {
            return val - 'A' + 10;
        } else if (val >= 'a' && val <= 'f') {
            return val - 'a' + 10;
        } else return -1;
    }
    
    private void processIoBytes(byte[] status) {
        boolean changed = false;
        //int inputVals = fromHex(status[1]) * 16 + fromHex(status[2]);
        int inputVals = (fromHex(status[2]) << 20) + (fromHex(status[3]) << 16) + (fromHex(status[4]) << 12) + (fromHex(status[5]) << 8) + (fromHex(status[6]) << 4) + fromHex(status[7]);
        //System.out.println("Inputs: " + Integer.toHexString(inputVals));
        inputVals ^= 0x30;
        
        //System.out.println("Inputs: " + Integer.toHexString(inputVals));
        for (var i = IoInputStart; i < IoInputStart + 24; i++) {
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
            if (config.connector.drs2In[Const.BLOCK_AH_IN]) {
              if (!actEinfahrt1) {
                  // Streckenblock M, Einfahrt von Weiß nach Rot
                  actEinfahrt1 = true;
                  if (!config.signale[0].isFahrt()) {
                    config.stoerungsmelder.meldung();
                  }
                  config.pendingTrainM = PENDING_TRAIN_DURATION;
              }
            } else {
              actEinfahrt1 = false;
            }
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
     * Füllt die Ausgabe-Bits in den Buffer für die serielle
     * Schnittstelle. Die zusätzlichen neuen Outputs liegen
     * im Bereich von 120 bis 144 (3 Platinen).
     * 
     * @param buffer 
     */
    private void fillupIOBuffer(byte[] buffer) {
        var outputs = config.connector.drs2Out;
        var pos = IoOutputStart;
        var ix = 0;
        
        buffer[ix++] = 'Z';
        for (var i = 0; i < 24; i++) {
            buffer[ix++] = (byte) (outputs[pos ++] ? ('a' + i) : ('A' + i));
        }
        buffer[ix] = 'y';
        System.out.println(new String(buffer));
    }
    
    /**
     * Sendet ein Vorblock Kommando zum ESTW.
     * 
     * Die Ausfahrt wird nur als belegt markiert, wenn vom
     * ESTW eine Antwort zurückkommt.
     */
    public void ESTW_Vorblocken() {
      sendByte(ESTW_LOCK_OUTBOUND);
    }
    
    /**
     * Sendet ein Rückblock Kommando zum ESTW.
     * 
     * Die Einfahrt wird nur als frei markiert, wenn vom
     * ESTW eine Antwort kommt.
     */
    public void ESTW_Rückblocken() {
      sendByte(ESTW_UNLOCK_INBOUND);
    }
    
    /**
     * Fordert die aktuelle Streckenbelegung an.
     */
    public void ESTW_UpdateInitState() {
      sendByte(ESTW_QUERY_TRACK_STATE);
      /*
      for (var i = 0; i < 100; i++) {
      sendByte(ESTW_QUERY_TRACK_STATE);
          try {
              Thread.sleep(50);
          } catch (InterruptedException ex) {
              Logger.getLogger(Uart.class.getName()).log(Level.SEVERE, null, ex);
              break;
          }
      }*/
    }
    
    /**
     * Meldet die Belegung des Einfahrtgleises zurück.
     * @return 
     */
    public boolean ESTW_GetInboundState() {
      return inboundState;
    }
    
    /**
     * Meldet die Belegung des Ausfahrtgleises zurück.
     * @return 
     */
    public boolean ESTW_GetOutboundState() {
      return outboundState;
    }
    
    /**
     * Sendet ein byte zum COM Port.
     * @param data 
     */
    private void sendByte(byte data) {
      byteBuffer[0] = data;
      comPort.writeBytes(byteBuffer, 1);
    }
    
    /**
     * Verarbeitet ein ESTW Kommando.
     * @param nextChar 
     */
    private void processEstwCommand(byte nextChar) {
      switch(nextChar) {
        case ESTW_TRACK_STATE_CHANGED0:
        case ESTW_TRACK_STATE_CHANGED1:
        case ESTW_TRACK_STATE_CHANGED2:
        case ESTW_TRACK_STATE_CHANGED3:
          if (!inboundState && ((nextChar & 1) == 1)) {
            config.pendingTrainH = PENDING_TRAIN_DURATION;
          }
          
          inboundState = (nextChar & 1) != 0;
          outboundState = (nextChar & 2) != 0;
          System.out.println("Track state: " + (nextChar & 0x3));
          break;
          
        case ESTW_ZUGNUMMER:
          readZugnummer = true;
          zugnummer.setLength(0);
          break;
          
        default:
          System.out.println("Invalid ESTW Command: " + Integer.toHexString(nextChar));
      }
    }
    
    /**
     * Verarbeitet das nächste Byte von der ESTW Schnittstelle.
     * @param nextChar 
     */
    private void tickESTW(byte nextChar) {
      if ((nextChar & 0x80) != 0) {
        processEstwCommand(nextChar);
      } else if (readZugnummer) {
        // process Zugnummer.
        zugnummer.append((char) nextChar);
        if (zugnummer.length() == 6) {
          config.alert("Zugankündigung " + zugnummer.toString());
          readZugnummer = false;
          zugnummer.setLength(0);
        }
      } else {
        System.out.print(nextChar);
      }
    }
    
    /**
     * Verarbeitet das nächste Byte vom DRS2.
     * @param nextChar 
     */
    private void tickDRS2(byte nextChar) {
      if ((statusPtr == -1) && (nextChar == RECEIVE_MARKER)) {
          statusPtr = 0;
      } else {
          if (statusPtr < 0) {
            //System.out.write(byteBuffer[0]);
          } else if (statusPtr >= STATUS_LEN) {
            if (nextChar == RECEIVE_END) {
              // process status
              processInputBytes(statusMsg);
              statusPtr = -1;
            } else {
              // invalid status message
              System.out.println("Invalid DRS2 status package dropped.");
              statusPtr = -1;
            }

          } else {
              statusMsg[statusPtr] = nextChar;
              statusPtr++;
              //System.out.println("cnt: " + statusPtr + ", read: " + nextChar + ", exp: " + STATUS_LEN + ", msg: " + (new String(statusMsg)));
          }
      }
    }
    
    /**
     * Verarbeitet das nächste Byte von den IO Karten.
     * @param nextChar 
     */
    private void tickIO(byte nextChar) {
      if ((statusPtr == -1) && (nextChar == RECEIVE_MARKER)) {
          statusPtr = 0;
      } else {
        if (statusPtr < 0) {
          //System.out.write(nextChar);
        } else if (statusPtr >= STATUS_LEN) {
          if (nextChar == RECEIVE_END) {
            processIoBytes(statusMsg);
            statusPtr = -1;
          } else {
            // invalid status message
            System.out.println("Invalid IO status package dropped: " + (new String(statusMsg)));
            statusPtr = -1;

            if (new String(statusMsg).startsWith("No ")) {
              sendCommand(UartCommand.GET_STATUS);
            }
          }
        } else {
          statusMsg[statusPtr] = nextChar;
          statusPtr++;
          //System.out.println("cnt: " + statusPtr + ", read: " + nextChar + ", exp: " + STATUS_LEN + ", msg: " + (new String(statusMsg)));
        }
      }
    }
    
    /**
     * Liest Rückmeldungen vom Block ein.
     * 
     * @param count 
     */
    @Override
    public void tick(int count) {
        while (comPort.bytesAvailable() > 0) {
            comPort.readBytes(byteBuffer, 1);
            //if (!isDRS2) System.out.print((char)byteBuffer[0]);
            // System.out.println("Read: " + Integer.toHexString(byteBuffer[0] & 0xff) + ", ptr: " + statusPtr);
            if (connectionType.equals(ConnectionType.ESTW)) {
              tickESTW(byteBuffer[0]);
              return;
            }
            
            switch (connectionType) {
              case DRS2:
                tickDRS2(byteBuffer[0]);
                break;
                
              case IO:
                tickIO(byteBuffer[0]);
                break;
                
              case ESTW:
                tickESTW(byteBuffer[0]);
                System.out.print((char)byteBuffer[0]);
                break;
            }
            
        }
    }
    
    private void clearUART() {
      byte[] buffer = new byte[2];
      while (comPort.bytesAvailable() > 0) {
          comPort.readBytes(buffer, 1);
          System.out.println(buffer[0]);
      }
    }
    
    public void sendCommand(UartCommand cmdNo) {
        byte[] buffer = new byte[30];
        var bytesToSend = 1;
        
        switch (cmdNo) {
            case TICKER:
                buffer[0] = 't';
                break;
                
            case GET_STATUS:
                clearUART();
                
                switch (connectionType) {
                  case DRS2:
                    buffer[0] = 'Q';
                    break;
                    
                  case IO:
                    buffer[0] = 'Z';
                    buffer[1] = 'z';
                    bytesToSend = 2;
                    statusPtr = -1;
                    break;
                    
                  case ESTW:
                    break;
                }
                break;
                
            case UPDATE_OUTPUTS:
                switch (connectionType) {
                  case DRS2:
                    buffer[0] = OUTPUT_MARKER;
                    buffer[15] = STATUS_END;
                    fillupOutputBuffer(buffer);

                    bytesToSend = 16;
                    break;

                  case IO:
                    fillupIOBuffer(buffer);

                    bytesToSend = 26;
                    break;

                  case ESTW:
                    break;
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
        System.out.print("Sent: ");
        for (var i = 0; i < bytesToSend; i++) {
            System.out.print(Integer.toHexString(buffer[i] & 0xff) + " ");
        }
        System.out.println();
    }
    
}
