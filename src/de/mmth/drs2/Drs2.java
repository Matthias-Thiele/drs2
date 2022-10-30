/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Connector;
import de.mmth.drs2.io.Mcp23017;
import de.mmth.drs2.parts.Weiche;
import javafx.application.Application;
import javafx.scene.Scene;
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
    
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        mcp23017 = new Mcp23017();
        mcp23017.init();
        
        config.init();
        
        config.connector.init(config.ticker);
        
        Weiche w1 = config.weichen[0];
        w1.init(config, "W1", 0, 1, 0);
        
        MainPane main = new MainPane(config);
        StackPane root = new StackPane();
        root.getChildren().add(main);
        config.mainPane = main;
        
        Scene scene = new Scene(root, 800, 500);
        
        primaryStage.setTitle("WSB-Calw DRS 2");
        primaryStage.setScene(scene);
        primaryStage.show();
        
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
