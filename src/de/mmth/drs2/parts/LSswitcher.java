/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2026 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;

/**
 *
 * @author matthias
 */
public class LSswitcher implements TastenEvent {

  private final Config config;
  private final Umschalttaster switchP1P3;
  private boolean actState = true;
  private boolean locked = true;
  
  public LSswitcher(Config config) {
    this.config = config;
    switchP1P3 = new Umschalttaster();
    switchP1P3.init(config, this, Const.SWITCH_LS);
    //updateIO();
  }

  public void setLockState(boolean locked) {
      this.locked = locked;
      if (locked) {
          signalOff();
      }
  }
  
  /**
   * Löst die Verbindung zwischen DRS2 und externen Lichtsignal.
   */
  public void signalOff() {
    config.signale[2].hasLichtsignal(false);
    config.signale[3].hasLichtsignal(false);
    config.ersatzsignale[2].hasLichtsignal(false);
    config.ersatzsignale[3].hasLichtsignal(false);
  }
  
  /**
   * Stellt die Verbindung zum zuletzt verwendeten LS wieder her.
   */
  public void signalOn() {
    if (!locked) {
      updateIO();
    }
  }
  
  @Override
  public void whenPressed(int taste1, int taste2) {
    actState = taste2 == 0;
    updateIO();
  }
  
  public void changeState(boolean isP3) {
    if (!locked && (actState != isP3)) {
      actState = isP3;
      switchP1P3.changeState(!actState);
      updateIO();
    }
  }
  
  private void updateIO() {
    if (!locked) {
        config.signale[2].hasLichtsignal(!actState);
        config.signale[3].hasLichtsignal(actState);
        config.ersatzsignale[2].hasLichtsignal(!actState);
        config.ersatzsignale[3].hasLichtsignal(actState);
        config.connector.setOut(Const.LS_P1_P3_State, !actState);
    }
  }
}
