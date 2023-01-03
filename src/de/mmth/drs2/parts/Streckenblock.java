/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.parts.state.StreckenState;

/**
 * Diese Klasse verwaltet einen Streckenblock.
 * Sie steuert die Lampen Weiß/ Rot für den 
 * Streckenpfeil und reagiert auf die Rückblock
 * Taste A oder F.
 * 
 * @author Matthias Thiele
 */
public class Streckenblock implements TastenEvent {

    private Config config;
    private int streckeWeiss;
    private int streckeRot;
    private Doppeltaster streckeTaster;
    private StreckenState streckenState;
    private String name;
    
    /**
     * Die Initialisierung übergibt die Nummer der Streckenblock
     * Taste und die Nummern der weißen- und roten Pfeil-Lampe.
     * 
     * @param config
     * @param name
     * @param streckenTaste
     * @param streckeWeiss
     * @param streckeRot 
     */
    public void init(Config config, String name, int streckenTaste, int streckeWeiss, int streckeRot) {
        this.config = config;
        this.name = name;
        this.streckeWeiss = streckeWeiss;
        this.streckeRot = streckeRot;
        if (streckenTaste >= 0) {
            this.streckeTaster = new Doppeltaster();
            this.streckeTaster.init(config, this, 22, streckenTaste);
            config.ticker.add(streckeTaster);
        }
        
        streckenState = StreckenState.FREE;
        markStrecke();
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
        config.alert("Strecke " + name + " vorgeblockt.");
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
    }
    
    /**
     * Über diese Funktion kann die Fahrstraße abfragen, ob der
     * Streckenblock frei ist.
     * 
     * @return 
     */
    public boolean isFree() {
        return streckenState.equals(StreckenState.FREE);
    }
    
    /**
     * Doppeltaster wurde betätigt.
     */
    @Override
    public void whenPressed(int taste) {
        if (streckenState.equals(StreckenState.TRAIN_ARRIVED)) {
            streckenState = StreckenState.FREE;
            markStrecke();
            config.alert("Endfeld " + name + " zurückgeblockt.");
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
}
