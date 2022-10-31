/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.fx;

import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.Fahrstrasse;
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
 * Diese Klasse visualisiert den Zustand einer Fahrstraße.
 * Zudem können über das User Interface verschiedene
 * Funktionen der Fahrstraße, wie z.B. Belegen oder Auflösen
 * aktiviert werden.
 * 
 * @author pi
 */
public class FahrstrasseFx extends GridPane implements TickerEvent {

    private final Fahrstrasse fahrstrasse;
    private final Text verschluss;
    
    /**
     * Der Konstuktor übernimmt die Fahrstraße, die
     * angzeigt werden soll.
     * 
     * @param fahrstrasse 
     */
    public FahrstrasseFx(Fahrstrasse fahrstrasse) {
        BorderStroke borderStroke = new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, new CornerRadii(3),
                new BorderWidths(1));
        Border border = new Border(borderStroke);   
        this.setBorder(border);
        this.setPadding(new Insets(5, 5, 5,5));
        
        this.setHgap(5);
        this.fahrstrasse = fahrstrasse;
        Text name = new Text(fahrstrasse.getName());
        this.add(name, 0, 0, 2, 1);
        
        // Verschluss
        Text labelVerschluss = new Text("Verschluss");
        this.add(labelVerschluss, 0, 2);
        verschluss = new Text("unbekannt");
        this.add(verschluss, 1, 2);
        
        updateView();
    }

    /**
     * Aktualisiert die Ansicht aus dem aktuellen
     * Fahrstraßenzustand.
     */
    public void updateView() {
        verschluss.setText(fahrstrasse.isLocked() ? "Verschlossen" : "Frei");
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
