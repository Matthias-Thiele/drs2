/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2026 Matthias Thiele
 */
package de.mmth.drs2.parts.state;

/**
 *
 * @author matthias
 */
public enum SwitchState {
  OK(0),
  AUFGEFAHREN(1),
  EINFACHE_BLOCKIERUNG(2),
  DAUERHAFTE_BLOCKIERUNG(3);
  
  private final int value;
  
  private SwitchState(int value) {
    this.value = value;
  }
  
  public int getValue() {
    return value;
  }
  
}
