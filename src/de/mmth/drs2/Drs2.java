/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import com.fazecast.jSerialComm.SerialPort;
import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Connector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Diese Klasse simuliert die Relaissteuerung
 * zu einem DRS 2 Stellpult.
 * 
 * @author pi
 */
public class Drs2 extends Application {
    public static int val = 1;
    
    private final Config config = new Config();
    
    /**
     * JavaFX Startpunkt erzeugt die Ansicht.
     * 
     * @param primaryStage
     * @throws Exception 
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        config.init();
        try {
            config.connector.init(config);
        } catch(Exception ex) {
            // retry bei Kommunikationsfehler im I2C Bus
            config.connector.init(config);
        }
        
        //inputTester();
        //outputTester2(48, 64);
        config.connector.setOut(Connector.LOCAL_REL2, true);
        MainPane main = new MainPane(config);
        StackPane root = new StackPane();
        root.getChildren().add(main);
        config.mainPane = main;
        
        Scene scene = new Scene(root, 1660, 900);
        
        primaryStage.setTitle("WSB-Calw DRS 2");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(we -> {
            try {
                config.ticker.interrupt();
                config.connector.switchOff();
                config.connector.tick(0);
                primaryStage.close();
                System.exit(0);
            } catch (Exception ex) {
                System.out.println("Error on closing app: " + ex);
            }
        });
        
        config.stage = primaryStage;
        config.ticker.start();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("drs2");
        launch(args);
    }
    
    public void outputTester2(int fromPort, int toPort) throws InterruptedException {
        for (int clr = 0; clr < 48; clr++) {
            config.connector.setOut(clr, false);
        }
        
        for (int i = fromPort; i < toPort; i++) {
            config.connector.setOut(i - 1, false);
            config.connector.setOut(i,true);
            config.connector.tick(i);
            Thread.sleep(300);
        }
    }
    
    /**public void outputTester() throws Exception {
        I2CDevice dev = config.connector.mcp.initOutput(4);
        for (int cnt = 0; cnt < 1000; cnt++) {
            dev.write(0x12, (byte) 0xff);
            dev.write(0x13, (byte) 0xff);
            Thread.sleep(200);
            
            int mask = 1;
            for (int port = 0; port < 16; port++) {
                dev.write(0x12, (byte)(mask & 0xff));
                dev.write(0x13, (byte)((mask >> 8) & 0xff));
                mask <<= 1;
                Thread.sleep(200);
            }
        }
        
        for (int j = 0; j < 1000; j++) {
            dev.write(0x12, (byte) 0x55); 
            dev.write(0x13, (byte) 0xAA); 
            Thread.sleep(500);
            dev.write(0x13, (byte) 0x55); 
            dev.write(0x12, (byte) 0xAA); 
            Thread.sleep(500);
        }       
    }
    
    public void inputTester() throws Exception {
        I2CDevice dev = config.connector.mcp.initInput(0);
        for (int j = 0; j < 1000; j++) {
            int valLow = dev.read(0x12);
            int valHi = dev.read(0x13);
            int valx = (valLow | (valHi << 8)) ^ 0xff80;
            System.out.println(Integer.toHexString(valx));
            Thread.sleep(500);
        }
    }**/
    
}
