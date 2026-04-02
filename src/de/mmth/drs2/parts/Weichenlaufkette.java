/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.TickerEvent;

/**
 *
 * @author matthias
 */
public class Weichenlaufkette implements TickerEvent {

  private final Weiche[] plusWeichen;
  private final Weiche[] minusWeichen;
  
  private int activePlus = -1;
  private int activeMinus = -1;
  private boolean hasError;
  private boolean done;
  private boolean doStart = false;
  
  /**
   * Initialisiert eine Weichenkette zu einer Fahrstraße.
   * 
   * @param plusWeichen
   * @param minusWeichen 
   */
  public Weichenlaufkette(Weiche[] plusWeichen, Weiche[] minusWeichen) {
    this.plusWeichen = plusWeichen;
    this.minusWeichen = minusWeichen;
  }

  /**
   * Startet den Umlauf der beteiligten Weichen.
   */
  public void start() {
    hasError = false;
    if (plusWeichen.length > 0) {
      activePlus = 0;
    } else {
      activeMinus = 0;
    }
    
    doStart = true;
  }
  
  /**
   * Meldet zurück, ob alle Umläufe abgearbeitet wurden.
   * @return 
   */
  public boolean isRunning() {
    return !hasError && ((activePlus != -1) || (activeMinus != -1));
  }
  
  public boolean isReady() {
    if (done) {
      done = false;
      
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Meldet zurück, ob beim Umlauf ein Fehler aufgetreten ist.
   * 
   * @return 
   */
  public boolean hasError() {
    return hasError;
  }
  
  private void processSwitch() {
    boolean isPlus = activePlus != -1;
    Weiche weiche;
    if (isPlus) {
      weiche = plusWeichen[activePlus];
    } else if (activeMinus != -1) { 
      weiche = minusWeichen[activeMinus];
    } else {
      return;
    }

    if (doStart) {
      doStart = false;
      if (!weiche.starteUmlauf()) {
        hasError = true;
        activePlus = -1;
        activeMinus = -1;
      }
    } else {
      if (weiche.isRunning()) {
        return;
      }
    }
    
    var isOk = (weiche.isPlus() == isPlus) && !weiche.isRunning();
    if (isOk) {
      if (isPlus) {
        doStart = true;
        activePlus++;
        if (activePlus >= plusWeichen.length) {
          activePlus = -1;
          if (minusWeichen.length > 0) {
            activeMinus = 0;
          } else {
            doStart = false;
            done = true;
          }
        }
      } else {
        doStart = true;
        activeMinus++;
        if (activeMinus >= minusWeichen.length) {
          activeMinus = -1;
          doStart = false;
          done = true;
        }
      }
    } else {
      if (weiche.isGestoert() || weiche.isLocked()) {
        hasError = true;
        activePlus = -1;
        activeMinus = -1;
      }
    }
  }
  
  @Override
  public void tick(int count) {
    processSwitch();
  }
}
