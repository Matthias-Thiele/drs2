/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Connector;
import de.mmth.drs2.io.Uart;
import de.mmth.drs2.io.UartCommand;
import de.mmth.drs2.parts.Counter;
import de.mmth.drs2.parts.Durchfahrt;
import de.mmth.drs2.parts.Ersatzsignal;
import de.mmth.drs2.parts.Signal;
import de.mmth.drs2.parts.Fahrstrasse;
import de.mmth.drs2.parts.Gleismarker;
import de.mmth.drs2.parts.Rangierfahrt;
import de.mmth.drs2.parts.Schluesselschalter;
import de.mmth.drs2.parts.Schluesselweiche;
import de.mmth.drs2.parts.Stoerungsmelder;
import de.mmth.drs2.parts.Strecke;
import de.mmth.drs2.parts.StreckeAusfahrt;
import de.mmth.drs2.parts.StreckeEinfahrt;
import de.mmth.drs2.parts.Weiche;
import javafx.stage.Stage;

/**
 *
 * @author pi
 */
public class Config implements TickerEvent {
    /**
     * Anzahl der Weichen im DRS 2 Stellpult die
     * über das System verwaltet werden.
     */
    public final static int ANZAHL_WEICHEN = 6;
    
    /**
     * Anzahl der Fahrstrassen die über das
     * System verwaltet werden.
     */
    public final static int ANZAHL_FAHRSTRASSEN = 8;
    
    /**
     * Anzahl der Signale die über das System
     * verwalte werden.
     */
    public final static int ANZAHL_SIGNALE = 6;
    
    /**
     * Anzahl der Start- oder Zielgleise im System.
     */
    public final static int ANZAHL_GLEISE = 5;
    
    /**
     * Anzahl der Ersatzsignale im System.
     */
    public final static int ANZAHL_ERSATZSIGNALE = 6;
    
    /**
     * Anzahl der Schlüsselweichen im System.
     * Aufgrund der begrenzten Portzahl ist nur eine
     * Schlüsselweiche verdrahtet.
     */
    public final static int ANZAHL_SCHLUESSELWEICHEN = 3;
    
    /**
     * Schlüsselschalter für die Fahrstraßenauflösung.
     */
    public final static int ANZAHL_SCHLUESSELSCHALTER = 2;
    
    /**
     * Rangierfahrten
     */
    public final static int ANZAHL_RANGIERFAHRTEN = 4;
    
    /**
     * Anzahl der Ein- und Ausfahrt Strecken.
     */
    public final static int ANZAHL_STRECKEN = 4;

    /**
     * Index der Strecken im Strecken-Array
     */
    public static final int FROM_H = 0;
    public static final int FROM_M = 1;
    public static final int TO_H = 2;
    public static final int TO_M = 3;
    
    
    /**
     * Ticker für die Weiterschaltung und
     * Aktualisierung der verschiedenen
     * Systeme.
     */
    public Ticker ticker = null;
    
    /**
     * Anbindung der DRS 2 Hardware. Das Modul stellt
     * die DRS 2 Ein- und Ausgänge als boolean Array
     * dar und kümmert sich um die Synchronisierung.
     * Mit jedem TickerEvent werden die Eingänge 
     * eingelesen und die Ausgänge aktualisiert.
     */
    public final Connector connector = new Connector();
    
    /**
     * Liste der Weichen auf dem Stellpult.
     */
    public final Weiche[] weichen = new Weiche[ANZAHL_WEICHEN];
    
    /**
     * Liste der Fahrstraßen auf dem Stellpult.
     */
    public final Fahrstrasse[] fahrstrassen = new Fahrstrasse[ANZAHL_FAHRSTRASSEN];
    
    /**
     * Liste der Signale auf dem Stellpult.
     */
    public final Signal[] signale = new Signal[ANZAHL_SIGNALE];
    
    /**
     * Liste der Ersatzsignale auf dem Stellpult.
     */
    public final Ersatzsignal[] ersatzsignale = new Ersatzsignal[ANZAHL_ERSATZSIGNALE];
    
