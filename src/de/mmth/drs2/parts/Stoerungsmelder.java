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
    private int klingel;
    private int tu;
    private boolean strgT;
    
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
     */
    public void init(Config config, int tasteS, int tasteW, int lampeS, int lampeW, int klingel,
            int tastenUeberwacher) {
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
        config.connector.setOut(klingel, true);
    }
    
    public void stoerungT() {
        strgT = true;
    }
    
    public void startCheckT() {
        strgT = false;
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
        
        config.connector.setOut(klingel, strgS || strgT || strgW);
        config.connector.setOut(tu, strgT);
    }

    /**
     * Prüft nach, ob an einem der Signale
     * eine Störung vorliegt.
     */
    private void checkSignalstoerung() {
        strgS = false;
        for (Signal signale : config.signale) {
            if (signale.isGestoert()) {
                strgS = true;
                break;
            }
        }
    }
    
    /**
     * Prüft nach, ob an einem der Signale
     * eine Störung vorliegt.
     */
    private void checkWeichenstoerung() {
        strgW = false;
        for (Weiche weichen : config.weichen) {
            if (weichen.isGestoert()) {
                strgW = true;
                break;
            }
        }
    }
    
    /**
     * Überwacht die Tasten zur Weckerunterbrechung und
     * schaltet die Störmeldungsanzeige bei Betätigung ab.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        if (taste1 == tasteSint) {
            strgS = false;
            config.connector.setOut(lampeS, false);
        } else {
            strgW = false;
            config.connector.setOut(lampeW, false);
        }
    }
    
}
