/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2.fx;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.Ersatzsignal;
import de.mmth.drs2.parts.Fahrstrasse;
import de.mmth.drs2.parts.Signal;
import de.mmth.drs2.parts.Weiche;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Die MainPane ist ein JavaFX Control welches
 * die Anzeige aller DRS 2 Systeminformationen
 * enthält.
 * 
 * @author pi
 */
public class MainPane extends HBox implements TickerEvent {
    private final static int PENDING_TRAIN_DURATION = 60;
    private final static int STD_BUTTON_SIZE = 140;
    
    private final Config config;
    private TextArea messages;
    private Button totmann;
    private int lastTotmannState = -1;
    private boolean pendingH, pendingM;
    private Button pendingHButton;
    private Button pendingMButton;
    
    /**
     * Der Konstruktor übernimmt die Konfiguration
     * der Stellpultdaten.
     * 
     * @param config 
     */
    public MainPane(Config config) {
        this.config = config;
        this.setSpacing(5);
        
        addSchalter();
        addSignale();
        addErsatzsignale();
        addWeichen();
        addFahrstrassen();
        addSchluesselschalter();
        
        config.ticker.add(this);
    }
    
    /**
     * Fügt die Schlüsselschalter für die
     * Fahrstraßenauflösung in die MainPane ein.
     */
    private void addSchluesselschalter() {
        var box = new VBox();
        
        Text hdr = new Text("Strecke");
        box.getChildren().add(hdr);
        
        pendingHButton = createSizedButton("Zug von H", STD_BUTTON_SIZE);
        pendingHButton.setOnAction(ev -> {
            config.pendingTrainH = PENDING_TRAIN_DURATION;
        });
        box.getChildren().add(pendingHButton);
        
        var schlF = createSizedButton("Schlüssel F", STD_BUTTON_SIZE);
        schlF.setOnAction(ev -> {
            condReleaseFahrstrasse(config.fahrstrassen[2]);
            condReleaseFahrstrasse(config.fahrstrassen[3]);
        });
        box.getChildren().add(schlF);
        
        pendingMButton = createSizedButton("Zug von M", STD_BUTTON_SIZE);
        pendingMButton.setOnAction(ev -> {
            config.pendingTrainM = PENDING_TRAIN_DURATION;
        });
        box.getChildren().add(pendingMButton);
        
        var schlA = createSizedButton("Schlüssel A", STD_BUTTON_SIZE);
        schlA.setOnAction(ev -> {
            condReleaseFahrstrasse(config.fahrstrassen[0]);
            condReleaseFahrstrasse(config.fahrstrassen[1]);
        });
        box.getChildren().add(schlA);
        
        box.setSpacing(5);
        this.getChildren().add(box);
    }
    
    /**
     * Erzeugt einen Button mit voreingestellten Eigenschaften
     * für den MainPane Dialog.
     * 
     * @param text
     * @param prefWidth
     * @return 
     */
    private Button createSizedButton(String text, int prefWidth) {
        var button = new Button(text);
        button.setPrefWidth(prefWidth);
        return button;
    }
    
    /**
     * Ein inactivityCounter überwacht alle Tastendrücke.
     * Wenn eine voreingestellte Zeit keine Aktivität
     * war, wird die 24Volt Versorgung abgeschaltet und
     * damit alle Lampen und alle Taster. Über diesen
     * Button kann die Spannung wieder eingeschaltet
     * werden oder die Abschaltung verzögert werden.
     */
    private void addSchalter() {
        var msgColumn = new VBox();
        msgColumn.setSpacing(5);
        
        messages = new TextArea();
        messages.setPrefHeight(800.0);
        messages.setPrefWidth(800);
        msgColumn.getChildren().add(messages);
        

        var box = new HBox();
        box.setSpacing(5);
        
        var clear = new Button("Nachrichten löschen");
        clear.setOnAction(ev -> {
            messages.clear();
        });
        box.getChildren().add(clear);
        
        totmann = new Button("Totmannschalter");
        totmann.setOnAction(ev -> {
            config.connector.resetInactivityCounter();
        });
        box.getChildren().add(totmann);
        
        var quit = new Button("Beenden");
        quit.setOnAction(ev -> {
            try {
                config.ticker.interrupt();
                config.connector.switchOff();
                config.connector.tick(0);
                
                Platform.runLater(() -> {
                    config.stage.close();
                });
                        
            } catch (Exception ex) {
                System.out.println("Error on closing app: " + ex);
            }
        });
        
        box.getChildren().add(quit);
        
        msgColumn.getChildren().add(box);
        this.getChildren().add(msgColumn);
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
        box.setMinWidth(STD_BUTTON_SIZE+ 30);
        box.setPrefWidth(STD_BUTTON_SIZE + 30);

        Text hdr = new Text("Weichen");
        box.getChildren().add(hdr);
        
        for (Weiche weiche: config.weichen) {
            WeicheFx wfx = new WeicheFx(weiche);
            box.getChildren().add(wfx);
            config.ticker.add(wfx);
        }
        
        this.getChildren().add(box);
    }
    
    /**
     * Fügt die Liste der Signal Controls in
     * die MainPane ein.
     */
    private void addSignale() {
        VBox box = new VBox(5);
        box.setMinWidth(STD_BUTTON_SIZE);
        box.setPrefWidth(STD_BUTTON_SIZE);
        
        Text hdr = new Text("Signale");
        box.getChildren().add(hdr);
        
        for (Signal signal: config.signale) {
            SignalFx sfx = new SignalFx(signal);
            box.getChildren().add(sfx);
            config.ticker.add(sfx);
        }
        
        this.getChildren().add(box);
    }
    
    /**
     * Fügt die Liste der Ersatzsignal Controls in
     * die MainPane ein.
     */
    private void addErsatzsignale() {
        VBox box = new VBox(5);
        Text hdr = new Text("Ersatzsignale");
        box.getChildren().add(hdr);
        box.setMinWidth(STD_BUTTON_SIZE);
        
        for (Ersatzsignal signal: config.ersatzsignale) {
            ErsatzsignalFx sfx = new ErsatzsignalFx(signal);
            box.getChildren().add(sfx);
            config.ticker.add(sfx);
        }
        
        this.getChildren().add(box);
    }
    
    /**
     * Fügt die Liste der Fahrstrassen Controls in
     * die MainPane ein.
     */
    private void addFahrstrassen() {
        VBox box = new VBox(5);
        Text hdr = new Text("Fahrstrassen");
        box.getChildren().add(hdr);
        box.setMinWidth(STD_BUTTON_SIZE);
        
        for (var fahrstrasse: config.fahrstrassen) {
            var ffx = new FahrstrasseFx(fahrstrasse);
            box.getChildren().add(ffx);
            config.ticker.add(ffx);
        }
        
        this.getChildren().add(box);
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

    @Override
    public void tick(int count) {
        if (pendingH != (config.pendingTrainH > 0)) {
            pendingH = !pendingH;
            pendingHButton.setStyle(pendingH ? "-fx-background-color: lightblue" : "");
        }
        
        if (pendingM != (config.pendingTrainM > 0)) {
            pendingM = !pendingM;
            pendingMButton.setStyle(pendingM ? "-fx-background-color: lightblue" : "");
        }
    }
}