    /**
     * Liste der Start- und Zielgleise auf dem Stellpult.
     */
    public final Gleismarker[] gleise = new Gleismarker[ANZAHL_GLEISE];
    
    /**
     * Liste der Schlüsselweichen auf dem Stellpult.
     */
    public final Schluesselweiche[] schluesselweichen = new Schluesselweiche[ANZAHL_SCHLUESSELWEICHEN];
    
    /**
     * Schlüsselschalter A und F für die Fahrstraßenauflösung.
     */
    public final Schluesselschalter[] schluesselschalter = new Schluesselschalter[ANZAHL_SCHLUESSELSCHALTER];

    public final Rangierfahrt[] rangierfahrten = new Rangierfahrt[ANZAHL_RANGIERFAHRTEN];
    
    /**
     * Streckenblöcke in Richtung H und M für Ein- und Ausfahrt.
     */
    public final Strecke[] strecken = new Strecke[ANZAHL_STRECKEN];
    
    /**
     * Verbindung zum externen Streckenblock-Adapter und DRS2
     */
    public Uart uart1, uart2;
    
    /**
     * JavaFX Anzeige des Systemzustands.
     */
    public MainPane mainPane;
    public Stage stage;
    
    /**
     * Zähler für die Betätigung eines Ersatzsignals.
     */
    public Counter ersatzsignalCounter = new Counter();
    
    /**
     * Anzeige und Weckerunterbrechungstaste für Weichen-
     * und Signalstörungen.
     */
    public Stoerungsmelder stoerungsmelder = new Stoerungsmelder();
    
    /**
     * Zustand des Schlüsselschalters auf der DRS2
     */
    public boolean tastenAnschalter = false;
    
    /**
     * Zugstart aus M oder H manuell ausgelöst.
     * Falls keine Fahrstraße eingerichtet wird, wird
     * diese Information nach einer gewissen Zeit wieder
     * gelöscht.
     */
    public int pendingTrainH = 0;
    public int pendingTrainM = 0;
    
    /**
     * Initialisiert die Systemkonfiguration
     */
    public void init() {
        ticker = new Ticker(this);
        initWeichen();
        initSignale();
        initErsatzsignale();
        initGleise();
        initStrecken();
        initFahrstrassen();
        initCounter();
        initSchluesselweichen();
        initSchluesselschalter();
        initRangierfahrten();
        
        var df = new Durchfahrt();
        df.init(this);
        
        stoerungsmelder.init(this, Const.WuT_S, Const.WuT_W, 68, 67, 66, 65, Const.Wecker);
        //Uart.createUarts(this, "/dev/ttyAMA2", "/dev/ttyAMA1", connector.polarity);        
        Uart.createUarts(this, "/dev/ttyUSB0", "/dev/ttyUSB1", connector.polarity);        
        ticker.add(this);
    }
    
    /**
     * Initialisiert die Zähler Ansteuerung.
     */
    private void initCounter() {
        ersatzsignalCounter.init(this, "Ersatzsignal", 63);
    }
    
    /**
     * Initialisiert die vier Streckenblöcke.
     */
    private void initStrecken() {
        Strecke fromH = new StreckeEinfahrt();
        fromH.init(this, "Von H", Const.BlockHIn, Const.StreckeVonHWeiss, 
                Const.StreckeVonHRot, Const.EinfRaeumungsmelderH, Const.VbHT_H, 
                Const.EinfFestlegemelderH, -1, 1);
        strecken[FROM_H] = fromH;
        fromH.setSimulationMode(true);
        
        Strecke fromM = new StreckeEinfahrt();
        fromM.init(this, "Von M", Const.BlockMIn, Const.StreckeVonMWeiss, 
                Const.StreckeVonMRot, Const.EinfRaeumungsmelderM, Const.VbHT_M, 
                Const.EinfFestlegemelderM, Const.StreckeVonAH, 0);
        strecken[FROM_M] = fromM;
        
        Strecke toH = new StreckeAusfahrt();
        toH.init(this, "Nach H", Const.BlockHOut, Const.StreckeNachHWeiss,
                Const.StreckeNachHRot, Const.AusfSperrmelderH, Const.VbHT_H,
                Const.AusfFestlegemelderH, -1, -1);
        strecken[TO_H] = toH;
        toH.setSimulationMode(true);
        
        Strecke toM = new StreckeAusfahrt();
        toM.init(this, "Nach M", Const.BlockMOut, Const.StreckeNachMWeiss,
                Const.StreckeNachMRot, Const.AusfSperrmelderM, Const.VbHT_M,
                Const.AusfFestlegemelderM, Const.StreckeNachAH, -1);
        strecken[TO_M] = toM;
    }
    
