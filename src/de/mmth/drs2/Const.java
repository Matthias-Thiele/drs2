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
    public final static int PENDING_TRAIN_DURATION = 600;

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
    public static final int SCHLUESSEL_A = 56;
    public static final int SCHLUESSEL_F = 57;
    public static final int BLOCK_AH_OUT = 60;
    public static final int BLOCK_AH_IN = 61;
    public static final int MOTORINDUKTOR = 62;
    public static final int WHSPERRE_ZURÜCK = 63;
    
    // Schlüsselweiche
    public static final int WSCHLUESSEL3 = 34;
    public static final int WSCHLUESSEL1 = 58;
    public static final int WSCHLUESSEL4 = 59;
    
    // Signaltasten
    public static final int SIGNAL_A = 10;
    public static final int SIGNAL_F = 11;
    public static final int SIGNAL_P1 = 12;
    public static final int SIGNAL_P3 = 13;
    public static final int SIGNAL_N2 = 14;
    public static final int SIGNAL_N3 = 15;
    
    // Neu
    public static final int BLZ = -1;
    public static final int WHT = -2;
    public static final int AZGRT = -3;
    
    // Ausgänge
    public static final int NC = -1;
    
    // Signal A
    public static final int SigAfahrt = 24;
    public static final int SigAhalt = 25;
    public static final int VSigAfahrt = 75;
    public static final int VSigAhalt = 76;
    public static final int SigAwegRot = 44;
    public static final int SigAwegWeiss = 45;
    public static final int SigAersatz = 53;
            
    // Signal F
    public static final int SigFfahrt = 36;
    public static final int SigFhalt = 39;
    public static final int VSigFfahrt = 73;
    public static final int VSigFhalt = 74;
    public static final int SigFwegRot = 42;
    public static final int SigFwegWeiss = 43;
    public static final int SigFersatz = 50;
    
    // Signal P1
    public static final int SigP1fahrt = 38;
    public static final int SigP1halt = 37;
    public static final int SigP1Rangier = 64;
    public static final int SigP1Ersatz = 49;
            
    // Signal P3
    public static final int SigP3fahrt = 34;
    public static final int SigP3halt = 35;
    public static final int SigP3Rangier = 79;
    public static final int SigP3Ersatz = 48;
            
    // Signal N2
    public static final int SigN2fahrt = 32;
    public static final int SigN2halt = 33;
    public static final int SigN2Rangier = 78;
    public static final int SigN2Ersatz = 52;
            
    // Signal N3
    public static final int SigN3fahrt = 46;
    public static final int SigN3halt = 47;
    public static final int SigN3Rangier = 77;
    public static final int SigN3Ersatz = 51;
            
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
    
    // Störmelder
    public static final int WuTS = 68;
    public static final int WuTW = 67;
    
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
    public static final int SlFT1Relais = 121;
    public static final int SlFT4Relais = 122;
    
    public static final int Klingel = 66;
    public static final int TastenUeberwacher = 65;
    public static final int Wecker = 120;
    public static final int BLINK_STOERUNG = 107;
    public static final int ZSM = 106;
    
    // Relaisblock
    public static final int StreckeNachAH = 123;
    public static final int StreckeNachAH_VBHT = 124;
    public static final int StrWSP = 125;
    public static final int StreckeVonAH = 126;
    
    // Bahnhofsgleise
    public static final int Gleis1Weiss = 30;
    public static final int Gleis1Rot = 31;
    public static final int Gleis2Weiss = 28;
    public static final int Gleis2Rot = 29;
    public static final int Gleis3Weiss = 26;
    public static final int Gleis3Rot = 27;
    public static final int GleisAWeiss = 41;
    public static final int GleisARot = 40;
    public static final int GleisFWeiss = 55;
    public static final int GleisFRot = 54;
    
    // Weichen
    public static final int W3MinusRed = 4;
    public static final int W3PlusRed = 5;
    public static final int W3MinusWhite = 6;
    public static final int W3PlusWhite = 7;
    public static final int W4MinusRed = 0;
    public static final int W4PlusRed = 1;
    public static final int W4MinusWhite = 2;
    public static final int W4PlusWhite = 3;
    public static final int W5MinusRed = 12;
    public static final int W5PlusRed = 13;
    public static final int W5MinusWhite = 14;
    public static final int W5PlusWhite = 15;
    public static final int W18MinusRed = 8;
    public static final int W18PlusRed = 9;
    public static final int W18MinusWhite = 10;
    public static final int W18PlusWhite = 11;
    public static final int W19MinusRed = 20;
    public static final int W19PlusRed = 21;
    public static final int W19MinusWhite = 22;
    public static final int W19PlusWhite = 23;
    public static final int W20MinusRed = 16;
    public static final int W20PlusRed = 17;
    public static final int W20MinusWhite = 18;
    public static final int W20PlusWhite = 19;
    
}
