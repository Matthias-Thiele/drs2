/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;

/**
 *
 * @author matthias
 */
public class Blinklicht implements TickerEvent {

  private Config config;
  private boolean onOff = true;
  private boolean stoerung;
  
  public void init(Config config) {
        this.config = config;
        config.ticker.add(this);
  }
  
  @Override
  public void tick(int count) {
    onOff = (count & 0x8) == 0x8;
  }
  
  public void setStoerung(boolean stoerung) {
    this.stoerung = stoerung;
    config.connector.setOut(Const.BLINK_STOERUNG, stoerung);
  }
  
  public boolean getStoerung() {
    return stoerung;
  }
  
  public boolean getBlink() {
    return stoerung ? true : onOff;
  }
}
