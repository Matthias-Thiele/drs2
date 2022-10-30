/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.io.Connector;
import de.mmth.drs2.io.Mcp23017;
import de.mmth.drs2.parts.Weiche;
import java.io.IOException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author pi
 */
public class Drs2 extends Application {
    Mcp23017 mcp23017;
    public static int val = 1;
    
    private final Config config = new Config();
    
    private final Connector drs2Con = new Connector();
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        mcp23017 = new Mcp23017();
        mcp23017.init();
        
        config.ticker.start();
        config.connector.init(config.ticker);
        
        Weiche w1 = new Weiche();
        w1.init(config, "W1", 0, 1, 0);
        
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction((ActionEvent event) -> {
            try {
                int old = mcp23017.read16(0);
                mcp23017.write16(0, val);
                int newval = mcp23017.read16(0);
                System.out.println("War: " + Integer.toHexString(old)+ ", jetzt " + Integer.toHexString(newval));
                System.err.print(drs2Con);
                val = val << 1;
            } catch (IOException ex) {
                System.out.println(ex);
            }
        });
        
        StackPane root = new StackPane();
        root.getChildren().add(btn);
        
        Scene scene = new Scene(root, 300, 250);
        
        primaryStage.setTitle("Hello World!");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("drs2");
        launch(args);
    }
    
}