    /**
     * Initialisiert die Schlüsselschalter für die Fahrstraßenauflösung.
     */
    private void initSchluesselschalter() {
        schluesselschalter[0] = new Schluesselschalter(this, Const.SCHLUESSEL_A, 0, 1);
        schluesselschalter[1] = new Schluesselschalter(this, Const.SCHLUESSEL_F, 2, 3);
    }
    
    /**
     * Initialisiert die verfügbaren Rangierfahrten.
     */
    private void initRangierfahrten() {
        rangierfahrten[0] = new Rangierfahrt();
        rangierfahrten[0].init(this, "Von Gleis 1 nach Gleis 3 über Alth", "19cnAXZ9WIAqBMC7.");
        rangierfahrten[1] = new Rangierfahrt();
        rangierfahrten[1].init(this, "Von Gleis 2 nach Gleis 1 über Wild", "29etDQEZyFY9WRF5.");
        rangierfahrten[2] = new Rangierfahrt();
        rangierfahrten[2].init(this, "Von Gleis 3 nach Gleis 1 über Alth", "39dsCKBZoAX9WHA5.");
        rangierfahrten[3] = new Rangierfahrt();
        rangierfahrten[3].init(this, "Von Gleis 1 nach Gleis 5 über Alth", "19cnAXZ9WIAqBMC79k.");
    }
    
    /**
     * Initialisiert die Schlüsselweichen im System.
     */
    private void initSchluesselweichen() {
        for (int i = 0; i < ANZAHL_SCHLUESSELWEICHEN; i++) {
            Schluesselweiche weiche = new Schluesselweiche();
            switch (i) {
                case 0:
                    weiche.init(this, "SlFT III", Const.SlFT3, Const.SlFT3Rot, Const.SlFT3Weiss, Const.SlFT3Relais, Const.WSCHLUESSEL3);
                    break;
                    
                case 1:
                    weiche.init(this, "SlFT I", Const.SlFT1,  Const.SlFT1Rot, Const.SlFT1Weiss, Const.SlFT1Relais, Const.WSCHLUESSEL1);
                    break;
                    
                case 2:
                    weiche.init(this, "SlFT IV", Const.SlFT4, Const.SlFT4Rot, Const.SlFT4Weiss,  Const.SlFT4Relais, Const.WSCHLUESSEL4);
                    break;
                    
                default:
                    // keine weiteren Schlüsselweichen
            }
            
            schluesselweichen[i] = weiche;
        }
    }
    
