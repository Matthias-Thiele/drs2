/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2026 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;

/**
 * Simuliert eine Einfahrt aus dem Gegengleis.
 * 
 * Startpunkt ist die Ne1 Tafel und der Lokführer
 * hat sich beim Fahrdienstleiter gemeldet.
 * 
 * @author matthias
 */
public class Ne1Fahrt implements TastenEvent {

  private final Config config;
  private final Rangierfahrt[] fahrwege = new Rangierfahrt[5];
  
  /**
   * 
   * @param config 
   */
  public Ne1Fahrt(Config config) {
    this.config = config;
    fahrwege[0] = new Rangierfahrt();
    fahrwege[0].init(config, "Ne1 AH nach Gleis 1", "DL B1 W1 GZ1 Z");
    fahrwege[1] = new Rangierfahrt();
    fahrwege[1].init(config, "Ne1 AH nach Gleis 2", "DL B1 W1 SN2 W2 W3 GZ2 Z");
    fahrwege[2] = new Rangierfahrt();
    fahrwege[2].init(config, "Ne1 AH nach Gleis 3", "DL B1 W1 SN2 W2 W3 GZ3 Z");
    fahrwege[3] = new Rangierfahrt();
    fahrwege[3].init(config, "Ne1 WB nach Gleis 2", "DL B3 SP5 W5 W4 GZ2 Z");
    fahrwege[4] = new Rangierfahrt();
    fahrwege[4].init(config, "Ne1 WB nach Gleis 3", "DL B3 SP5 W5 W4 GZ3 Z");
    
    var tasterAH = new Einfachtaster();
    tasterAH.init(config, this, Const.NE1_COMMAND_AH);
    var tasterWB = new Einfachtaster();
    tasterWB.init(config, this, Const.NE1_COMMAND_WB);
  }
  
  /**
   * Durchführen einer Einfahrt aus Richtung Althengstett.
   * 
   * @return 
   */
  public boolean startAH() {
    int gleis = checkDestinationAH();
    
    if (fahrwege[gleis - 1].isRunning()) {
      return false;
    } else {
      fahrwege[gleis - 1].startNe1(true);
      return true;
    }
  }
  
  /**
   * Durchführen einer Einfahrt aus Richtung Wildberg.
   * 
   * @return 
   */
  public boolean startWB() {
    int gleis = checkDestinationWB();
    
    if (fahrwege[gleis - 1].isRunning()) {
      return false;
    } else {
      fahrwege[gleis - 1].startNe1(false);
      return true;
    }
  }
  
  private int checkDestinationWB() {
    Weiche[] weichen = config.weichen;
    
    if (weichen[3].isPlus()) {
      // Einfahrt nach Gleis 1
      return 4;
    } else {
      return 5;
    }
  }

  private int checkDestinationAH() {
    Weiche[] weichen = config.weichen;
    
    if (weichen[0].isPlus()) {
      // Einfahrt nach Gleis 1
      return 1;
    } else if (weichen[2].isPlus()) {
      return 2;
    } else {
      return 3;
    }
  }

    @Override
    public void whenPressed(int taste1, int taste2) {
      if (taste1 == Const.NE1_COMMAND_AH) {
        startAH();
      } else {
        startWB();
      }
    }
}
