/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2;

/**
 *
 * @author pi
 */
public class Const {
    public static final int BlGT = 22;
    public static final int ErsGT = 16;
    public static final int FHT = 25;
    public static final int HaGT = 20;
    public static final int SGT = 19;
    public static final int WGT = 0;
    public static final int WuT_S = 23;
    public static final int WuT_W = 24;
    
    // Zusätzliche Vorsignale für Durchfahrten
    public static final int Vp13 = 87;
    public static final int WVp1 = 86;
    
    // Anzeigelampen und Zeiten für Motorinduktor
    public static final int MJ1 = 92;
    public static final int MJ2 = 91;
    public static final int KURBELINDUKTOR_RUNDEN = 120;
}
