/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2025 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Matthias Thiele
 */
public class Rangierfahrt implements TickerEvent {
    private static int SHORT_DELAY = 20;
    private static int LONG_DELAY = 80;
    
    private Config config;
    private String name;
    private List<Step> fahrweg = new ArrayList<>();
    
    private int position = -1;
    private int nextAction = 0;
    private Step pendingClear = null;
    private int rangierSignal;
    
    
    private enum ActionType {
        CheckGleis,
        ClearGleis,
        SetGleis,
        WaitSh1,
        HaltSh1,
        CheckWeiche,
        ClearWeiche,
        SetWeiche,
        Message,
        Wait,
        Meldung,
        StreckeRot,
        StreckeClear,
        Stop
    }
    
    private class Step {
        ActionType action;
        int param;
        boolean param2;
        int delay;
        String message;
        
        protected Step(ActionType at, int param, int delay) {
            this.action = at;
            this.param = param;
            this.param2 = false;
            this.delay = delay;
            this.message = "";
        }
        
        protected Step(ActionType at, int param, int delay, String message) {
            this.action = at;
            this.param = param;
            this.param2 = false;
            this.delay = delay;
            this.message = message;
        }
        
        protected Step(ActionType at, int param, int delay, String message, boolean param2) {
            this.action = at;
            this.param = param;
            this.param2= param2;
            this.delay = delay;
            this.message = message;
        }
    }
    /**
     * Initialisiert einen Rangierweg.
     * 
     * Der fahrweg String enthält in codierter Form die Start und
     * Endpunkte sowie die Weichenpositionen und die Gleismarker.
     * 
     * @param config
     * @param name
     * @param fahrweg 
     */
    public void init(Config config, String name, String fahrweg) {
        this.config = config;
        this.name = name;
        
        for (var i = 0; i < fahrweg.length(); i++) {
            addStep(fahrweg.charAt(i));
        }
        
        config.ticker.add(this);
    }

    public String getName() {
        return name;
    }
    
    /**
     * Startet eine Rangierfahrt. Es wird nur geprüft, ob auf dem
     * Startgleis ein Zug steht, das Ziel und die Weichenstellung
     * wird nicht geprüft, das ist die Aufgabe des Weichenwärters.
     */
    public void start() {
        if (position != -1) {
            if (!resume()) {
                config.alert("Rangierfahrt abgebrochen.");
                position = -1;
            }
        } else if (!checkStartGleis(fahrweg.get(0).param)) {
            config.alert("Startgleis ist nicht besetzt.");
        } else {
            position = 0;
        }
    }
    
    private boolean resume() {
        Step step = fahrweg.get(position);
        if (step.action == ActionType.Wait) {
            config.alert("Rangierfahrt wird fortgesetzt.");
            position++;
            return true;
        }
        
        return false;
    }
    
    private void advance(int count) {
        Step step = fahrweg.get(position);
        nextAction = count + step.delay;
        if (!step.message.isEmpty()) {
            config.alert(step.message);
        }
        
        switch (step.action) {
            case WaitSh1:
                if (config.signale[step.param].isSh1()) {
                    position++;
                } else {
                    config.alert("Warte auf Sh1");
                }
                break;
            
            case HaltSh1:
                config.signale[step.param].halt();
                position++;
                break;
                
            case CheckGleis:
                position++;
                break;
                
            case ClearGleis:
                config.gleise[step.param].clear();
                position++;
                break;
                
            case SetGleis:
                config.gleise[step.param].red();
                position++;
                break;
                
            case SetWeiche:
                config.weichen[step.param].red();
                position++;
                break;
                
            case ClearWeiche:
                config.weichen[step.param].white();
                position++;
                break;
            
            case CheckWeiche:
                System.out.println("Weiche " + config.weichen[step.param].isPlus());
                System.out.println("Wert " + step.param2);
                System.out.println("Position " + position);
                System.out.println("Compare " + (config.weichen[step.param].isPlus() == step.param2));
                if (config.weichen[step.param].isPlus() == step.param2) {
                    config.alert("Weiche " + config.weichen[step.param].getName() + " in der falschen Position.");
                } else {
                    position++;
                }
                break;
                
            case Message:
                position++;
                break;
                
            case Wait:
                break;
                
            case Stop:
                position = -1;
                break;
                
            case Meldung:
                position++;
                config.stoerungsmelder.rangierMeldung();
                break;
                
            case StreckeRot:
                position++;
                config.connector.setOut(step.param, true);
                break;
                
            case StreckeClear:
                position++;
                config.connector.setOut(step.param, false);
                break;
                
            default:
                config.alert("Unbekanntes Kommando: " + step.action);
                position = -1;
        }
    }
    
