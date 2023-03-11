/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.state.StreckenState;

/**
 * Diese Klasse verwaltet einen Streckenblock.
 * Sie steuert die Lampen Weiß/ Rot für den 
 * Streckenpfeil und reagiert auf die Rückblock
 * Taste A oder F.
 * 
 * @author Matthias Thiele
 */
public class Streckenblock implements TastenEvent, TickerEvent {

    private Config config;
    private int streckeWeiss;
    private int streckeRot;
    private Doppeltaster streckeTaster;
    private StreckenState streckenState;
    private String name;
    private int sperrRaeumungsmelder;
    private boolean useMJ1MJ2;
    int rueckblockenUntil;
    private boolean isInbound;
    
    private boolean melder;
    private boolean hide;
    private Doppeltaster vbHT;
    private int ersatzSignalNummer;
    
    /**
     * Die Initialisierung übergibt die Nummer der Streckenblock
     * Taste und die Nummern der weißen- und roten Pfeil-Lampe.
     * 
     * @param config
     * @param name
     * @param isInbound
     * @param streckenTaste
     * @param streckeWeiss
     * @param streckeRot 
     * @param sperrRaeumungsmelder 
     * @param vorblockHilfsTaste 
     * @param ersatzSignalNummer 
     */
    public void init(Config config, String name, boolean isInbound, 
            int streckenTaste, int streckeWeiss, int streckeRot, 
            int sperrRaeumungsmelder, int vorblockHilfsTaste,
            int ersatzSignalNummer) {
        this.config = config;
        this.name = name;
        this.isInbound = isInbound;
        this.streckeWeiss = streckeWeiss;
        this.streckeRot = streckeRot;
        this.sperrRaeumungsmelder = sperrRaeumungsmelder;
        this.ersatzSignalNummer = ersatzSignalNummer;
        
        if (streckenTaste >= 0) {
            this.streckeTaster = new Doppeltaster();
            this.streckeTaster.init(config, this, Const.BlGT, streckenTaste);
            config.ticker.add(streckeTaster);
        }
        
        streckenState = StreckenState.FREE;
        markStrecke();
        rueckblockenUntil = Integer.MAX_VALUE;
        
        if (!isInbound && vorblockHilfsTaste >= 0) {
            vbHT = new Doppeltaster();
            vbHT.init(config, this, vorblockHilfsTaste, Const.BlGT);
            config.ticker.add(vbHT);                
        }
        
        config.ticker.add(this);
    }

    /**
     * Über diesen Aufruf meldet die Fahrstraße, dass ein
     * Zug auf der Strecke ist bzw., dass er eingefahren ist.
     * 
     * Das Rückbocken erfolgt nach der Einfahrt durch die
     * Betätigung der Rückblocktaste.
     * 
     * @param trainArrived 
     */
    public void markUsed(boolean trainArrived) {
        streckenState = trainArrived ? StreckenState.TRAIN_ARRIVED : StreckenState.WAIT_FOR_TRAIN;
        markStrecke();
        config.alert("Strecke " + name + " vor/rückgeblockt.");
        if (!isInbound && (sperrRaeumungsmelder != -1)) {
            hide = true;
            rueckblockenUntil = 0;
        }
    }
    
    /**
     * Führt die Rückblockung bei den Ausfahrten durch.
     * 
     * Das würde im echten Leben durch den Fahrdienstleiter
     * vom Zielbahnhof durchgeführt werden.
     */
    public void unblock() {
        streckenState = StreckenState.FREE;
        markStrecke();
        if (!isInbound && (sperrRaeumungsmelder != -1)) {
            hide = true;
        }
    }
    
    /**
     * Die Fahrstraße wurde manuell aufgelöst, es gibt
     * keine Information über den Verbleib des Zuges.
     */
    public void cancel() {
        streckenState = StreckenState.TRAIN_CANCELED;
    }
    
    /**
     * Über diese Funktion kann die Fahrstraße abfragen, ob der
     * Streckenblock frei ist.
     * 
     * @return 
     */
    public boolean isFree() {
        return streckenState.equals(StreckenState.FREE) || streckenState.equals(StreckenState.TRAIN_CANCELED);
    }
    
    /**
     * Sperr- oder Räumungsmelder aktivieren.
     */
    public void activateSR() {
        melder = true;
    }
    
    /**
     * Doppeltaster wurde betätigt.
     */
    @Override
    public void whenPressed(int taste1, int taste2) {
        if (taste1 == Const.BlGT) {
            // Rückblocken nach Einfahrt
            if (streckenState.equals(StreckenState.TRAIN_ARRIVED) || streckenState.equals(StreckenState.TRAIN_CANCELED)) {
                streckenState = StreckenState.FREE;
                markStrecke();
                hide = true;
                if (isInbound) {
                    config.alert("Endfeld " + name + " zurückgeblockt.");
                }

                rueckblockenUntil = 0;
            }
        } else {
            // manuelles Vorblocken für Fahrt mit Hilfssignal
            if ((ersatzSignalNummer >= 0) && config.ersatzsignale[ersatzSignalNummer].isFahrt()) {
                markUsed(false);
            }
        }
    }

    
    /**
     * Setzt den Streckenpfeil auf Weiß oder Rot, je nachdem
     * ob die Strecke belegt ist.
     * 
     * @param besetzt 
     */
    private void markStrecke() {
        boolean besetzt = streckenState != StreckenState.FREE;
        config.connector.setOut(streckeRot, besetzt);
        config.connector.setOut(streckeWeiss, !besetzt);
    }

    /**
     * Zum Rückblocken läuft ein Kurbelinduktor für etwa
     * 9 Sekunden. Es gibt zwei Induktoren, die abwechselnd
     * verwendet werden (Anzeigelampen MJ1 und MJ2).
     * @param count 
     */
    @Override
    public void tick(int count) {
        if ((sperrRaeumungsmelder != -1) && (melder || hide)) {
            boolean meldung = melder;
            if (hide) {
                hide = false;
                melder = false;
                meldung = false;
            }
            
            meldung = meldung && (count & 0x8) == 0x8;
            config.connector.setOut(sperrRaeumungsmelder, meldung);            
        }
        
        if (rueckblockenUntil == 0) {
            rueckblockenUntil = count + Const.KURBELINDUKTOR_RUNDEN;
            useMJ1MJ2 = !useMJ1MJ2;
            config.connector.setOut(useMJ1MJ2 ? Const.MJ1 : Const.MJ2, true);
            config.alert("Vor-/ Rückblocken " + name + " gestartet.");
        } else {
            if (count > rueckblockenUntil) {
                config.connector.setOut(Const.MJ1, false);
                config.connector.setOut(Const.MJ2, false);
                config.alert("Vor-/ Rückblocken " + name + " beendet.");
                rueckblockenUntil = Integer.MAX_VALUE;
            }
        }
    }
}
