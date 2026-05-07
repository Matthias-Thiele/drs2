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
  public LSswitcher(Config config) {
    this.config = config;
    switchP1P3 = new Umschalttaster();
    switchP1P3.init(config, this, Const.SWITCH_LS);
  }

  @Override
  public void whenPressed(int taste1, int taste2) {
    config.signale[2].hasLichtsignal(taste2 != 0);
    config.signale[3].hasLichtsignal(taste2 == 0);
  }
  
}
