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
    public void init(Config config, int tasteS, int tasteW, int lampeS, int lampeW, int klingel) {
        this.config = config;
        tasteSint = tasteS;
        this.tasteS = new Einfachtaster();
        this.tasteS.init(config, this, tasteS);
        this.tasteW = new Einfachtaster();
        this.tasteW.init(config, this, tasteW);
        this.lampeS = lampeS;
        this.lampeW = lampeW;
        this.klingel = klingel;
        config.ticker.add(this);
    }
    
    /**
     * Ein Signal meldet eine Signalstörung.
     */
    public void stoerungS() {
        strgS = true;
    }
    
    /**
     * Eine Weiche meldet eine Weichenstörung.
     */
    public void stoerungW() {
        strgW = true;
    }
    
    /**
     * Intervallsteuerung für Anzeige und Wecker.
     * @param count 
     */
    @Override
    public void tick(int count) {
        if (strgS) {
            config.connector.setOut(lampeS, (count & 0x10) != 0);
        }
        if (strgW) {
            config.connector.setOut(lampeW, (count & 0x10) == 0);
        }
        
        if (strgS || strgW) {
            config.connector.setOut(klingel, (count & 0x10) == 0);
        }
    }

    /**
     * Überwacht die Tasten zur Weckerunterbrechung und
     * schaltet die Störmeldungsanzeige bei Betätigung ab.
     * 
     * @param taste 
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
        
        config.connector.setOut(klingel, false);
    }
    
}