    /**
     * Stellt die Portnummern für die 6 Weichen ein.
     */
    private void initWeichen() {
        for (int i = 0; i < ANZAHL_WEICHEN; i++) {
            var weiche = new Weiche();
            String name;
            int taste1 = Const.WGT;
            int taste2, whitePlus, whiteMinus, redPlus, redMinus;
            
            switch (i) {
                case 0:
                    name = "W3";
                    taste2 = Const.WEICHE3;
                    whitePlus = 7;
                    whiteMinus = 6;
                    redPlus = 5;
                    redMinus = 4;
                    break;
                
                case 1:
                    name = "W4";
                    taste2 = Const.WEICHE4;
                    whitePlus = 3;
                    whiteMinus = 2;
                    redPlus = 1;
                    redMinus = 0;
                    break;
                
                case 2:
                    name = "W5";
                    taste2 = Const.WEICHE5;
                    whitePlus = 15;
                    whiteMinus = 14;
                    redPlus = 13;
                    redMinus = 12;
                    break;
                
                case 3:
                    name = "W18";
                    taste2 = Const.WEICHE18;
                    whitePlus = 11;
                    whiteMinus = 10;
                    redPlus = 9;
                    redMinus = 8;
                    break;
                
                case 4:
                    name = "W19";
                    taste2 = Const.WEICHE19;
                    whitePlus = 22;
                    whiteMinus = 23;
                    redPlus = 20;
                    redMinus = 21;
                    break;
                
                case 5:
                    name = "W20";
                    taste2 = Const.WEICHE20;
                    whitePlus = 19;
                    whiteMinus = 18;
                    redPlus = 17;
                    redMinus = 16;
                    break;
                    
                default:
                    name = "TBD";
                    taste2 = whitePlus = whiteMinus = redPlus = redMinus = -1;
            }
            
            weiche.init(this, name, taste1, taste2, whitePlus, whiteMinus, redPlus, redMinus);
            weichen[i] = weiche;
        }
    }
    
    private void initGleise() {
        for (int i = 0; i < ANZAHL_GLEISE; i++) {
            gleise[i] = new Gleismarker();
        }
        
        gleise[0].init(this, "Gleis 1", 30, 31, 1);
        gleise[1].init(this, "Gleis 2", 28, 29, 2);
        gleise[2].init(this, "Gleis 3", 26, 27, 3);
        gleise[3].init(this, "Strecke A", 41, 40, 4);
        gleise[4].init(this, "Strecke F", 55, 54, 5);
    }
    
