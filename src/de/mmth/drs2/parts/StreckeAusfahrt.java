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
public class StreckeAusfahrt extends Strecke {
    private boolean pendingClear = false;
    
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
        streckenState = isInUse ? StreckenState.WAIT_FOR_TRAIN : StreckenState.FREE;
        if (pendingClear && isInUse) {
            sperrRaeummelder = false;
        }
        
        markStrecke();
    }

    /**
     * Wenn das Signal hinter dem ausfahrenden Zug auf Halt fällt,
     * wird die Wiederholsperre gesetzt.
     */
    @Override
    public void setWiederholsperre() {
        System.out.println("Wiederholsperre setzen.");
        sperrRaeummelder = true;
        markStrecke();
    }
    
    @Override
    public void clearWiederholsperre() {
        System.out.println("Wiederholsperre löschen.");
        pendingClear = true;
        markStrecke();
    }
    
    /**
     * Mit der Fahrstraßenauflösung wird der Melder zurückgesetzt.
     */
    @Override
    public void fahrstrassenauflösung() {
        festlegemelder = false;
        markStrecke();
    }
    
    /**
     * Der ausfahrende Zug blockt automatisch vor und löst die Wiederholsperre. 
     * 
     * @param mitErsatzsignal
     */
    @Override
    public void activateGleiskontakt(boolean mitErsatzsignal) {
        if (!mitErsatzsignal) {
          triggerBlock();
        }
        
        clearWiederholsperre();
    }
    
    @Override
    public void whenPressed(int taste1, int taste2) {
        switch (taste1) {
            case Const.AsT:
                setWiederholsperre();
                break;
                
            case Const.AsLT:
                sperrRaeummelder = false;
                markStrecke();
                break;
            
            case Const.BlGT:
                if (taste2 == vorblockHilfsTaste) {
                    // Bei der Ausfahrt über Hilfssignal muss manuell vorgeblockt werden.
                    triggerBlock();
                    clearWiederholsperre();
                    rueckblockenUntil = 0;
                }
                break;
                
            default:
                
        }
    }

}
