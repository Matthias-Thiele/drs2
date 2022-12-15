/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Connector;
import de.mmth.drs2.parts.Signal;
import de.mmth.drs2.parts.Fahrstrasse;
import de.mmth.drs2.parts.Weiche;

/**
 *
 * @author pi
 */
public class Config {
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
     * Ticker für die Weiterschaltung und
     * Aktualisierung der verschiedenen
     * Systeme.
     */
    public final Ticker ticker = new Ticker();
    
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
     * JavaFX Anzeige des Systemzustands.
     */
    public MainPane mainPane;
    
    
    /**
     * Initialisiert die Systemkonfiguration
     */
    public void init() {
        initWeichen();
        initSignale();
        initFahrstrassen();
    }
    
    /**
     * Stellt die Portnummern für die 6 Weichen ein.
     */
    private void initWeichen() {
        for (int i = 0; i < ANZAHL_WEICHEN; i++) {
            var weiche = new Weiche();
            String name;
            int taste1, taste2, whitePlus, whiteMinus, redPlus, redMinus;
            
            switch (i) {
                case 0:
                    name = "W3";
                    taste1 = 0;
                    taste2 = 1;
                    whitePlus = 7;
                    whiteMinus = 6;
                    redPlus = 5;
                    redMinus = 4;
                    break;
                
                case 1:
                    name = "W4";
                    taste1 = 0;
                    taste2 = 2;
                    whitePlus = 3;
                    whiteMinus = 2;
                    redPlus = 1;
                    redMinus = 0;
                    break;
                
                case 2:
                    name = "W5";
                    taste1 = 0;
                    taste2 = 3;
                    whitePlus = 15;
                    whiteMinus = 14;
                    redPlus = 13;
                    redMinus = 12;
                    break;
                
                case 3:
                    name = "W18";
                    taste1 = 0;
                    taste2 = 4;
                    whitePlus = 11;
                    whiteMinus = 10;
                    redPlus = 9;
                    redMinus = 8;
                    break;
                
                case 4:
                    name = "W19";
                    taste1 = 0;
                    taste2 = 5;
                    whitePlus = 22;
                    whiteMinus = 23;
                    redPlus = 20;
                    redMinus = 21;
                    break;
                
                case 5:
                    name = "W20";
                    taste1 = 0;
                    taste2 = 6;
                    whitePlus = 19;
                    whiteMinus = 18;
                    redPlus = 17;
                    redMinus = 16;
                    break;
                    
                default:
                    name = "TBD";
                    taste1 = taste2 = whitePlus = whiteMinus = redPlus = redMinus = -1;
            }
            
            weiche.init(this, name, taste1, taste2, whitePlus, whiteMinus, redPlus, redMinus);
            weichen[i] = weiche;
        }
    }
    
    /**
     * Stellt die Weichen und Portnummern für die 8 Fahrstrassen ein.
     */
    private void initFahrstrassen() {
        for (int i = 0; i < ANZAHL_FAHRSTRASSEN; i++) {
            var fahrstrasse = new Fahrstrasse();
            String name;
            int taste1, taste2, gleislampeWeiss, gleislampeRot, signalNummer;
            int[] minusWeichen, plusWeichen;
            
            switch(i) {
                case 0:
                    name = "Von M auf G2";
                    taste1 = 8;
                    taste2 = 9;
                    gleislampeWeiss = 20;
                    gleislampeRot = 21;
                    int[] minusWeichen0 = {1, 2};
                    minusWeichen = minusWeichen0;
                    int[] plusWeichen0 = {0};
                    plusWeichen = plusWeichen0;
                    signalNummer = 0;
                    break;
                    
                case 1:
                    name = "Einfahrt Alsenz Gleis 2";
                    taste1 = 8;
                    taste2 = 10;
                    gleislampeWeiss = 22;
                    gleislampeRot = 23;
                    int[] minusWeichen1 = {0, 2};
                    minusWeichen = minusWeichen1;
                    int[] plusWeichen1 = {1};
                    plusWeichen = plusWeichen1;
                    signalNummer = 1;
                    break;
                    
                default:
                    name = "TBD";
                    taste1 = taste2 = gleislampeWeiss = gleislampeRot = -1; signalNummer = 0;
                    plusWeichen = new int[0];
                    minusWeichen = new int[0];
            }
            
            fahrstrasse.init(this, name, plusWeichen, minusWeichen, taste1, taste2, gleislampeWeiss, gleislampeRot, signalNummer);
            fahrstrassen[i] = fahrstrasse;
        }
    }
    
    /**
     * Stellt die Portnummern für die Signale ein.
     */
    private void initSignale() {
        for (int i = 0; i < ANZAHL_SIGNALE; i++) {
            Signal signal = new Signal();
            String name;
            int sigFahrt, sigHalt, vorsigFahrt, vorsigHalt;
            switch (i) {
                case 0:
                    name = "Sig A";
                    sigFahrt = 24;
                    sigHalt = 25;
                    vorsigFahrt = 24;
                    vorsigHalt = 25;
                    break;
                    
                case 1:
                    name = "Sig F";
                    sigFahrt = 36;
                    sigHalt = 39;
                    vorsigFahrt = 36;
                    vorsigHalt = 39;
                    break;
                    
                case 2:
                    name = "Sig P1";
                    sigFahrt = 38;
                    sigHalt = 37;
                    vorsigFahrt = 38;
                    vorsigHalt = 37;
                    break;
                    
                case 3:
                    name = "Sig P3";
                    sigFahrt = 34;
                    sigHalt = 35;
                    vorsigFahrt = 34;
                    vorsigHalt = 35;
                    break;
                    
                case 4:
                    name = "Sig N2";
                    sigFahrt = 32;
                    sigHalt = 33;
                    vorsigFahrt = 32;
                    vorsigHalt = 33;
                    break;
                    
                case 5:
                    name = "Sig N3";
                    sigFahrt = 46;
                    sigHalt = 47;
                    vorsigFahrt = 46;
                    vorsigHalt = 47;
                    break;
                    
                default:
                    name = "TBD";
                    sigFahrt = sigHalt = vorsigFahrt = vorsigHalt = -1;
                    break;
                    
            }
            
            signal.init(connector, name, sigFahrt, sigHalt, vorsigFahrt, vorsigHalt);
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
}
