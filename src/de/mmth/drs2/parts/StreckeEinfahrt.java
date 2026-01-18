/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Const;
import de.mmth.drs2.parts.state.StreckenState;

/**
 *
 * @author root
 */
public class StreckeEinfahrt extends Strecke {

    private boolean allowAsLT;
    private boolean raeumungsmelderAktiv = false;

    public StreckeEinfahrt() {
      isInbound = true;
    }
    
    /**
     * Wird vom Arduino im Streckenblock aufgerufen wenn sich der
     * Status der Strecke ändert. 
     * 
     * Der Block selber wird vom Relaisblock verwaltet, die DRS2
     * Steuerung kümmert sich nur um die Anzeige und das Anstoßen
     * von Änderungen (Vor-, Rückblocken).
     * 
     * @param isInUse 
     */
    @Override
    public void updateStreckenblock(boolean isInUse) {
        if (isInUse && streckenState == StreckenState.FREE) {
            streckenState = StreckenState.WAIT_FOR_TRAIN;
            if (name.endsWith("H")) {
              config.pendingTrainH = Const.PENDING_TRAIN_DURATION;
            } else if (name.endsWith("M")) {
              config.pendingTrainM = Const.PENDING_TRAIN_DURATION;
            }
            
            if (simulationMode) {
              boolean weckerEin = true;
              if (signalId >= 0) {
                if (config.signale[signalId].isFahrt()) {
                  // kein Wecker, wenn das Einfahrtssignal bereits auf Fahrt steht.
                  weckerEin = false;
                }
              }
              
              if (weckerEin) {
                config.stoerungsmelder.meldung();
              }
            }
        } else if (!isInUse) {
            if (!streckenState.equals(StreckenState.FREE) && name.endsWith("M")) {
              config.connector.setOut(Const.ZSM, false);
            }
            
            streckenState = StreckenState.FREE;
        }
        
        markStrecke();
    }

    /**
     * Der einfahrende Zug setzt den Melder. 
     * 
     * @param mitErsatzsignal
     */
    @Override
    public void activateGleiskontakt(boolean mitErsatzsignal) {
        if (streckenState == StreckenState.WAIT_FOR_TRAIN) {
            if (!mitErsatzsignal) {
                sperrRaeummelder = true;
            }
            config.alert("Gleiskontakt " + name + " wurde befahren.");
        } else {
            config.alert("Gleiskontakt auf nicht vorgeblockter Strecke wurde befahren.");
        }
    }
    
    /**
     * Mit der Fahrstraßenauflösung wird der Melder blinkend und
     * durch das Rückblocken wird er zurückgesetzt.
     */
    @Override
    public void fahrstrassenauflösung() {
        if (streckenState == StreckenState.WAIT_FOR_TRAIN && festlegemelder) {
            streckenState = StreckenState.TRAIN_ARRIVED;
        }
        
        festlegemelder = false;
        markStrecke();
    }
    
    @Override
    public void whenPressed(int taste1, int taste2) {
        if (taste1 == Const.BlGT) {
            // Zug ist eingefahren, Strecke wird zurückgeblockt.
            if ((streckenState == StreckenState.TRAIN_ARRIVED) || raeumungsmelderAktiv) {
                triggerBlock();
                rueckblockenUntil = 0;
                raeumungsmelderAktiv = false;
            } else {
                config.alert("Zug ist noch nicht eingefahren.");
            }
        } else if (taste1 == Const.RbHGT) {
            if (streckenState != StreckenState.FREE) {
                int signalNr = blockPort == (Const.StreckeVonAH) ? 0 : 1;
                if (config.ersatzsignale[signalNr].isFahrt() || config.signale[signalNr].isFahrt() || config.signale[signalNr].isSh1()) {
                    config.alert("Signal oder Ersatzsignal noch auf Fahrt.");
                } else {
                    triggerBlock();
                    rueckblockenUntil = 0;
                }
            }
        } else if (taste1 == Const.AsT) {
            //if (streckenState != StreckenState.FREE) {
                raeumungsmelderAktiv = true;
                allowAsLT = true;
            //}
        } else if (taste1 == Const.AsLT) {
            if (allowAsLT) {
                allowAsLT = false;
                raeumungsmelderAktiv = false;
            }
        } else if (taste2 == vorblockHilfsTaste) {
            // Zug ist ausgefallen oder per Hilfssignal eingefahren
            streckenState = StreckenState.TRAIN_ARRIVED;
        }
        
        markStrecke();
    }

    @Override
    public void tick(int count) {
        super.tick(count);
        var meldung = raeumungsmelderAktiv;
        if (streckenState == StreckenState.TRAIN_ARRIVED) {
            meldung &= config.blinklicht.getBlink();
        }
        config.connector.setOut(sperrRaeumungsmelder, meldung);
    }

    @Override
    public void setWiederholsperre() {
        config.alert("Es gibt keine Wiederholsperre bei der Einfahrt!");
    }

    @Override
    public void clearWiederholsperre() {
        config.alert("Es gibt keine Wiederholsperre bei der Einfahrt!");
    }
    
}
