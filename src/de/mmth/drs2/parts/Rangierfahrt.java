/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2025 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
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
    private boolean addPendingClear;
    private int waitCounter = 0;
    
    
    private enum ActionType {
        CheckGleis,
        ClearGleis,
        SetGleis,
        WaitSh1,
        HaltSh1,
        WaitSW,
        CheckWeiche,
        CheckWeicheMitStoerung,
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
        
        @Override
        public String toString() {
            var msg = "Action: " + action.name() + ", param: " + param + ", param2: " + param2;
            return msg;
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
        
        /*for (var i = 0; i < fahrweg.length(); i++) {
            addStep(fahrweg.charAt(i));
        }*/
        String[] parts = fahrweg.split(" ");
        for (var part: parts) {
          addStep(part);
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
        System.out.println(step.toString());
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
                
            case WaitSW:
                if (config.schluesselweichen[step.param].isLocked()) {
                    position++;
                } else {
                    config.alert("Warte auf Schlüsselweiche");
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
                
                if (config.weichen[step.param].isGestoert()) {
                    config.alert("Weiche gestört und kann nicht befahren werden.");
                    return;
                }
                
                if (config.weichen[step.param].isRunning()) {
                    config.alert("Weiche läuft noch um.");
                    return;
                }
                
                if (config.weichen[step.param].isPlus() == step.param2) {
                    if (waitCounter == 0) {
                        config.alert("Weiche " + config.weichen[step.param].getName() + " in der falschen Position.");
                    }
                    
                    waitCounter++;
                    if (waitCounter > 10) {
                        config.stoerungsmelder.rangierMeldung();
                        waitCounter = 0;
                    }
                } else {
                    waitCounter = 0;
                    position++;
                }
                
                break;
                
            case CheckWeicheMitStoerung:
                System.out.println("Weiche " + config.weichen[step.param].isPlus());
                System.out.println("Wert " + step.param2);
                System.out.println("Position " + position);
                System.out.println("Compare " + (config.weichen[step.param].isPlus() == step.param2));
                if (config.weichen[step.param].isRunning()) {
                    config.alert("Weiche läuft noch um.");
                    return;
                }
                
                if (config.weichen[step.param].isPlus() == step.param2) {
                    config.alert("Weiche " + config.weichen[step.param].getName() + " in der falschen Position.");
                    config.weichen[step.param].setStoerung();
                    nextAction = count;
                }
                
                position++;
                
                break;
                
            case Message:
                position++;
                break;
                
            case Wait:
                position++;
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
    
    
    private void addStep(String action) {
      String msg;
      int gleis, weiche, sound, marker;
      boolean param2;
      
      switch (action.charAt(0)) {
        case 'G': // Gleis (Start oder Ziel) GS1 GZ4
          msg = "Rangierfahrt gestartet: " + name;
          gleis = action.charAt(2) - '1'; // Gleis 1..3 oder 5 -> 1..4
          var atype = (action.charAt(1) == 'S') ? ActionType.CheckGleis : ActionType.SetGleis;
          fahrweg.add(new Step(atype, gleis, SHORT_DELAY, msg));
          if (atype.equals(ActionType.CheckGleis)) {
            pendingClear = new Step(ActionType.ClearGleis, gleis, SHORT_DELAY);
          } else {
            checkPendingClear();
          }
          break;
          
        case 'Z': // Ziel erreicht
          if (addPendingClear) {
            // Ausfahrt auf Industriegleis, Gleis 3 Besetztmelder löschen.
            addPendingClear = false;
            pendingClear = new Step(ActionType.ClearGleis, 2, SHORT_DELAY);
            checkPendingClear();
          }
          
          fahrweg.add(new Step(ActionType.Stop, 0, 0, "Rangierziel erreicht."));
          break;
          
        case 'W': // Weiche befahren W5
          weiche = action.charAt(1) - '1'; // Weiche 3, 4, 5, 18, 19, 20 ->1...6
          fahrweg.add(new Step(ActionType.SetWeiche, weiche, SHORT_DELAY));
          checkPendingClear();
          pendingClear = new Step(ActionType.ClearWeiche, weiche, SHORT_DELAY);
          break;
          
        case 'C': // Weichenstellung prüfen und bei Bedarf warten CP3 CM1
          param2 = action.charAt(1) != 'P'; // Plus oder Minusstellung
          weiche = action.charAt(2) - '1'; // Weiche 3, 4, 5, 18, 19, 20 ->1...6
          System.out.println("Weiche " + weiche + " Stellung " + param2);
          fahrweg.add(new Step(ActionType.CheckWeiche, weiche, LONG_DELAY, "", param2));
          break;
        
        case 'S': // Weichenstellung prüfen und Störung im Fehlerfall SP6
          param2 = action.charAt(1) != 'P'; // Plus oder Minusstellung
          weiche = action.charAt(2) - '1'; // Weiche 3, 4, 5, 18, 19, 20 ->1...6
          System.out.println("Weiche " + weiche + " Stellung " + param2);
          fahrweg.add(new Step(ActionType.CheckWeicheMitStoerung, weiche, LONG_DELAY, "", param2));
          break;
          
        case 'F': // Warte auf FDL/ Weichenwärter
          sound = action.charAt(1) - '1'; // Klangdatei 1..9 abspielen
          fahrweg.add(new Step(ActionType.Message, 0, 0, "Zwischenziel erreicht, WW Aktion erwartet."));
          fahrweg.add(new Step(ActionType.Wait, sound, SHORT_DELAY));
          break;
          
        case 'R': // Warte auf Rangiersignal
          rangierSignal = action.charAt(1) - '1';
          fahrweg.add(new Step(ActionType.WaitSh1, rangierSignal, SHORT_DELAY));
          break;
          
        case 'X': // Rangiersignal zurückstellen
          fahrweg.add(new Step(ActionType.HaltSh1, rangierSignal, 0));
          break;

        case 'K': // Warte auf Schlüsselweiche 1,2
          weiche = action.charAt(1) - '1';
          fahrweg.add(new Step(ActionType.WaitSW, weiche, LONG_DELAY));
          addPendingClear = true;
          break;
          
        case 'M': // Lokführer meldet sich beim Weichenwärter
          sound = action.charAt(1) - '1'; // Klangdatei 1..9 abspielen
          fahrweg.add(new Step(ActionType.Meldung, sound, SHORT_DELAY));
          break;
          
        case 'B': // Streckenmarker auf belegt oder frei umstellen
          marker = action.charAt(1) == 'b' ? 40 : 42;
          fahrweg.add(new Step(ActionType.StreckeRot, marker, SHORT_DELAY));
          checkPendingClear();
          pendingClear = new Step(ActionType.StreckeClear, marker, SHORT_DELAY);
          break;
          
        case 'H':
          fahrweg.add(new Step(ActionType.HaltSh1, rangierSignal, 0));
          break;
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
