/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.fx;

import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.Weiche;
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
 * Diese Klasse visualisiert den Zustand einer
 * Weiche.
 * 
 * Zudem kann der Zustand der Weiche über das
 * User Interface verändert werden. Durch Klick
 * auf das Label "Stellung" kann die Weiche umgestellt
 * werden.
 * 
 * @author pi
 */
public class WeicheFx extends GridPane implements TickerEvent {

    private final Weiche weiche;
    private final Text stellung;
    private final Text verschluss;
    
    /**
     * Der Konstruktor enthält die Weiche deren
     * Zustand angezeigt werden soll.
     * 
     * @param weiche 
     */
    public WeicheFx(Weiche weiche) {
        BorderStroke borderStroke = new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, new CornerRadii(3),
                new BorderWidths(1));
        Border border = new Border(borderStroke);   
        this.setBorder(border);
        this.setPadding(new Insets(5, 5, 5,5));
        
        this.setHgap(5);
        this.weiche = weiche;
        Text name = new Text(weiche.getName());
        this.add(name, 0, 0, 2, 1);
        
        // Stellung
        Text labelStellung = new Text("Stellung");
        labelStellung.setOnMouseClicked(ev -> {
            weiche.whenPressed();
        });
        this.add(labelStellung, 0, 1);
        stellung = new Text("unbekannt");
        this.add( stellung, 1, 1);
        
        // Verschluss
        Text labelVerschluss = new Text("Verschluss");
        this.add(labelVerschluss, 0, 2);
        verschluss = new Text("unbekannt");
        this.add(verschluss, 1, 2);
    }
    
    /**
     * Aktualisiert die Ansicht aus dem
     * aktuellen Zustand der Weiche.
     */
    public void updateView() {
        String status = weiche.isPlus() ? "Plus" : "Minus";
        if (weiche.isRunning()) {
            status = "Umlauf";
        }
        stellung.setText(status);
        
        verschluss.setText(weiche.isLocked() ? "Verschlossen" : "Frei");
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
