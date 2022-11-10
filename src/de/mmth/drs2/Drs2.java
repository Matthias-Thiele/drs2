/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Mcp23017;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        
        config.connector.init(config.ticker);
        
        MainPane main = new MainPane(config);
        StackPane root = new StackPane();
        root.getChildren().add(main);
        config.mainPane = main;
        
        Scene scene = new Scene(root, 1200, 500);
        
        primaryStage.setTitle("WSB-Calw DRS 2");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(we -> {try {
            config.ticker.interrupt();
            Platform.exit();
            } catch (Exception ex) {
            }
});
        
        config.ticker.start();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("drs2");
        launch(args);
    }
    
}
