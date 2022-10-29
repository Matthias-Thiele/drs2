/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */

package de.mmth.drs2;

/**
 *
 * @author pi
 */
public interface TickerEvent {
    public void tick(int count);
}