    /**
     * Stellt die Weichen und Portnummern für die 8 Fahrstrassen ein.
     */
    private void initFahrstrassen() {
        for (int i = 0; i < ANZAHL_FAHRSTRASSEN; i++) {
            var fahrstrasse = new Fahrstrasse();
            String name;
            int taste1, taste2, gleis, signalNummer, ausfahrt = -1;
            int[] minusWeichen, plusWeichen, fahrwegWeichen = {};
            int streckenTaster = -1;
            int ersatz = -1;
            int schluesselweiche1 = -1, schluesselweiche2 = -1;
            int pruefungPlus = -1, pruefungMinus = -1;
            int aufloesungsTaste = -1;
            Strecke strecke;
            
            switch(i) {
                case 0:
                    name = "Von M nach Gleis 2";
                    taste1 = Const.SIGNAL_A;
                    taste2 = Const.GLEIS2;
                    aufloesungsTaste = Const.SIGNAL_A;
                    gleis = 1;
                    int[] minusWeichen0 = {};
                    minusWeichen = minusWeichen0;
                    int[] plusWeichen0 = {0, 1, 2};
                    plusWeichen = plusWeichen0;
                    int[] fahrwegWeichen0 = {1, 2};
                    fahrwegWeichen = fahrwegWeichen0;
                    signalNummer = 0;
                    streckenTaster = 17;
                    ersatz = 0;
                    strecke = strecken[FROM_M];
                    pruefungPlus = 3;
                    break;
                    
                case 1:
                    name = "Von M nach Gleis 3";
                    taste1 = Const.SIGNAL_A;
                    taste2 = Const.GLEIS3;
                    aufloesungsTaste = Const.SIGNAL_A;
                    gleis = 2;
                    int[] minusWeichen1 = {2};
                    minusWeichen = minusWeichen1;
                    int[] plusWeichen1 = {0, 1};
                    plusWeichen = plusWeichen1;
                    int[] fahrwegWeichen1 = {1, 2};
                    fahrwegWeichen = fahrwegWeichen1;
                    signalNummer = 0;
                    streckenTaster = 17;
                    ersatz = 0;
                    strecke = strecken[FROM_M];
                    schluesselweiche1 = 0;
                    pruefungMinus = 3;
                    break;
                    
                case 2:
                    name = "Von H nach Gleis 1";
                    taste1 = Const.SIGNAL_F;
                    taste2 = Const.GLEIS1;
                    aufloesungsTaste = Const.SIGNAL_F;
                    gleis = 0;
                    int[] minusWeichen2 = {};
                    minusWeichen = minusWeichen2;
                    int[] plusWeichen2 = {4, 5, 1};
                    plusWeichen = plusWeichen2;
                    int[] fahrwegWeichen2 = {5};
                    fahrwegWeichen = fahrwegWeichen2;
                    signalNummer = 1;
                    streckenTaster = 18;
                    ersatz = 1;
                    strecke = strecken[FROM_H];
                    schluesselweiche1 = 2;
                    pruefungPlus = 0;
                    break;
                    
                case 3:
                    name = "Von H nach Gleis 3";
                    taste1 = Const.SIGNAL_F;
                    taste2 = Const.GLEIS3;
                    aufloesungsTaste = Const.SIGNAL_F;
                    gleis = 2;
                    int[] minusWeichen3 = {3, 4, 5};
                    minusWeichen = minusWeichen3;
                    int[] plusWeichen3 = {};
                    plusWeichen = plusWeichen3;
                    int[] fahrwegWeichen3 = {5, 4, 3};
                    fahrwegWeichen = fahrwegWeichen3;
                    signalNummer = 1;
                    streckenTaster = 18;
                    ersatz = 1;
                    strecke = strecken[FROM_H];
                    schluesselweiche1 = 0;
                    pruefungMinus = 2;
                    break;
                    
                case 4:
                    name = "Von Gleis 1 nach M";
                    taste1 = Const.SIGNAL_P1;
                    taste2 = Const.BlockMOut;
                    aufloesungsTaste = Const.BlockMOut;
                    gleis = 0;
                    ausfahrt = 3;
                    int[] minusWeichen4 = {};
                    minusWeichen = minusWeichen4;
                    int[] plusWeichen4 = {0, 1};
                    plusWeichen = plusWeichen4;
                    int[] fahrwegWeichen4 = {0};
                    fahrwegWeichen = fahrwegWeichen4;
                    signalNummer = 2;
                    strecke = strecken[TO_M];
                    ersatz = 4;
                    schluesselweiche1 = 1;
                    schluesselweiche2 = 2;
                    break;
                    
                case 5:
                    name = "Von Gleis 3 nach M";
                    taste1 = Const.SIGNAL_P3;
                    taste2 = Const.BlockMOut;
                    aufloesungsTaste = Const.BlockMOut;
                    gleis = 2;
                    ausfahrt = 3;
                    int[] minusWeichen5 = {0, 1, 2};
                    minusWeichen = minusWeichen5;
                    int[] plusWeichen5 = {};
                    plusWeichen = plusWeichen5;
                    int[] fahrwegWeichen5 = {2, 1, 0};
                    fahrwegWeichen = fahrwegWeichen5;
                    signalNummer = 3;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
                    strecke = strecken[TO_M];
                    ersatz = 5;
                    schluesselweiche1 = 0;
                    schluesselweiche2 = 1;
                    break;
                    
                case 6:
                    name = "Von Gleis 2 nach H";
                    taste1 = Const.SIGNAL_N2;
                    taste2 = Const.BlockHOut;
                    aufloesungsTaste = Const.BlockHOut;
                    gleis = 1;
                    ausfahrt = 4;
                    int[] minusWeichen6 = {};
                    minusWeichen = minusWeichen6;
                    int[] plusWeichen6 = {3, 4, 5};
                    plusWeichen = plusWeichen6;
                    int[] fahrwegWeichen6 = {3, 4};
                    fahrwegWeichen = fahrwegWeichen6;
                    signalNummer = 4;
                    strecke = strecken[TO_H];
                    ersatz = 2;
                    break;
                    
                case 7:
                    name = "Von Gleis 3 nach H";
                    taste1 = Const.SIGNAL_N3;
                    taste2 = Const.BlockHOut;
                    aufloesungsTaste = Const.BlockHOut;
                    gleis = 2;
                    ausfahrt = 4;
                    int[] minusWeichen7 = {3};
                    minusWeichen = minusWeichen7;
                    int[] plusWeichen7 = {4};
                    plusWeichen = plusWeichen7;
                    int[] fahrwegWeichen7 = {3, 4};
                    fahrwegWeichen = fahrwegWeichen7;
                    signalNummer = 5;
                    strecke = strecken[TO_H];
                    ersatz = 3;
                    schluesselweiche1 = 0;
                    break;
                    
                default:
                    name = "TBD";
                    taste1 = taste2 = gleis = 2; signalNummer = 0;
                    plusWeichen = new int[0];
                    minusWeichen = new int[0];
                    strecke = strecken[0];
            }
            
            Gleismarker ausfahrtsGleis = (ausfahrt == -1) ? null : gleise[ausfahrt];
            fahrstrasse.init(this, name, plusWeichen, minusWeichen, fahrwegWeichen, 
                    taste1, taste2, gleise[gleis], signalNummer, ersatz, ausfahrtsGleis,
                    strecke, streckenTaster, schluesselweiche1, schluesselweiche2,
                    pruefungPlus, pruefungMinus, aufloesungsTaste);
            
            fahrstrassen[i] = fahrstrasse;
        }
        
        // Durchfahrt von M nach H auf Gleis 2
        fahrstrassen[6].addEinfahrt(0);
        // Durchfahrt von H nach M auf Gleis 1
        fahrstrassen[4].addEinfahrt(2);
    }
    
