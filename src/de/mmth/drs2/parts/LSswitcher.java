/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
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
  private int actState = 0;
  
  public LSswitcher(Config config) {
    this.config = config;
    switchP1P3 = new Umschalttaster();
    switchP1P3.init(config, this, Const.SWITCH_LS);
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
    config.signale[2].hasLichtsignal(actState != 0);
    config.signale[3].hasLichtsignal(actState == 0);
    config.ersatzsignale[2].hasLichtsignal(actState != 0);
    config.ersatzsignale[3].hasLichtsignal(actState == 0);
    
  }
  
  @Override
  public void whenPressed(int taste1, int taste2) {
    actState = taste2;
    config.signale[2].hasLichtsignal(taste2 != 0);
    config.signale[3].hasLichtsignal(taste2 == 0);
    config.ersatzsignale[2].hasLichtsignal(taste2 != 0);
    config.ersatzsignale[3].hasLichtsignal(taste2 == 0);
  }
  
}
