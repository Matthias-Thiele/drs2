/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.fx;

import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.Ersatzsignal;
import de.mmth.drs2.parts.Signal;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 *
 * @author pi
 */
public class ErsatzsignalFx extends GridPane implements TickerEvent {

    private final Ersatzsignal signal;
    private final Text name;
    
    /**
     * Der Konstruktor enthält das Signal dessen
     * Zustand angezeigt werden soll.
     * 
     * @param signal 
     */
    public ErsatzsignalFx(Ersatzsignal signal) {
        BorderStroke borderStroke = new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, new CornerRadii(3),
                new BorderWidths(1));
        Border border = new Border(borderStroke);   
        this.setBorder(border);
        this.setPadding(new Insets(5, 5, 5,5));
        
        this.setHgap(5);
        this.signal = signal;
        name = new Text(signal.toString());
        this.add(name, 0, 0, 2, 1);
        name.setOnMouseClicked(ev -> {
            signal.whenPressed();
            updateView();
        });
    }
    
    /**
     * Aktualisiert die Ansicht aus dem
     * aktuellen Zustand der Weiche.
     */
    public void updateView() {
        name.setText(signal.toString());
    }
    
    /**
     * Löst regelmäßig eine Aktualisierung
     * der Ansicht aus.
     * @param count 
     */
    @Override
    public void tick(int count) {
        Platform.runLater(() -> {
            updateView();
        });
    }
    
}
