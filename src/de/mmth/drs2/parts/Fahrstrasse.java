/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;

/**
 * Diese Klasse verwaltet die Funktionen zum
 * Festlegen, Auflösen und Betreiben einer
 * Fahrstraße.
 * 
 * @author pi
 */
public class Fahrstrasse implements TastenEvent, TickerEvent {
    
    private final static int SIGNAL_FIRST_OUTBOUND = 2;
    private final static int STEP_SHORT_WAIT = 20;
    private final static int STEP_LONG_WAIT = 3 * STEP_SHORT_WAIT;
    private final static int DORMANT = -1;
    private final static int INBOUND_RED = -2;
    private final static int INIT = -3;
    private final static int SIGNAL_HP0 = -4;
    private final static int DONE = -5;
    private final static int AUSFAHRT1 = -6;
    private final static int AUSFAHRT2 = -7;
    private final static int INCOMMING_TRAIN = -8;
    private final static int OUTGOING_TRAIN = -9;
    private final static int WAIT_FOR_HP1 = -10;
    private final static int EINFAHRT1 = -11;
    private final static int SET_HP1 = -12;
    private final static int WAIT_FOR_TRAIN = -13;
    
    private final static int RED_DELTA_TICKS = STEP_SHORT_WAIT / 2;
    
    private Config config;
    private Weiche[] plusWeichen;
    private Weiche[] minusWeichen;
    private Weiche[] fahrwegWeichen;
    private Doppeltaster fahrstrassenAktivierung;
    private Doppeltaster hilfsaufloesung;
    
    private boolean isLocked = false;
    private Gleismarker bahnhofsGleis;
    private String name;
    private Signal signal;
    private boolean isInbound;
    private boolean reportWait;
    private boolean pendingTrain;
    
    private int state = DORMANT;
    private int nextStep = -1;
    private Gleismarker ausfahrtsGleis;
    private Streckenblock strecke;
    private int ersatzSignalNummer;
    private int festlegemelder;
    private int sperrRaeumungsmelder;
    private Doppeltaster tasteAsT;
    private Doppeltaster tasteAsLT;
    
    private ColorMarker lastRed;
    private ColorMarker nextWhite;
    private long nextWhiteTStamp = Integer.MAX_VALUE;
    
    private int verbundeneEinfahrt = -1;
    private String streckeName;
    
    /**
     * Initialisiert die Parameter der Fahrstraße.
     * 
     * @param config
     * @param name
     * @param plusWeichen diese Weichen müssen in Plus Stellung stehen
     * @param minusWeichen diese Weichen müssen in Minus Stellung stehen
     * @param fahrwegWeichen
     * @param signalTaste
     * @param gleisTaste
     * @param bahnhofsGleis
     * @param signalNummer Ein- oder Ausfahrtsignal zu dieser Fahrstraße.
     * @param ersatzSignalNummer
     * @param ausfahrtsGleis
     * @param streckeName
     * @param streckeWeiss
     * @param streckeRot
     * @param streckeTaster
     * @param festlegemelder
     * @param sperrRaeumungsmelder
     */
    public void init(Config config, String name, int[] plusWeichen, int[] minusWeichen, int[] fahrwegWeichen, 
            int signalTaste, int gleisTaste, Gleismarker bahnhofsGleis, int signalNummer, int ersatzSignalNummer,
            Gleismarker ausfahrtsGleis, String streckeName, int streckeWeiss, int streckeRot, int streckeTaster,
            int festlegemelder, int sperrRaeumungsmelder) {
        this.config = config;
        this.name = name;
        this.streckeName = streckeName;
        
        this.plusWeichen = new Weiche[plusWeichen.length];
        for (int i = 0; i < plusWeichen.length; i++) {
            this.plusWeichen[i] = config.weichen[plusWeichen[i]];    
        }
        
        this.minusWeichen = new Weiche[minusWeichen.length];
        for (int i = 0; i < minusWeichen.length; i++) {
            this.minusWeichen[i] = config.weichen[minusWeichen[i]];
        }
        
        this.fahrwegWeichen = new Weiche[fahrwegWeichen.length];
        for (int i = 0; i <fahrwegWeichen.length; i++) {
            this.fahrwegWeichen[i] = config.weichen[fahrwegWeichen[i]];
        }
        
        fahrstrassenAktivierung = new Doppeltaster();
        if (signalTaste >= 0) {
            fahrstrassenAktivierung.init(config, this, gleisTaste, signalTaste);
        }
        
        hilfsaufloesung = new Doppeltaster();
        hilfsaufloesung.init(config, this, Const.FHT, signalTaste);
        
        this.bahnhofsGleis = bahnhofsGleis;
        this.ausfahrtsGleis = ausfahrtsGleis;
        this.signal = config.signale[signalNummer];
        this.ersatzSignalNummer = ersatzSignalNummer;
        isInbound = signalNummer < SIGNAL_FIRST_OUTBOUND;
        
        strecke = new Streckenblock();
        strecke.init(config, streckeName, isInbound, streckeTaster, streckeWeiss, streckeRot,
                sperrRaeumungsmelder);
        
        this.festlegemelder = festlegemelder;
        this.sperrRaeumungsmelder = sperrRaeumungsmelder;
        
        if (streckeTaster >= 0) {
            tasteAsT = new Doppeltaster();
            tasteAsT.init(config, this, Const.AsT, streckeTaster);
            config.ticker.add(tasteAsT);
            
            tasteAsLT = new Doppeltaster();
            tasteAsLT.init(config, this, Const.AsLT, streckeTaster);
            config.ticker.add(tasteAsLT);
        }
        
        config.ticker.add(this);
    }
    
