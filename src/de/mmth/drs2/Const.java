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
    // Eingänge
    
    // Tastenanschalter
    public static final int TA = 44;
    
    // Gruppentasten allgemein
    public static final int ErsGT = 16;
    public static final int FHT = 25;
    public static final int HaGT = 20;
    public static final int SGT = 19;
    public static final int WuT_S = 23;
    public static final int WuT_W = 24;
    
    // Streckenblock
    public static final int BlGT = 22;
    public static final int AsT = 27;
    public static final int AsLT = 28;
    public static final int VbHT_M = 29;
    public static final int VbHT_H = 30;
    public static final int RbHGT = 31;
    
    public static final int BlockMIn = 17; // Gleicher Input für Ein- und Ausfahrt, wird geändert
    public static final int BlockMOut = 43; // Gleicher Input für Ein- und Ausfahrt, wird geändert
    public static final int BlockHIn = 18; // Gleicher Input für Ein- und Ausfahrt, wird geändert
    public static final int BlockHOut = 42; // Gleicher Input für Ein- und Ausfahrt, wird geändert
    
    // Weichentasten
    public static final int WGT = 0;
    public static final int SlFLT = 26;
    public static final int SlFT3 = 21;
    public static final int SlFT1 = 41;
    public static final int SlFT4 = 40;
    
    public static final int WEICHE3 = 1;
    public static final int WEICHE4 = 2;
    public static final int WEICHE5 = 3;
    public static final int WEICHE18 = 4;
    public static final int WEICHE19 = 5;
    public static final int WEICHE20 = 6;
    
    // Gleistasten
    public static final int GLEIS1 = 7;
    public static final int GLEIS2 = 8;
    public static final int GLEIS3 = 9;
    
    // Schlüsselschalter
    public static final int SCHLUESSEL_A = 33;
    public static final int SCHLUESSEL_F = 32;
    
    // Schlüsselweiche
    public static final int WSCHLUESSEL3 = 34;
    public static final int WSCHLUESSEL1 = 35;
    public static final int WSCHLUESSEL4 = 37;
    
    // Signaltasten
    public static final int SIGNAL_A = 10;
    public static final int SIGNAL_F = 11;
    public static final int SIGNAL_P1 = 12;
    public static final int SIGNAL_P3 = 13;
    public static final int SIGNAL_N2 = 14;
    public static final int SIGNAL_N3 = 15;
    
    
    // Ausgänge
    public static final int NC = -1;
    
    // Zusätzliche Vorsignale für Durchfahrten
    public static final int Vp13 = 87;
    public static final int Vn23 = 72;
    
    public static final int WVp1Weiss = 85;
    public static final int WVp1Gruen = 86;
    public static final int WVp1 = 87;
    
    // Anzeigelampen und Zeiten für Motorinduktor
    public static final int MJ1 = 92;
    public static final int MJ2 = 91;
    public static final int KURBELINDUKTOR_RUNDEN = 120;
    
    // Streckenblock
    public static final int StreckeVonHWeiss = 62;
    public static final int StreckeVonHRot = 61;
    public static final int StreckeNachHWeiss = 58;
    public static final int StreckeNachHRot = 57;
    public static final int EinfFestlegemelderH = 95;
    public static final int EinfRaeumungsmelderH = 81;
    public static final int AusfSperrmelderH = 83;
    public static final int AusfFestlegemelderH = 94;
    
    public static final int StreckeVonMWeiss = 60;
    public static final int StreckeVonMRot = 59;
    public static final int StreckeNachMWeiss = 56;
    public static final int StreckeNachMRot = 71;
    public static final int EinfFestlegemelderM = 80;
    public static final int EinfRaeumungsmelderM = 82;
    public static final int AusfSperrmelderM = 84;
    public static final int AusfFestlegemelderM = 93;
    
    // Weichenschlüssel
    public static final int SlFT3Rot = 70;
    public static final int SlFT3Weiss = 69;
    public static final int SlFT1Rot = 109;
    public static final int SlFT1Weiss = 108;
    public static final int SlFT4Rot = 111;
    public static final int SlFT4Weiss = 110;
    public static final int SlFT3Relais = 96;
    public static final int SlFT1Relais = 98;
    public static final int SlFT4Relais = 99;
    public static final int Wecker = 97;
}
