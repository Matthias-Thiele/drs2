/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

import de.mmth.drs2.fx.MainPane;
import de.mmth.drs2.io.Connector;
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
     * Liste der Weichen aus dem Stellpult.
     */
    public final Weiche[] weichen = new Weiche[ANZAHL_WEICHEN];
    
    /**
     * Liste der Fahrstraßen aus dem Stellpult.
     */
    public final Fahrstrasse[] fahrstrassen = new Fahrstrasse[ANZAHL_FAHRSTRASSEN];
    
    /**
     * JavaFX Anzeige des Systemzustands.
     */
    public MainPane mainPane;
    
    
    /**
     * Initialisiert die Systemkonfiguration
     */
    public void init() {
        initWeichen();
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
                    name = "W1";
                    taste1 = 0;
                    taste2 = 1;
                    whitePlus = 0;
                    whiteMinus = 1;
                    redPlus = 2;
                    redMinus = 3;
                    break;
                
                case 1:
                    name = "W2";
                    taste1 = 2;
                    taste2 = 3;
                    whitePlus = 4;
                    whiteMinus = 5;
                    redPlus = 6;
                    redMinus = 7;
                    break;
                
                case 2:
                    name = "W3";
                    taste1 = 4;
                    taste2 = 5;
                    whitePlus = 8;
                    whiteMinus = 9;
                    redPlus = 10;
                    redMinus = 11;
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
            int taste1, taste2, gleislampeWeiss, gleislampeRot;
            int[] minusWeichen, plusWeichen;
            
            switch(i) {
                case 0:
                    name = "Einfahrt Alsenz Gleis 1";
                    taste1 = 8;
                    taste2 = 9;
                    gleislampeWeiss = 20;
                    gleislampeRot = 21;
                    int[] minusWeichen0 = {1, 2};
                    minusWeichen = minusWeichen0;
                    int[] plusWeichen0 = {0};
                    plusWeichen = plusWeichen0;
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
                    break;
                    
                default:
                    name = "TBD";
                    taste1 = taste2 = gleislampeWeiss = gleislampeRot = -1;
                    plusWeichen = new int[0];
                    minusWeichen = new int[0];
            }
            
            fahrstrasse.init(this, name, plusWeichen, minusWeichen, taste1, taste2, gleislampeWeiss);
            fahrstrassen[i] = fahrstrasse;
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