    /**
     * Erzeugt und initialisiert die Liste der Ersatzsignale.
     */
    private void initErsatzsignale() {
        for (int i = 0; i < ANZAHL_ERSATZSIGNALE; i++) {
            ersatzsignale[i] = new Ersatzsignal();
        }
        
        for (int i = 0; i < ANZAHL_ERSATZSIGNALE; i++) {
            Ersatzsignal signal = ersatzsignale[i];
            String name = "";
            int taste = -1;
            int lampe = -1;
            int interlock1 = -1;
            int interlock2 = -1;
            
            switch(i) {
                case 0:
                    name = "Ers A";
                    taste = 10;
                    lampe = 53;
                    interlock1 = 4;
                    interlock2 = 5;
                    break;
                    
                case 1:
                    name = "Ers F";
                    taste = 11;
                    lampe = 50;
                    interlock1 = 2;
                    interlock2 = 3;
                    break;
                    
                case 2:
                    name = "Ers N2";
                    taste = 14;
                    lampe = 52;
                    interlock1 = 1;
                    interlock2 = 3;
                    break;
                    
                case 3:
                    name = "Ers N3";
                    taste = 15;
                    lampe = 51;
                    interlock1 = 1;
                    interlock2 = 2;
                    break;
                    
                case 4:
                    name = "Ers P1";
                    taste = 12;
                    lampe = 49;
                    interlock1 = 0;
                    interlock2 = 5;
                    break;
                    
                case 5:
                    name = "Ers P3";
                    taste = 13;
                    lampe = 48;
                    interlock1 = 0;
                    interlock2 = 4;
                    break;
            }
            
            signal.init(this, name, taste, lampe,
                    ersatzsignale[interlock1], ersatzsignale[interlock2]);
        }
    }
    
