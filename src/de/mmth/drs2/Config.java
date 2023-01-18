/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Connector;
import de.mmth.drs2.parts.Counter;
import de.mmth.drs2.parts.Ersatzsignal;
import de.mmth.drs2.parts.Signal;
import de.mmth.drs2.parts.Fahrstrasse;
import de.mmth.drs2.parts.Gleismarker;
import de.mmth.drs2.parts.Schluesselweiche;
import de.mmth.drs2.parts.Stoerungsmelder;
import de.mmth.drs2.parts.Weiche;

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
    public final static int ANZAHL_SCHLUESSELWEICHEN = 1;
    
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
     * JavaFX Anzeige des Systemzustands.
     */
    public MainPane mainPane;
    
    /**
     * Zähler für die Betätigung eines Ersatzsignals.
     */
    public Counter ersatzsignalCounter = new Counter();
    
    /**
     * Anzeige und Weckerunterbrechungstaste für Weichen-
     * und Signalstörungen.
     */
    public Stoerungsmelder stoerungsmelder = new Stoerungsmelder();
    
    public boolean tastenAnschalter = false;
    
    /**
     * Initialisiert die Systemkonfiguration
     */
    public void init() {
        ticker = new Ticker(this);
        initWeichen();
        initSignale();
        initErsatzsignale();
        initGleise();
        initFahrstrassen();
        initCounter();
        initSchluesselweichen();
        
        stoerungsmelder.init(this, Const.WuT_S, Const.WuT_W, 68, 67, 66, 65);
        
        ticker.add(this);
    }
    
    /**
     * Initialisiert die Zähler Ansteuerung
     */
    private void initCounter() {
        ersatzsignalCounter.init(this, "Ersatzsignal", 63);
    }
    
    /**
     * Initialisiert die Schlüsselweichen im System.
     */
    private void initSchluesselweichen() {
        for (int i = 0; i < ANZAHL_SCHLUESSELWEICHEN; i++) {
            Schluesselweiche weiche = new Schluesselweiche();
            switch (i) {
                case 0:
                    weiche.init(this, Const.SlFT, Const.SlFLT, 70, 69);
                    break;
                    
                default:
                    // keine weiteren Schlüsselweichen
            }
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
                    taste2 = 1;
                    whitePlus = 7;
                    whiteMinus = 6;
                    redPlus = 5;
                    redMinus = 4;
                    break;
                
                case 1:
                    name = "W4";
                    taste2 = 2;
                    whitePlus = 3;
                    whiteMinus = 2;
                    redPlus = 1;
                    redMinus = 0;
                    break;
                
                case 2:
                    name = "W5";
                    taste2 = 3;
                    whitePlus = 15;
                    whiteMinus = 14;
                    redPlus = 13;
                    redMinus = 12;
                    break;
                
                case 3:
                    name = "W18";
                    taste2 = 4;
                    whitePlus = 11;
                    whiteMinus = 10;
                    redPlus = 9;
                    redMinus = 8;
                    break;
                
                case 4:
                    name = "W19";
                    taste2 = 5;
                    whitePlus = 22;
                    whiteMinus = 23;
                    redPlus = 20;
                    redMinus = 21;
                    break;
                
                case 5:
                    name = "W20";
                    taste2 = 6;
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
        
        gleise[0].init(this, 30, 31);
        gleise[1].init(this, 28, 29);
        gleise[2].init(this, 26, 27);
        gleise[3].init(this, 41, 40);
        gleise[4].init(this, 55, 54);
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
            int streckeWeiss = -1, streckeRot = -1, streckenTaster = -1;
            int ersatz = -1, sperrRaeumungsmelder = -1, festlegemelder = -1;
            String streckeName = "?";
            
            switch(i) {
                case 0:
                    name = "Von M auf G2";
                    taste1 = 10;
                    taste2 = 8;
                    gleis = 1;
                    int[] minusWeichen0 = {};
                    minusWeichen = minusWeichen0;
                    int[] plusWeichen0 = {0, 1, 2, 3, 4, 5};
                    plusWeichen = plusWeichen0;
                    int[] fahrwegWeichen0 = {1, 2};
                    fahrwegWeichen = fahrwegWeichen0;
                    signalNummer = 0;
                    streckeWeiss = 60;
                    streckeRot = 59;
                    streckenTaster = 17;
                    ersatz = 0;
                    streckeName = "M";
                    sperrRaeumungsmelder = 82;
                    festlegemelder = 80;
                    break;
                    
                case 1:
                    name = "Von M auf G3";
                    taste1 = 10;
                    taste2 = 9;
                    gleis = 2;
                    int[] minusWeichen1 = {2, 3};
                    minusWeichen = minusWeichen1;
                    int[] plusWeichen1 = {0, 1};
                    plusWeichen = plusWeichen1;
                    int[] fahrwegWeichen1 = {1, 2};
                    fahrwegWeichen = fahrwegWeichen1;
                    signalNummer = 0;
                    streckeWeiss = 60;
                    streckeRot = 59;
                    streckenTaster = 17;
                    ersatz = 0;
                    streckeName = "M";
                    sperrRaeumungsmelder = 82;
                    festlegemelder = 80;
                    break;
                    
                case 2:
                    name = "Von H auf G1";
                    taste1 = 11;
                    taste2 = 7;
                    gleis = 0;
                    int[] minusWeichen2 = {};
                    minusWeichen = minusWeichen2;
                    int[] plusWeichen2 = {0, 1, 4, 5};
                    plusWeichen = plusWeichen2;
                    int[] fahrwegWeichen2 = {5};
                    fahrwegWeichen = fahrwegWeichen2;
                    signalNummer = 1;
                    streckeWeiss = 62;
                    streckeRot = 61;
                    streckenTaster = 18;
                    ersatz = 1;
                    streckeName = "H";
                    sperrRaeumungsmelder = 81;
                    festlegemelder = 95;
                    break;
                    
                case 3:
                    name = "Von H auf G3";
                    taste1 = 11;
                    taste2 = 9;
                    gleis = 2;
                    int[] minusWeichen3 = {3, 4, 5};
                    minusWeichen = minusWeichen3;
                    int[] plusWeichen3 = {2};
                    plusWeichen = plusWeichen3;
                    int[] fahrwegWeichen3 = {5, 4, 3};
                    fahrwegWeichen = fahrwegWeichen3;
                    signalNummer = 1;
                    streckeWeiss = 62;
                    streckeRot = 61;
                    streckenTaster = 18;
                    ersatz = 1;
                    streckeName = "H";
                    sperrRaeumungsmelder = 81;
                    festlegemelder = 95;
                    break;
                    
                case 4:
                    name = "Von Gleis 1 nach M";
                    taste1 = 12;
                    taste2 = 7;
                    gleis = 0;
                    ausfahrt = 3;
                    int[] minusWeichen4 = {};
                    minusWeichen = minusWeichen4;
                    int[] plusWeichen4 = {0, 1};
                    plusWeichen = plusWeichen4;
                    int[] fahrwegWeichen4 = {0};
                    fahrwegWeichen = fahrwegWeichen4;
                    signalNummer = 2;
                    streckeWeiss = 56;
                    streckeRot = 71;
                    ersatz = 4;
                    sperrRaeumungsmelder = 84;
                    festlegemelder = 93;
                    break;
                    
                case 5:
                    name = "Von Gleis 3 nach M";
                    taste1 = 13;
                    taste2 = 9;
                    gleis = 2;
                    ausfahrt = 3;
                    int[] minusWeichen5 = {0, 1, 2};
                    minusWeichen = minusWeichen5;
                    int[] plusWeichen5 = {};
                    plusWeichen = plusWeichen5;
                    int[] fahrwegWeichen5 = {2, 1, 0};
                    fahrwegWeichen = fahrwegWeichen5;
                    signalNummer = 3;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  
                    streckeWeiss = 56;
                    streckeRot = 71;
                    ersatz = 5;
                    sperrRaeumungsmelder = 84;
                    festlegemelder = 93;
                    break;
                    
                case 6:
                    name = "Von Gleis 2 nach H";
                    taste1 = 14;
                    taste2 = 8;
                    gleis = 1;
                    ausfahrt = 4;
                    int[] minusWeichen6 = {};
                    minusWeichen = minusWeichen6;
                    int[] plusWeichen6 = {3, 4, 5};
                    plusWeichen = plusWeichen6;
                    int[] fahrwegWeichen6 = {3, 4};
                    fahrwegWeichen = fahrwegWeichen6;
                    signalNummer = 4;
                    streckeWeiss = 58;
                    streckeRot = 57;
                    ersatz = 2;
                    sperrRaeumungsmelder = 83;
                    festlegemelder = 94;
                    break;
                    
                case 7:
                    name = "Von Gleis 3 nach H";
                    taste1 = 15;
                    taste2 = 9;
                    gleis = 2;
                    ausfahrt = 4;
                    int[] minusWeichen7 = {3};
                    minusWeichen = minusWeichen7;
                    int[] plusWeichen7 = {4};
                    plusWeichen = plusWeichen7;
                    int[] fahrwegWeichen7 = {3, 4};
                    fahrwegWeichen = fahrwegWeichen7;
                    signalNummer = 5;
                    streckeWeiss = 58;
                    streckeRot = 57;
                    ersatz = 3;
                    sperrRaeumungsmelder = 83;
                    festlegemelder = 94;
                    break;
                    
                default:
                    name = "TBD";
                    taste1 = taste2 = gleis = 2; signalNummer = 0;
                    plusWeichen = new int[0];
                    minusWeichen = new int[0];
            }
            
            Gleismarker ausfahrtsGleis = (ausfahrt == -1) ? null : gleise[ausfahrt];
            fahrstrasse.init(this, name, plusWeichen, minusWeichen, fahrwegWeichen, 
                    taste1, taste2, gleise[gleis], signalNummer, ersatz, ausfahrtsGleis,
                    streckeName, streckeWeiss, streckeRot, streckenTaster,
                    festlegemelder, sperrRaeumungsmelder);
            
            fahrstrassen[i] = fahrstrasse;
        }
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
            
            signal.init(this, name, Const.ErsGT, taste, lampe,
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
            int fahrwegWhite = -1, fahrwegRed = -1;
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
                    break;
                    
                case 3:
                    name = "Sig P3";
                    sigTaste = 13;
                    sigFahrt = 34;
                    sigHalt = 35;
                    vorsigFahrt = 87;
                    vorsigHalt = 35;
                    sh1Lampe = 79;
                    sh1WMinus = 0; // Weiche 3
                    break;
                    
                case 4:
                    name = "Sig N2";
                    sigTaste = 14;
                    sigFahrt = 32;
                    sigHalt = 33;
                    vorsigFahrt = 72;
                    vorsigHalt = 33;
                    sh1Lampe = 78;
                    sh1WPlus = 3;
                    break;
                    
                case 5:
                    name = "Sig N3";
                    sigTaste = 15;
                    sigFahrt = 46;
                    sigHalt = 47;
                    vorsigFahrt = 72;
                    vorsigHalt = 47;
                    sh1Lampe = 77;
                    sh1WMinus = 3;
                    break;
                    
                default:
                    name = "TBD";
                    sigTaste = sigFahrt = sigHalt = vorsigFahrt = vorsigHalt = -1;
                    break;
                    
            }
            
            signal.init(this, name, sigTaste, sigFahrt, sigHalt, vorsigFahrt, vorsigHalt, 
                    fahrwegWhite, fahrwegRed, 
                    sh1Lampe, sh1WPlus, sh1WMinus);
            
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
    }
}
