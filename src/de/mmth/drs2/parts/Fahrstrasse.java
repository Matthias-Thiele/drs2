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
    private final static int WAITERSATZ = -14;
    
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
    private Strecke strecke;
    private int ersatzSignalNummer;
    
    private ColorMarker lastRed;
    private ColorMarker nextWhite;
    private long nextWhiteTStamp = Integer.MAX_VALUE;
    
    private int verbundeneEinfahrt = -1;
    private boolean ersatzSignalFahrt;
    private int schluesselweiche1;
    private int schluesselweiche2;
    private Weiche pruefungPlus;
    private Weiche pruefungMinus;
    
    private boolean pendingHalt = false;
    
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
     * @param strecke
     * @param streckeTaster
     * @param schluesselweiche1
     * @param schluesselweiche2
     * @param pruefungPlus
     * @param pruefungMinus
     * @param aufloesungsTaste
     */
    public void init(Config config, String name, int[] plusWeichen, int[] minusWeichen, int[] fahrwegWeichen, 
            int signalTaste, int gleisTaste, Gleismarker bahnhofsGleis, int signalNummer, int ersatzSignalNummer,
            Gleismarker ausfahrtsGleis, Strecke strecke, int streckeTaster, int schluesselweiche1, int schluesselweiche2,
            int pruefungPlus, int pruefungMinus, int aufloesungsTaste) {
        this.config = config;
        this.name = name;
        this.strecke = strecke;
        
        this.plusWeichen = new Weiche[plusWeichen.length];
        for (int i = 0; i < plusWeichen.length; i++) {
            this.plusWeichen[i] = config.weichen[plusWeichen[i]];    
        }
        
        if (pruefungPlus != -1) {
            this.pruefungPlus = config.weichen[pruefungPlus];
        } else {
            this.pruefungPlus = null;
        }
        
        this.minusWeichen = new Weiche[minusWeichen.length];
        for (int i = 0; i < minusWeichen.length; i++) {
            this.minusWeichen[i] = config.weichen[minusWeichen[i]];
        }
        
       if (pruefungMinus != -1) {
            this.pruefungMinus = config.weichen[pruefungMinus];
        } else {
            this.pruefungMinus = null;
        }
        
        this.fahrwegWeichen = new Weiche[fahrwegWeichen.length];
        for (int i = 0; i <fahrwegWeichen.length; i++) {
            this.fahrwegWeichen[i] = config.weichen[fahrwegWeichen[i]];
        }
        
        fahrstrassenAktivierung = new Doppeltaster();
        if (signalTaste >= 0) {
            fahrstrassenAktivierung.init(config, this, gleisTaste, signalTaste, true);
        }
        
        hilfsaufloesung = new Doppeltaster();
        hilfsaufloesung.init(config, this, Const.FHT, aufloesungsTaste);
        
        this.bahnhofsGleis = bahnhofsGleis;
        this.ausfahrtsGleis = ausfahrtsGleis;
        this.signal = config.signale[signalNummer];
        this.ersatzSignalNummer = ersatzSignalNummer;
        this.schluesselweiche1 = schluesselweiche1;
        this.schluesselweiche2 = schluesselweiche2;
        isInbound = signalNummer < SIGNAL_FIRST_OUTBOUND;
        
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
        signal.halt();
        signal.clear();
        unlock();
        if (ausfahrtsGleis != null) {
            ausfahrtsGleis.clear();
        }
        bahnhofsGleis.clear();
        
        strecke.fahrstrasseFHT();
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
        
        if (!isInbound && !strecke.isFree()) {
            config.alert("Der Streckenblock ist noch belegt.");
            return;
        }
        
        if (!isInbound && strecke.isLocked()) {
            config.alert("Der Ausfahrtsperrmelder ist noch aktiv.");
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
        
        if ((pruefungPlus != null) && !pruefungPlus.isPlus()) {
            config.alert("Die Weiche " + pruefungPlus.getName() + " ist nicht in Plus Stellung.");
            return;
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
        
        if ((pruefungMinus != null) && pruefungMinus.isPlus()) {
            config.alert("Die Weiche " + pruefungMinus.getName() + " ist nicht in Minus Stellung.");
            return;
        }
        
        if (schluesselweiche1 != -1) {
            Schluesselweiche sw = config.schluesselweichen[schluesselweiche1];
            if (sw.isLocked()) {
                config.alert("Die Schlüsselweiche " + sw.getName() + " ist nicht verschlossen.");
                return;
            }
        }
        
        if (schluesselweiche2 != -1) {
            Schluesselweiche sw = config.schluesselweichen[schluesselweiche2];
            if (sw.isLocked()) {
                config.alert("Die Schlüsselweiche " + sw.getName() + " ist nicht verschlossen.");
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
        strecke.setFahrstrassenfestlegung();
        
        // Schluesselweichen verrigeln
        if (schluesselweiche1 != -1) {
            Schluesselweiche sw = config.schluesselweichen[schluesselweiche1];
            sw.fsVerriegelt(true);
        }
        if (schluesselweiche2 != -1) {
            Schluesselweiche sw = config.schluesselweichen[schluesselweiche2];
            sw.fsVerriegelt(true);
        }
        
        if (!bahnhofsGleis.isInUse() && this.isInbound) {
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
        
        // Schluesselweichen entrigeln
        if (schluesselweiche1 != -1) {
            Schluesselweiche sw = config.schluesselweichen[schluesselweiche1];
            sw.fsVerriegelt(false);
        }
        if (schluesselweiche2 != -1) {
            Schluesselweiche sw = config.schluesselweichen[schluesselweiche2];
            sw.fsVerriegelt(false);
        }
        
        if (!bahnhofsGleis.isInUse()) {
            bahnhofsGleis.clear();
        }
        
        signal.clear();
        strecke.fahrstrassenauflösung();
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

    /**
     * Prüft ob die Weichen für diesen Fahrweg gestellt sind.
     * Bei Einfahrten mit Ersatzsignal gibt es keine Fahrstraße,
     * es muss also manuell sicher gestellt werden, dass die
     * Weichen korrekt gestellt sind. Diese Funktion macht das
     * Gegenteil dazu: sie prüft, wie die Weichen stehen und
     * entscheidet dann, welcher Fahrweg aktiv wird.
     * 
     * @return 
     */
    private boolean checkFahrweg() {
        switch ( ersatzSignalNummer) {
            case 0:
                switch (bahnhofsGleis.getGleisNummer()) {
                    case 2:
                        return config.weichen[2].isPlus();

                    case 3:
                        return !config.weichen[2].isPlus();

                    default: 
                        return false;
                }
                
            case 1:
                switch (bahnhofsGleis.getGleisNummer()) {
                    case 1:
                        return config.weichen[5].isPlus();

                    case 3:
                        return !config.weichen[5].isPlus() && !config.weichen[3].isPlus();

                    default: 
                        return false;
                }
            
            case 2:
            case 3:
            case 4:
            case 5:
                // Bei den Ausfahrten ist die Zuordnung Gleis zu
                // Ersatzsignal eindeutig, die Weichenstellung
                // muss nicht überprüft werden.
                return true;
        }
        
        return false;
    }
    
    @Override
    public void tick(int count) {
        if (count > nextWhiteTStamp) {
            System.out.println("set white: " + nextWhite.getName() + " at " + count);
            if (ersatzSignalFahrt && (nextWhite instanceof Signal)) {
              nextWhite.clear();
            } else {
                nextWhite.white();
            }
            
            nextWhiteTStamp = Integer.MAX_VALUE;
        }
        
        if (isInbound) {
            if (!ersatzSignalFahrt && (ersatzSignalNummer >= 0) 
                    && config.ersatzsignale[ersatzSignalNummer].isFahrt()
                    && checkFahrweg()) {
                ersatzSignalFahrt = true;
                state = INIT;
            }
        } else {
            if (!ersatzSignalFahrt && (ersatzSignalNummer >= 0) 
                    && config.ersatzsignale[ersatzSignalNummer].isFahrt() 
                    && bahnhofsGleis.isInUse()
                    && checkFahrweg()) {
                ersatzSignalFahrt = true;
                state = INIT;
            }
        }
        
        if (ersatzSignalFahrt) {
            if (isInbound) {
                inboundErsatzTick(count);
            } else {
                outboundErsatzTick(count);
            }
        } else {
            if (isInbound) {
                inboundTick(count);
            } else {
                outboundTick(count);
            }
        }
    }
    
    /**
     * Statemaschine für Ausfahrten mit Ersatzsignal.
     * @param count 
     */
    public void outboundErsatzTick(int count) {
        if (count < nextStep) {
            return;
        }
        
        switch (this.state) {
            case  DORMANT:
                return; // do nothing
                
            case INIT:
                config.alert("Fahrt mit Ersatzsignal gestartet.");
                state = SIGNAL_HP0;
                setRed(count, bahnhofsGleis); // damit setWhite funktioniert
                nextStep = count + STEP_LONG_WAIT;
                break;
                
            case SIGNAL_HP0:
                // Ersatzsignal erlischt nicht automatisch, sondern erst nach 90 Sek
                //config.ersatzsignale[ersatzSignalNummer].hp0();
                state = 0; // Erste Weiche.
                //bahnhofsGleis.clear();
                //lastRed = null;
                nextStep = count + STEP_LONG_WAIT;
                break;

            case AUSFAHRT1:
                setRed(count, ausfahrtsGleis);
                nextStep = count + STEP_LONG_WAIT;
                state = AUSFAHRT2;
                break;
                
            case AUSFAHRT2:
                config.alert("Bahnhof verlassen.");
                fahrwegWeichen[fahrwegWeichen.length - 1].white();
                ausfahrtsGleis.clear();
                nextStep = count + STEP_LONG_WAIT;
                state = OUTGOING_TRAIN;
                break;
                
            case OUTGOING_TRAIN:
                nextStep = count + STEP_LONG_WAIT;
                //strecke.unblock();
                ausfahrtsGleis.clear();
                state = DONE;
                break;
                
            case DONE:
                config.alert("Zugfahrt beendet.");
                bahnhofsGleis.clear();
                lastRed = null;
                strecke.activateGleiskontakt(true);
                ersatzSignalFahrt = false;
                state = DORMANT;
                break;
                
            default:
                if (this.state == 0) {
                    bahnhofsGleis.clear();
                    lastRed = null;
                }
                
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
                strecke.setFahrstrassenfestlegung();
                nextStep = count + STEP_SHORT_WAIT;
                state = SET_HP1; // Fahrstraße wurde ausgewählt.
                break;
                
            case SET_HP1:
                signal.fahrt();
                strecke.setWiederholsperre();
                nextStep = count + STEP_SHORT_WAIT;
                state = WAIT_FOR_TRAIN;
                break;
                
            case WAIT_FOR_TRAIN:
                if (bahnhofsGleis.isInUse()) {
                    config.alert("Fahrt gestartet in Richtung " + strecke.getName());
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
                pendingHalt = true;
                nextStep = count + STEP_SHORT_WAIT;
                state = 0; // erste Weiche
                break;
              
            case AUSFAHRT1:
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
                strecke.activateGleiskontakt(false);
                strecke.fahrstrassenauflösung();
                if (verbundeneEinfahrt >= 0) {
                    // bei Durchfahrten wird die Einfahrt automatisch aufgelöst.
                    if (config.fahrstrassen[verbundeneEinfahrt].isLocked) {
                        config.fahrstrassen[verbundeneEinfahrt].unlock();
                    }
                }
                state = DONE;
                
            case DONE:
                ausfahrtsGleis.clear();
        
                lastRed = null;
                state = DORMANT;
                break;
                
            default:
                checkPendingHalt();
                
                // Weiche 0 bis n
                int weiche = state;
                if (weiche == 0) {
                  // bei der Ausfahrt ist es nicht Teil des Fahrwegs und
                  // wird deshalb sofort dunkel wenn der Zug es verlassen hat.
                  bahnhofsGleis.clear();
                  lastRed = null;
                }
                
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
    
    private void checkPendingHalt() {
        if (pendingHalt) {
            config.alert("Signal auf Halt.");
            signal.halt(true);
            pendingHalt = false;
        }
    }
    
    /**
     * Statemaschine für Einfahrten mit Ersatzsignal.
     * @param count 
     */
    public void inboundErsatzTick(int count) {
        inboundTick(count);
    }
    
    /**
     * Statemaschine für Einfahrten.
     * @param count 
     */
    public void inboundTick(int count) {
        if (count < nextStep) {
            return;
        }
        
        switch (state) {
            case  DORMANT:
                return; // do nothing
                
            case INIT:
                if (!ersatzSignalFahrt) {
                    signal.white();
                    strecke.setFahrstrassenfestlegung();
                }
                
                nextStep = count + STEP_LONG_WAIT;
                state = WAIT_FOR_TRAIN; // Fahrstraße wurde ausgewählt.
                reportWait = true;
                config.alert("Warte auf Zugfahrt " + name);
                break;
                
            case WAIT_FOR_TRAIN:
                boolean vonH = name.startsWith("Von H");
                var trainStarted = (vonH) ? config.pendingTrainH > 0 : config.pendingTrainM > 0;
                
                if (trainStarted) {
                    state = INCOMMING_TRAIN; // Zug wurde gestartet
                    if (vonH) {
                        config.pendingTrainH = 0;
                    } else {
                        config.pendingTrainM = 0;
                    }
                    //config.stoerungsmelder.meldung();
                    config.alert("Fahrt gestartet.");
                    nextStep = count + STEP_LONG_WAIT;
                } else {
                    nextStep = count + STEP_SHORT_WAIT;
                }
                break;
                
            case INCOMMING_TRAIN:
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
                if (ersatzSignalFahrt) {
                    // Signal erlischt nicht automatisch, sondern nach 90 Sekunden
                    //config.ersatzsignale[ersatzSignalNummer].hp0();
                }
                nextStep = count + STEP_SHORT_WAIT;
                state = 0; // erste Weiche
                break;
                
            case EINFAHRT1:
                // Zielgleis erreicht
                config.alert("Zielgleis erreicht.");
                setRed(count, bahnhofsGleis);
                if (!ersatzSignalFahrt) {
                    strecke.trainArrived();
                }
                
                lastRed = null;
                if (ersatzSignalFahrt) {
                    state = WAITERSATZ;
                } else {
                    state = DORMANT;
                }
                break;
            
            case WAITERSATZ:
                if (config.ersatzsignale[this.ersatzSignalNummer].isFahrt()) {
                    nextStep = count + STEP_SHORT_WAIT;
                    break;
                }
                
                config.alert("Ersatzsignalfahrt beendet.");
                ersatzSignalFahrt = false;
                state = DORMANT;
                break;
                
            default:
                // Weiche 0 bis n
                
                int weiche = state;
                
                if (weiche == 0) {
                    // erste Weiche, es gibt keinen Vorgänger, sondern nur
                    // den Streckenabschnitt zum Einfahrtssignal.
                    config.alert("Zug verlässt Signalblock.");
                    strecke.activateGleiskontakt(ersatzSignalFahrt);
                    
                    // signal geht erst beim Befahren der ersten Weiche auf HP0
                    signal.halt();
                }
                
                nextStep = count + STEP_SHORT_WAIT;
                if (weiche < fahrwegWeichen.length) {
                    config.alert("Zug bei Weiche " + fahrwegWeichen[weiche].getName());
                    setRed(count, fahrwegWeichen[weiche]);
                } else {
                    // Zielgleis wird angefahren
                    nextStep = count + STEP_LONG_WAIT;
                    state = EINFAHRT1;
                    return;
                }
                
                state++;
                break;
        }
    }
    
    private void setRed(int actTick, ColorMarker nextRed) {
        if (nextRed != null) {
            if (!nextRed.hasMarker() && !ersatzSignalFahrt) {
                return;
            }

            nextWhite = lastRed;
            if (nextWhite != null) {
                nextWhiteTStamp = actTick + RED_DELTA_TICKS;
                System.out.println("set white timer to " + nextWhiteTStamp);
            }

            lastRed = nextRed;
            nextRed.red();
            System.out.println("set red: " + nextRed.getName());
        }
    }
}