    /**
     * Bei Durchfahrten wird die Einfahrtsfahrstraße
     * automatisch aufgelöst sobald der Zug den
     * Bahnhof verlassen hat.
     * 
     * @param verbundeneEinfahrt 
     */
    public void addEinfahrt(int verbundeneEinfahrt) {
        this.verbundeneEinfahrt = verbundeneEinfahrt;
    }

    /**
     * Callback Funktion vom Doppeltaster.
     * Der Fahrdienstleiter hat die Signaltaste
     * und die Gleistaste betätigt. Das System
     * versucht nun die Fahrstraße einzurichten.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        switch (taste1) {
            case Const.FHT:
                fahrstrassenaufloesung();
                break;
                
            case Const.AsT:
                config.connector.setOut(festlegemelder, true);
                break;
                
            case Const.AsLT:
                config.connector.setOut(festlegemelder, false);
                break;
                
            default:
                fahrstrassenfestlegung();
        }
    }
    
    /**
     * Callback Funktion vom Doppeltaster.
     * Der Fahrdienstleiter hat die FHT Taste
     * und die Signaltaste bedient um die
     * Fahrstraße manuell aufzulösen.
     */
    private void fahrstrassenaufloesung() {
        config.alert("Fahrstraße manuell aufgelöst.");
        pendingTrain = (state == INIT) || (state == WAIT_FOR_HP1);
        signal.clear();
        unlock();
        strecke.cancel();
        state = DORMANT;
    }
    
    /**
     * Callback Funktion vom Doppeltaster.
     * Der Fahrdienstleiter hat die Signaltaste
     * und die Gleistaste betätigt. Das System
     * versucht nun die Fahrstraße einzurichten.
     */
    private void fahrstrassenfestlegung() {
        config.alert("Die Fahrstrasse " + name + " wurde ausgewählt.");
        if (isLocked) {
            config.alert("Die Fahrstrasse " + name + " ist bereits verrigelt.");
            return;
        }
        
        if (!strecke.isFree()) {
            config.alert("Der Streckenblock ist noch belegt.");
            return;
        }
        
        // Zielgleis prüfen
        if (isInbound && bahnhofsGleis.isInUse()) {
            config.alert("Das Zielgleis ist bereits belegt.");
            return;
        }
        
        // Weichenstellung prüfen.
        for (Weiche plusWeiche : plusWeichen) {
            if (!plusWeiche.isPlus()) {
                config.alert("Die Weiche " + plusWeiche.getName() + " ist nicht in Plus Stellung.");
                return;
            }
            if (plusWeiche.isRunning()) {
                config.alert("Die Weiche " + plusWeiche.getName() + " ist gestört oder läuft noch um.");
                return;
            }
        }
        
        for (Weiche minusWeiche : minusWeichen) {
            if (minusWeiche.isPlus()) {
                config.alert("Die Weiche " + minusWeiche.getName() + " ist nicht in Minus Stellung.");
                return;
            }
            if (minusWeiche.isRunning()) {
                config.alert("Die Weiche " + minusWeiche.getName() + " ist gestört oder läuft noch um.");
                return;
            }
        }
        
        // Weichen verrigeln
        for (Weiche plusWeiche : plusWeichen) {
            plusWeiche.lock();
        }

        for (Weiche minusWeiche : minusWeichen) {
            minusWeiche.lock();
        }
        
        isLocked = true;
        if (!bahnhofsGleis.isInUse()) {
            bahnhofsGleis.white();
        }
        
        if (!this.isInbound) {
            ausfahrtsGleis.white();
        }
        
        if (isInbound) {
            signal.fahrt();
        }
        
        state = INIT;
        if (isInbound && pendingTrain) {
            state = WAIT_FOR_HP1;
            config.alert("Begonnen Fahrt wird fortgesetzt.");
        }
        
        reportWait = true;
        config.alert("Die Fahrstraße " + name + " wurde verschlossen.");
    }
    
