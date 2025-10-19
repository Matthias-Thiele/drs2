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

    /**
     * Wird vom Arduino im Streckenblock aufgerufen wenn sich der
     * Status der Strecke ändert. 
     * 
     * Der Block selber wird vom Arduino verwaltet, die DRS2
     * Steuerung kümmert sich nur um die Anzeige und das Anstoßen
     * von Änderungen (Vor-, Rückblocken).
     * 
     * @param isInUse 
     */
    @Override
    public void updateStreckenblock(boolean isInUse) {
        if (isInUse && streckenState == StreckenState.FREE) {
            streckenState = StreckenState.WAIT_FOR_TRAIN;
        } else if (!isInUse) {
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
            sperrRaeummelder = true;
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
            if (streckenState == StreckenState.TRAIN_ARRIVED) {
                config.uart1.sendCommand(blockCommand);
                rueckblockenUntil = 0;
            } else {
                config.alert("Zug ist noch nicht eingefahren.");
            }
        } else if (taste1 == Const.RbHGT) {
            if (streckenState != StreckenState.FREE) {
                config.uart1.sendCommand(blockCommand);
                rueckblockenUntil = 0;
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
        //raeumungsmelderAktiv |= (streckenState != StreckenState.FREE);
        
        var meldung = raeumungsmelderAktiv;
        if (streckenState == StreckenState.TRAIN_ARRIVED) {
            meldung = (count & 0x8) == 0x8;
        }
        config.connector.setOut(sperrRaeumungsmelder, meldung);

        /*
        if (rueckblockenUntil == 0) {
            rueckblockenUntil = count + Const.KURBELINDUKTOR_RUNDEN;
            useMJ1MJ2 = !useMJ1MJ2;
            config.connector.setOut(useMJ1MJ2 ? Const.MJ1 : Const.MJ2, true);
            config.alert("Rückblocken " + name + " gestartet.");
        } else if (rueckblockenUntil != Integer.MAX_VALUE) {
            if (count > rueckblockenUntil) {
                config.connector.setOut(Const.MJ1, false);
                config.connector.setOut(Const.MJ2, false);
                config.alert("Rückblocken " + name + " beendet.");
                rueckblockenUntil = Integer.MAX_VALUE;
                raeumungsmelderAktiv = false;
            }
        }
        */
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
