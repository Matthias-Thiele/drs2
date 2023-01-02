/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts.state;

/**
 *
 * @author pi
 */
public enum StreckenState {
    FREE,
    WAIT_FOR_TRAIN,
    TRAIN_ARRIVED
}
