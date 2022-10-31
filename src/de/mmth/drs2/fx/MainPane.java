/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2.fx;

import de.mmth.drs2.Config;
import de.mmth.drs2.parts.Weiche;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Die MainPane ist ein JavaFX Control welches
 * die Anzeige aller DRS 2 Systeminformationen
 * enthält.
 * 
 * @author pi
 */
public class MainPane extends GridPane{

    private final Config config;
    private final TextArea messages;
    
    /**
     * Der Konstruktor übernimmt die Konfiguration
     * der Stellpultdaten.
     * 
     * @param config 
     */
    public MainPane(Config config) {
        this.config = config;
        this.setHgap(5);
        this.setVgap(5);
        
        messages = new TextArea();
        this.add(messages, 0, 0);
        
        VBox box = new VBox(5);
        Text hdr = new Text("Weichen");
        box.getChildren().add(hdr);
        box.setMinWidth(120);
        
        for (Weiche weiche: config.weichen) {
            WeicheFx wfx = new WeicheFx(weiche);
            box.getChildren().add(wfx);
            config.ticker.add(wfx);
        }
        
        this.add(box, 1, 0);
    }
    
    /**
     * Fügt eine Nachricht an das interne
     * Nachrichtenfenster an.
     * 
     * @param message 
     */
    public void addMessage(String message) {
        Platform.runLater(() -> {
            String txt = messages.getText() + message + "\r\n";
            messages.setText(txt);
            messages.setScrollTop(Double.MAX_VALUE);
        });
    }
}