    private void addStep(char action) {
        if (action >= '1' && action <= '4') {
            // Startgleis
            var msg = "Rangierfahrt gestartet: " + name;
            fahrweg.add(new Step(ActionType.CheckGleis, action - '1', SHORT_DELAY, msg));
            pendingClear = new Step(ActionType.ClearGleis, action - '1', SHORT_DELAY);
        } else if (action == '.') {
            // Ende
            fahrweg.add(new Step(ActionType.Stop, 0, 0, "Rangierziel erreicht."));
        } else if (action >= 'A' && action <= 'F') {
            // Weiche befahren
            fahrweg.add(new Step(ActionType.SetWeiche, action - 'A', SHORT_DELAY));
            checkPendingClear();
            
            pendingClear = new Step(ActionType.ClearWeiche, action - 'A', SHORT_DELAY);
        } else if (action >= 'H' && action <= 'S') {
            // Weichenstellung prüfen
            boolean param2 = (action & 1) == 1;
            int weicheNr = (action - 'H') >> 1;
            System.out.println("Weiche " + weicheNr + " Stellung " + param2);
            fahrweg.add(new Step(ActionType.CheckWeiche, weicheNr, LONG_DELAY, "", param2));
        } else if (action == 'W') {
            // Warte auf Weichenwärter
            fahrweg.add(new Step(ActionType.Message, 0, 0, "Zwischenziel erreicht, WW Aktion erwartet."));
            fahrweg.add(new Step(ActionType.Wait, 0, SHORT_DELAY));
        } else if (action >= '5' && action <= '8') {
            // Zielgleis erreicht
            fahrweg.add(new Step(ActionType.SetGleis, action - '5', SHORT_DELAY));
            checkPendingClear();
        } else if (action >= 'a' && action <= 'h') {
            // Warte auf Rangiersignal
            rangierSignal = action - 'a';
            fahrweg.add(new Step(ActionType.WaitSh1, rangierSignal, SHORT_DELAY));
        } else if (action == '9') {
            // Lokführer meldet sich beim Weichenwärter
            fahrweg.add(new Step(ActionType.Meldung, 0, SHORT_DELAY));
        } else if (action >= 'X' && action <= 'Y') {
            // Streckenmarker auf belegt oder frei umstellen
            int marker = action == 'X' ? 40 : 42;
            fahrweg.add(new Step(ActionType.StreckeRot, marker, SHORT_DELAY));
            checkPendingClear();
            
            pendingClear = new Step(ActionType.StreckeClear, marker, SHORT_DELAY);
        } else if (action == 'Z') {
            fahrweg.add(new Step(ActionType.HaltSh1, rangierSignal, 0));
        } else {
            config.alert("Fehler in der Konfiguration: " + action);
        }
    }
    
    private void checkPendingClear() {
        if (pendingClear != null) {
            fahrweg.add(pendingClear);
            pendingClear = null;
        }
    }
    
    private boolean checkStartGleis(int gleis) {
        if (gleis == 3) {
            return true; // Industriegleis, kein Gleismarker, da ist immer ein Zug
        } else {
            return config.gleise[gleis].isInUse();
        }
    }
    
    @Override
    public void tick(int count) {
        if (count >= nextAction && position != -1) {
            advance(count);
        }
    }
}