    /**
     * Stellt die Portnummern für die Signale ein.
     */
    private void initSignale() {
        for (int i = 0; i < ANZAHL_SIGNALE; i++) {
            Signal signal = new Signal();
            String name;
            int sigTaste, sigFahrt, sigHalt, vorsigFahrt, vorsigHalt;
            int sh1Lampe = -1, sh1WPlus = -1, sh1WMinus = -1;
            int fahrwegWhite = -1, fahrwegRed = -1, einfahrtSignal = -1;
            int fahrstrasse1 = -1, fahrstrasse2 = -1, fahrstrasse3 = -1, fahrstrasse4 = -1;
            switch (i) {
                case 0:
                    name = "Sig A";
                    sigTaste = 10;
                    sigFahrt = 24;
                    sigHalt = 25;
                    vorsigFahrt = 75;
                    vorsigHalt = 76;
                    fahrwegWhite = 45;
                    fahrwegRed = 44;
                    break;
                    
                case 1:
                    name = "Sig F";
                    sigTaste = 11;
                    sigFahrt = 36;
                    sigHalt = 39;
                    vorsigFahrt = 73;
                    vorsigHalt = 74;
                    fahrwegWhite = 43;
                    fahrwegRed = 42;
                    break;
                    
                case 2:
                    name = "Sig P1";
                    sigTaste = 12;
                    sigFahrt = 38;
                    sigHalt = 37;
                    vorsigFahrt = 86;
                    vorsigHalt = 85;
                    sh1Lampe = 64;
                    sh1WPlus = 0; // Weiche 3
                    einfahrtSignal = 1;
                    fahrstrasse1 = 4;
                    fahrstrasse2 = 2;
                    break;
                    
                case 3:
                    name = "Sig P3";
                    sigTaste = 13;
                    sigFahrt = 34;
                    sigHalt = 35;
                    vorsigFahrt = 87;
                    vorsigHalt = 35;
                    einfahrtSignal = 1;
                    sh1Lampe = 79;
                    sh1WMinus = 2; // Weiche 5
                    fahrstrasse1 = 5;
                    fahrstrasse2 = 3;
                    fahrstrasse3 = 7;
                    fahrstrasse4 = 1;
                    break;
                    
                case 4:
                    name = "Sig N2";
                    sigTaste = 14;
                    sigFahrt = 32;
                    sigHalt = 33;
                    vorsigFahrt = 32;
                    vorsigHalt = 33;
                    einfahrtSignal = 0;
                    sh1Lampe = 78;
                    sh1WPlus = 3;
                    fahrstrasse1 = 6;
                    fahrstrasse2 = 0;
                    break;
                    
                case 5:
                    name = "Sig N3";
                    sigTaste = 15;
                    sigFahrt = 46;
                    sigHalt = 47;
                    vorsigFahrt = 46;
                    vorsigHalt = 47;
                    einfahrtSignal = 0;
                    sh1Lampe = 77;
                    sh1WMinus = 3;
                    fahrstrasse1 = 7;
                    fahrstrasse2 = 1;
                    fahrstrasse3 = 5;
                    fahrstrasse4 = 3;
                    break;
                    
                default:
                    name = "TBD";
                    sigTaste = sigFahrt = sigHalt = vorsigFahrt = vorsigHalt = -1;
                    break;
                    
            }
            
            signal.init(this, name, sigTaste, sigFahrt, sigHalt, vorsigFahrt, vorsigHalt, 
                    fahrwegWhite, fahrwegRed, 
                    sh1Lampe, sh1WPlus, sh1WMinus, einfahrtSignal, 
                    fahrstrasse1, fahrstrasse2,fahrstrasse3, fahrstrasse4);
            
            signale[i] = signal;
        }
    }
    
    /**
     * Gibt eine Benachrichtigung aus
     * @param message 
     */
    public void alert(String message) {
        System.out.println(message);
        mainPane.addMessage(message);
    }

    @Override
    public void tick(int count) {
        if (tastenAnschalter != this.connector.isInSet(Const.TA)) {
            tastenAnschalter = this.connector.isInSet(Const.TA);
            alert("Tastenfeld " + (tastenAnschalter ? "eingeschaltet." : "abgeschaltet."));
        }
        
        // angemeldete Zugfahrten bleiben nicht stehen wenn sie nicht genutzt werden.
        if (pendingTrainH > 0) {
            pendingTrainH--;
        }
        
        if (pendingTrainM > 0) {
            pendingTrainM--;
        }
    }
}
