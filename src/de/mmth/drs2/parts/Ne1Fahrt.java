/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2026 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;

/**
 * Simuliert eine Einfahrt aus dem Gegengleis.
 * 
 * Startpunkt ist die Ne1 Tafel und der Lokführer
 * hat sich beim Fahrdienstleiter gemeldet.
 * 
 * @author matthias
 */
public class Ne1Fahrt {

  private final Config config;
  private final Rangierfahrt[] fahrwege = new Rangierfahrt[3];
  
  /**
   * 
   * @param config 
   */
  public Ne1Fahrt(Config config) {
    this.config = config;
    fahrwege[0] = new Rangierfahrt();
    fahrwege[0].init(config, "Ne1 nach Gleis 1", "DL B1 W1 GZ1 Z");
    fahrwege[1] = new Rangierfahrt();
    fahrwege[1].init(config, "Ne1 nach Gleis 2", "DL B1 W1 SN2 W2 W3 GZ2 Z");
    fahrwege[2] = new Rangierfahrt();
    fahrwege[2].init(config, "Ne1 nach Gleis 3", "DL B1 W1 SN2 W2 W3 GZ3 Z");
  }
  
  /**
   * Durchführen einer Einfahrt aus Richtung Althengstett.
   * 
   * @return 
   */
  public boolean startAH() {
    int gleis = checkDestination();
    
    fahrwege[gleis - 1].startNe1();
    return true;
    
  }
  
  private int checkDestination() {
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
}
