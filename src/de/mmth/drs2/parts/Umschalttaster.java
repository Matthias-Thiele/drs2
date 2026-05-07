/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2026 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.TickerEvent;

/**
 * Taster der bei jeder Betätigung seinen Zustand invertiert.
 * 
 * @author matthias
 */
public class Umschalttaster implements TickerEvent {

  private Config config;
  private TastenEvent activateWhenPressed;
  private int taste;
  private boolean wasSet = false;
  private boolean activated = false;
  private boolean actState = false;

  /**
   * Initialisiert einen neuen Umschalttaster.
   * 
   * @param config
   * @param activateWhenPressed
   * @param taste 
   */
  public void init(Config config, TastenEvent activateWhenPressed, int taste) {
    this.config = config;
    config.ticker.add(this);
    this.activateWhenPressed = activateWhenPressed;
    this.taste = taste;
    
  }
  
  /**
   * Prüft regelmäßig ob der Taster betätigt wurde.
   * 
   * @param count 
   */
  @Override
  public void tick(int count) {
    if (config.connector.isInSet(taste)) {
      if (wasSet) {
        if (!activated) {
          actState = !actState;
          activated = true;
          activateWhenPressed.whenPressed(taste, actState ? 1 : 0);
        }
      } else {
        wasSet = true;
      }
    } else {
      wasSet = false;
      activated = false;
    }
  }
  
}
