/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2.fx;

import de.mmth.drs2.Config;
import de.mmth.drs2.parts.Weiche;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 *
 * @author pi
 */
public class MainPane extends GridPane{

    private final Config config;
    
    public MainPane(Config config) {
        this.config = config;
        this.setHgap(5);
        this.setVgap(5);
        
        VBox box = new VBox(5);
        Text hdr = new Text("Weichen");
        box.getChildren().add(hdr);
        
        for (Weiche weiche: config.weichen) {
            WeicheFx wfx = new WeicheFx(weiche);
            box.getChildren().add(wfx);
            config.ticker.add(wfx);
        }
        
        this.add(box, 0, 0);
    }
}