    /**
     * Fahrstraße auflösen.
     * 
     * Dabei werden alle Weichensperren zurückgenommen
     * und die Anzeige DRS 2 aktualisiert.
     */
    public void unlock() {
        if (!isLocked) {
            config.alert("Die Fahrstraße ist nicht verschlossen.");
            return;
        }
        
        // Weichen entrigeln
        for (Weiche plusWeiche : plusWeichen) {
            plusWeiche.unlock();
        }

        for (Weiche minusWeiche : minusWeichen) {
            minusWeiche.unlock();
        }
        
        isLocked = false;
        if (!bahnhofsGleis.isInUse()) {
            bahnhofsGleis.clear();
        }
        
        config.connector.setOut(festlegemelder, false);
        config.alert("Die Fahrstraße " + name + " wurde aufgelöst.");
    }
    
    /**
     * Liefert den Namen der Fahrstraße zurück.
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Liefert zurück, ob die Fahrstraße verschlossen ist.
     * @return 
     */
    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public void tick(int count) {
        if (count > nextWhiteTStamp) {
            System.out.println("set white: " + nextWhite.getName() + " at " + count);
            nextWhite.white();
            nextWhiteTStamp = Integer.MAX_VALUE;
        }
        
        if (isInbound) {
            inboundTick(count);
        } else {
            outboundTick(count);
        }
    }
    
    /**
     * Statemaschine für Ausfahrten.
     * @param count 
     */
    public void outboundTick(int count) {
        if (count < nextStep) {
            return;
        }
        
        switch (this.state) {
            case  DORMANT:
                return; // do nothing
                
            case INIT:
                signal.white();
                config.connector.setOut(festlegemelder, true);
                nextStep = count + STEP_SHORT_WAIT;
                state = SET_HP1; // Fahrstraße wurde ausgewählt.
                break;
                
            case SET_HP1:
                signal.fahrt();
                if (bahnhofsGleis.isInUse()) {
                    config.alert("Fahrt gestartet.");
                    state = WAIT_FOR_HP1; // Fahrstraße wurde ausgewählt.
                    setRed(count, bahnhofsGleis); // damit setWhite funktioniert
                    reportWait = true;
                } else {
                    if (reportWait) {
                        config.alert("Warte auf Zug.");
                        reportWait = false;
                    }
                }
                
                nextStep = count + STEP_SHORT_WAIT;
                break;
                
            case WAIT_FOR_HP1:
                boolean isFahrt = signal.isFahrt();
                if (!isFahrt && (ersatzSignalNummer >= 0)) {
                    isFahrt = config.ersatzsignale[ersatzSignalNummer].isFahrt();
                }
                
                if (isFahrt) {
                    strecke.activateSR();
                    nextStep = count + STEP_LONG_WAIT;
                    state = INBOUND_RED;
                } else {
                    if (reportWait) {
                        config.alert("Zug wartet auf HP1.");
                        reportWait = false;
                    }
                }
                break;
                
            case INBOUND_RED:
                config.alert("Zug fährt aus.");
                setRed(count, signal);
                nextStep = count + STEP_LONG_WAIT;
                state = SIGNAL_HP0;
                break;
            
            case SIGNAL_HP0:
                config.alert("Signal auf Halt.");
                signal.halt();
                nextStep = count + STEP_SHORT_WAIT;
                state = 0; // erste Weiche
                break;
              
            case AUSFAHRT1:
                strecke.markUsed(false);
                setRed(count, ausfahrtsGleis);
                nextStep = count + STEP_LONG_WAIT;
                state = AUSFAHRT2;
                break;
                
            case AUSFAHRT2:
                config.alert("Fahrt abgeschlossen.");
                fahrwegWeichen[fahrwegWeichen.length - 1].white();
                nextStep = count + STEP_LONG_WAIT;
                state = OUTGOING_TRAIN;
                break;
                
            case OUTGOING_TRAIN:
                nextStep = count + STEP_LONG_WAIT;
                // Ausfahrtsgleis erreicht
                config.alert("Bahnhof verlassen.");
                unlock();
                if (verbundeneEinfahrt >= 0) {
                    // bei Durchfahrten wird die Einfahrt automatisch aufgelöst.
                    config.fahrstrassen[verbundeneEinfahrt].unlock();
                }
                state = DONE;
                
            case DONE:
                strecke.unblock();
                config.connector.setOut(festlegemelder, false);
                ausfahrtsGleis.clear();
        
                lastRed = null;
                state = DORMANT;
                break;
                
            default:
                // Weiche 0 bis n
                int weiche = state;
                
                if (weiche < fahrwegWeichen.length) {
                    config.alert("Zug bei Weiche " + fahrwegWeichen[weiche].getName());
                    setRed(count, fahrwegWeichen[weiche]);
                } else {
                    config.alert("Ausfahrtsgleis erreicht.");
                    nextStep = count + STEP_SHORT_WAIT;
                    state = AUSFAHRT1;
                    break;
                }
                nextStep = count + STEP_SHORT_WAIT;
                
                state++;
                break;
        }
    }
    
