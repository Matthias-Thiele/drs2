/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2.fx;

import de.mmth.drs2.Config;
import de.mmth.drs2.parts.Ersatzsignal;
import de.mmth.drs2.parts.Fahrstrasse;
import de.mmth.drs2.parts.Signal;
import de.mmth.drs2.parts.Weiche;
import javafx.application.Platform;
import javafx.scene.control.Button;
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
    private Button totmann;
    private int lastTotmannState = -1;
    
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
        this.add(messages, 0, 0, 1, 3);
        
        addWeichen();
        addSignale();
        addErsatzsignale();
        addFahrstrassen();
        addSchluesselschalter();
        addTotmannschalter();
    }
    
    /**
     * Fügt die Schlüsselschalter für die
     * Fahrstraßenauflösung in die MainPane ein.
     */
    private void addSchluesselschalter() {
        var schlA = new Button("Schlüssel A");
        schlA.setOnAction(ev -> {
            condReleaseFahrstrasse(config.fahrstrassen[0]);
            condReleaseFahrstrasse(config.fahrstrassen[1]);
        });
        this.add(schlA, 2, 1);
        
        var schlF = new Button("Schlüssel F");
        schlF.setOnMouseClicked(ev -> {
            condReleaseFahrstrasse(config.fahrstrassen[2]);
            condReleaseFahrstrasse(config.fahrstrassen[3]);
        });
        this.add(schlF, 3, 1);
    }
    
    /**
     * Ein inactivityCounter überwacht alle Tastendrücke.
     * Wenn eine voreingestellte Zeit keine Aktivität
     * war, wird die 24Volt Versorgung abgeschaltet und
     * damit alle Lampen und alle Taster. Über diesen
     * Button kann die Spannung wieder eingeschaltet
     * werden oder die Abschaltung verzögert werden.
     */
    private void addTotmannschalter() {
        totmann = new Button("Totmannschalter");
        totmann.setOnAction(ev -> {
            config.connector.resetInactivityCounter();
        });
        this.add(totmann, 2, 2, 2, 1);
    }
    
    /**
     * Ändert die Hintergrundfarbe des Totmannschalters
     * in Abhängigkeit vom incativityCount.
     * @param state 0: ok, 1: warn, 2: alarm
     */
    public void markTotmannschalter(int state) {
        if (state == lastTotmannState) {
            return;
        }
        
        lastTotmannState = state;
        switch (state) {
            case 0:
                totmann.setStyle("-fx-background-color: lime");
                config.alert("Lampenspannung eingeschaltet.");
                break;
                
            case 1:
                totmann.setStyle("-fx-background-color: orange");
                config.alert("Lampenspannung wird bald abgeschaltet.");
                break;
                
            case 2:
                totmann.setStyle("-fx-background-color: red");
                config.alert("Lampenspannung abgeschaltet.");
                break;
        }
    }
    
    /**
     * Prüft, ob die Fahrstraße verschlossen ist und löst
     * Sie bei Bedarf auf.
     * @param fahrstrasse 
     */
    private void condReleaseFahrstrasse(Fahrstrasse fahrstrasse) {
        if (fahrstrasse.isLocked()) {
            fahrstrasse.unlock();
        }
    }
    /**
     * Fügt die Liste der Weichen Controls in
     * die MainPane ein.
     */
    private void addWeichen() {
        VBox box = new VBox(5);
        Text hdr = new Text("Weichen");
        box.getChildren().add(hdr);
        box.setMinWidth(120);
        
        for (Weiche weiche: config.weichen) {
            WeicheFx wfx = new WeicheFx(weiche);
            box.getChildren().add(wfx);
            config.ticker.add(wfx);
        }
        
        this.add(box, 1, 0, 1, 3);
    }
    
    /**
     * Fügt die Liste der Signal Controls in
     * die MainPane ein.
     */
    private void addSignale() {
        VBox box = new VBox(5);
        Text hdr = new Text("Signale");
        box.getChildren().add(hdr);
        box.setMinWidth(120);
        
        for (Signal signal: config.signale) {
            SignalFx sfx = new SignalFx(signal);
            box.getChildren().add(sfx);
            config.ticker.add(sfx);
        }
        
        this.add(box, 2, 0);
    }
    
    /**
     * Fügt die Liste der Ersatzsignal Controls in
     * die MainPane ein.
     */
    private void addErsatzsignale() {
        VBox box = new VBox(5);
        Text hdr = new Text("Ersatzsignale");
        box.getChildren().add(hdr);
        box.setMinWidth(120);
        
        for (Ersatzsignal signal: config.ersatzsignale) {
            ErsatzsignalFx sfx = new ErsatzsignalFx(signal);
            box.getChildren().add(sfx);
            config.ticker.add(sfx);
        }
        
        this.add(box, 3, 0);
    }
    
    /**
     * Fügt die Liste der Fahrstrassen Controls in
     * die MainPane ein.
     */
    private void addFahrstrassen() {
        VBox box = new VBox(5);
        Text hdr = new Text("Fahrstrassen");
        box.getChildren().add(hdr);
        box.setMinWidth(120);
        
        for (var fahrstrasse: config.fahrstrassen) {
            var ffx = new FahrstrasseFx(fahrstrasse);
            box.getChildren().add(ffx);
            config.ticker.add(ffx);
        }
        
        this.add(box, 4, 0, 1, 3);
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
