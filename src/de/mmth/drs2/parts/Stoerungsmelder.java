/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;

/**
 * Diese Klasse implementiert die Behandlung der
 * Störmeldungen.
 * 
 * @author Matthias Thiele
 */
public class Stoerungsmelder implements TickerEvent, TastenEvent {

    private Config config;
    private Einfachtaster tasteS;
    private Einfachtaster tasteW;
    private int tasteSint;
    private int lampeS;
    private int lampeW;

    private boolean strgS = false;
    private boolean strgW = false;
    private boolean strgT = false;
    private boolean beepS = false;
    private boolean beepW = false;
    
    private int klingel;
    private int tu;
    private int melder;
    
    private String lastSignalStoerung = "";
    private Object lastWeichenStoerung = "";
    private int wecker;
    
    /**
     * Initialisiert die Ports für die Tastenabschalter und
     * Anzeigen sowie den Wecker.
     * 
     * @param config
     * @param tasteS
     * @param tasteW
     * @param lampeS
     * @param lampeW
     * @param klingel 
     * @param tastenUeberwacher 
     * @param wecker 
     */
    public void init(Config config, int tasteS, int tasteW, int lampeS, int lampeW, int klingel,
            int tastenUeberwacher, int wecker) {
        this.config = config;
        tasteSint = tasteS;
        this.tasteS = new Einfachtaster();
        this.tasteS.init(config, this, tasteS);
        this.tasteW = new Einfachtaster();
        this.tasteW.init(config, this, tasteW);
        this.lampeS = lampeS;
        this.lampeW = lampeW;
        this.klingel = klingel;
        this.tu = tastenUeberwacher;
        this.wecker = wecker;
        config.ticker.add(this);
    }
    
    /**
     * Ein Signal meldet eine Signalstörung.
     */
    public void stoerungS() {
        strgS = true;
        config.connector.setOut(klingel, true);
    }
    
    /**
     * Eine Weiche meldet eine Weichenstörung.
     */
    public void stoerungW() {
        strgW = true;
        //config.connector.setOut(klingel, true);
    }
    
    public void stoerungT() {
        strgT = true;
    }
    
    public void startCheckT() {
        strgT = false;
    }
    
    /**
     * Streckenblockmeldung
     */
    public void meldung() {
        melder = 100;
        config.connector.setOut(wecker, true);
        System.out.println("Wecker ein.");
    }
    
    /**
     * Rangiermeldung
     */
    public void rangierMeldung() {
        melder = 15;
        config.connector.setOut(wecker, true);
        System.out.println("Wecker ein.");
    }
    
    /**
     * Intervallsteuerung für Anzeige und Wecker.
     * @param count 
     */
    @Override
    public void tick(int count) {
        checkSignalstoerung();
        if (strgS) {
            config.connector.setOut(lampeS, (count & 0x10) != 0);
        } else {
            config.connector.setOut(lampeS, false);
        }
        
        checkWeichenstoerung();
        if (strgW) {
            config.connector.setOut(lampeW, (count & 0x10) == 0);
        } else {
            config.connector.setOut(lampeW, false);
        }
        
        if (!strgS) {
          beepS = false;
        }
        
        if (!strgW) {
          beepW = false;
        }
        
        if (melder > 0) {
            melder--;
        } else {
            config.connector.setOut(wecker, false);
        }
        
        config.connector.setOut(klingel, beepS | beepW | strgT);
        config.connector.setOut(tu, strgT);
    }

    /**
     * Prüft nach, ob an einem der Signale
     * eine Störung vorliegt.
     */
    private void checkSignalstoerung() {
        for (Signal signal : config.signale) {
            if (signal.isGestoert()) {
                if (!signal.getName().equals(lastSignalStoerung)) {
                    strgS = true;
                    beepS = true;
                    lastSignalStoerung = signal.getName();
                    config.alert("Signalstörung: " + lastSignalStoerung);
                }
                
                return;
            }
        }
        
        strgS = false;
        lastSignalStoerung = "";
    }
    
    /**
     * Prüft nach, ob an einer der Weichen
     * eine Störung vorliegt.
     */
    private void checkWeichenstoerung() {
        for (Weiche weiche : config.weichen) {
            if (weiche.isGestoert()) {
                if (!weiche.getName().equals(lastWeichenStoerung)) {
                    strgW = true;
                    beepW = true;
                    lastWeichenStoerung = weiche.getName();
                    config.alert("Weichenstörung: " + lastWeichenStoerung);
                }
                
                return;
            }
        }
        
        strgW = false;
        lastWeichenStoerung = "";
    }
    
    /**
     * Überwacht die Tasten zur Weckerunterbrechung und
     * schaltet die Störmeldungsanzeige bei Betätigung ab.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        if (taste1 == tasteSint) {
            beepS = false;
        } else {
            beepW = false;
        }
    }
    
}
