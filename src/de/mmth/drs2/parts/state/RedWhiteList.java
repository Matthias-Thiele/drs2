/*
 * (c) 2026 by Matthias Thiele
 * DRS2 Stellpultsteuerung für Raspberry Pi
 */
package de.mmth.drs2.parts.state;

import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.ColorMarker;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author matthias
 */
public class RedWhiteList implements TickerEvent {

  class RWData {
    public int expirationTime;
    public ColorMarker nextRed;
    public boolean clear;
    
    public RWData(int expirationTime, ColorMarker nextRed, boolean clear) {
      this.expirationTime = expirationTime;
      this.nextRed = nextRed;
      this.clear = clear;
    };
  }
  
  private final List<RWData> activeReds = new ArrayList<>();
  
  public void add(int expirationTime, ColorMarker nextRed, boolean clear) {
    System.out.println("Add red timer " + nextRed.getName() + ", Time: " + expirationTime);
    var data = new RWData(expirationTime, nextRed, clear);
    activeReds.add(data);
    nextRed.red();
  }
  
  @Override
  public void tick(int count) {
    for (var d: activeReds) {
      if (d.expirationTime < count) {
        System.out.println("Clear red " + d.nextRed.getName() + ", Time: " + d.expirationTime);
        if (d.clear) {
          d.nextRed.clear();
        } else {
          d.nextRed.white();
        }
        
        activeReds.remove(d);
        break;
      }
    }
  }
  
}
