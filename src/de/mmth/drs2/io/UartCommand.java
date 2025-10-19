/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.io;

/**
 *
 * @author root
 */
public enum UartCommand {
    TICKER,
    GET_STATUS,
    BLOCK1,
    BLOCK2,
    BLOCK3,
    BLOCK4,
    FLIP1,
    FLIP2,
    FLIP3,
    FLIP4,
    UPDATE_OUTPUTS
}