    /**
     * Statemaschine für Einfahrten.
     * @param count 
     */
    public void inboundTick(int count) {
        if (count < nextStep) {
            return;
        }
        
        if (state != -1) {
            System.out.println("State: " + state + " at " + count);
        }
        
        switch (state) {
            case  DORMANT:
                return; // do nothing
                
            case INIT:
                signal.white();
                config.connector.setOut(festlegemelder, true);
                nextStep = count + STEP_LONG_WAIT;
                state = WAIT_FOR_TRAIN; // Fahrstraße wurde ausgewählt.
                reportWait = true;
                config.alert("Warte auf Zugfahrt von " + streckeName);
                break;
                
            case WAIT_FOR_TRAIN:
                var trainStarted = (streckeName.equals("H")) ? config.pendingTrainH > 0 : config.pendingTrainM > 0;
                
                if (trainStarted) {
                    state = INCOMMING_TRAIN; // Zug wurde gestartet
                    config.alert("Fahrt gestartet.");
                    nextStep = count + STEP_LONG_WAIT;
                } else {
                    nextStep = count + STEP_SHORT_WAIT;
                }
                break;
                
            case INCOMMING_TRAIN:
                strecke.markUsed(false);
                state = WAIT_FOR_HP1;
                nextStep = count + STEP_LONG_WAIT;
                break;
                
            case WAIT_FOR_HP1:
                boolean isFahrt = signal.isFahrt();
                if (!isFahrt && (ersatzSignalNummer >= 0)) {
                    isFahrt = config.ersatzsignale[ersatzSignalNummer].isFahrt();
                }
                
                if (isFahrt) {
                    nextStep = count + STEP_LONG_WAIT;
                    state = INBOUND_RED;
                } else {
                    if (reportWait) {
                        config.alert("Zug wartet auf HP1.");
                        reportWait = false;
                    }
                }
                break;
                
            case INBOUND_RED:
                config.alert("Zug im Signalblock.");
                setRed(count, signal);
                nextStep = count + STEP_LONG_WAIT;
                state = SIGNAL_HP0; // Signal wieder auf Halt zurückstellen.
                break;
            
            case SIGNAL_HP0:
                signal.halt();
                nextStep = count + STEP_SHORT_WAIT;
                state = 0; // erste Weiche
                break;
                
            case EINFAHRT1:
                // Zielgleis erreicht
                config.alert("Fahrt beendet.");
                signal.clear();
                strecke.markUsed(true);
                lastRed = null;
                state = DORMANT;
                break;
            
            default:
                // Weiche 0 bis n
                
                int weiche = state;
                
                if (weiche == 0) {
                    // erste Weiche, es gibt keinen Vorgänger, sondern nur
                    // den Streckenabschnitt zum Einfahrtssignal.
                    config.alert("Zug verlässt Signalblock.");
                    strecke.activateSR();
                }
                
                nextStep = count + STEP_SHORT_WAIT;
                if (weiche < fahrwegWeichen.length) {
                    config.alert("Zug bei Weiche " + fahrwegWeichen[weiche].getName());
                    setRed(count, fahrwegWeichen[weiche]);
                } else {
                    // Zielgleis wird angefahren
                    config.alert("Zielgleis erreicht.");
                    setRed(count, bahnhofsGleis);
                    nextStep = count + STEP_LONG_WAIT;
                    state = EINFAHRT1;
                    return;
                }
                
                state++;
                break;
        }
    }
    
    private void setRed(int actTick, ColorMarker nextRed) {
        if (!nextRed.hasMarker()) {
            return;
        }
        
        nextWhite = lastRed;
        if (nextWhite != null) {
            nextWhiteTStamp = actTick + RED_DELTA_TICKS;
            System.out.println("set white timer to " + nextWhiteTStamp);
        }
        
        lastRed = nextRed;
        if (nextRed != null) {
            nextRed.red();
            System.out.println("set red: " + nextRed.getName());
        }
    }
}
