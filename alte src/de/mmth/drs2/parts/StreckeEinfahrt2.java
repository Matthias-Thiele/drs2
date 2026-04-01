/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.parts.state.StreckenState;

/**
 *
 * @author matthias
 */
public class StreckeEinfahrt2 implements TastenEvent, TickerEvent {
  private static final int RÜCKBLOCK_RELAIS_COUNT = -20;
  private static final int RÜCKBLOCK_SIMULATION_COUNT = -60;
  
  private Config config;
  private int blockMelderWeiss;
  private int blockMelderRot;
  private Doppeltaster rückblockTaster;
  private StreckenState streckenState;
  private String name;
  private int räumungsmelderId;
  private boolean räummelderState;
  private boolean räummelderDauerlicht;
  private boolean simulationMode = false;
  private Doppeltaster ast;
  private Doppeltaster aslt;
  private Doppeltaster RbHG;

  private int signalId;
  private int blockStatePort;
  private int rückblockPort;
  private int rückblockCount = Integer.MAX_VALUE;
  private boolean simulatedBlockState;
  private int festlegemelderId;
  private boolean festlegemelderState = false;
  private int zsmCount = Integer.MAX_VALUE;
  private boolean allowAsLT;
    
  /**
   * Die Initialisierung übergibt die Nummer der Streckenblock
   * Taste und die Nummern der weißen- und roten Pfeil-Lampe.
   * 
   * @param config
   * @param name
   * @param rueckblockTaste
   * @param blockMelderWeiss
   * @param blockMelderRot 
   * @param raeumungsmelderId 
   * @param festlegemelderId 
   * @param rueckblockPort 
   * @param blockStatePort 
   * @param signalId 
   */
  public void init(Config config, String name, 
        int rueckblockTaste, int blockMelderWeiss, int blockMelderRot, 
        int raeumungsmelderId, int festlegemelderId, 
        int rueckblockPort, int blockStatePort, int signalId) {
      this.config = config;
      this.name = name;
      this.blockMelderWeiss = blockMelderWeiss;
      this.blockMelderRot = blockMelderRot;
      this.räumungsmelderId = raeumungsmelderId;
      this.festlegemelderId = festlegemelderId;
      this.rückblockPort = rueckblockPort;
      this.blockStatePort = blockStatePort;
      this.signalId = signalId;

      this.rückblockTaster = new Doppeltaster();
      this.rückblockTaster.init(config, this, Const.BlGT, rueckblockTaste);
      config.ticker.add(rückblockTaster);

      this.RbHG = new Doppeltaster();
      this.RbHG.init(config, this, Const.RbHGT, rueckblockTaste);
      config.ticker.add(RbHG);

      streckenState = StreckenState.FREE;
      updateView();

      ast = new Doppeltaster();
      ast.init(config, this, Const.AsT, rueckblockTaste, true);
      config.ticker.add(ast);

      aslt = new Doppeltaster();
      aslt.init(config, this, Const.AsLT, rueckblockTaste, true);
      config.ticker.add(aslt);


      config.ticker.add(this);
  }

  public String getName() {
    return name;
  }
  
  /**
   * Stellt ein, ob sich der Block im Simulationsmodus befindet
   * oder mit dem Relaisblock verbunden ist.
   * 
   * @param simulation 
   */
  public void setSimulationMode(boolean simulation) {
    simulationMode = simulation;
  }
  
  public boolean tryVorblock() {
    if (simulationMode && !räummelderState) {
      simulatedBlockState = true;
      return true;
    }
    
    return false;
  }
  
  public void doFHT() {
    
  }
  
  /**
   * Meldet zurück ob die Strecke frei ist.
   * @return 
   */
  public boolean isFree() {
    return streckenState == StreckenState.FREE;
  }
  
  /**
   * Mit der Fahrstraßenfestlegung wird der Festlegemelder aktiviert.
   */
  public void setFahrstrassenfestlegung() {
      festlegemelderState = true;
      räummelderDauerlicht = true;
      updateView();
  }
  
  /**
   * Mit der Fahrstraßenauflösung wird der Melder zurückgesetzt.
   */
  public void fahrstrassenauflösung() {
    festlegemelderState = false;
    updateView();
  }

  public void trainArrived() {
    //???
  }
  
  /**
   * Der einfahrende Zug macht was?. 
   * 
   * @param mitErsatzsignal
   */
  public void activateGleiskontakt(boolean mitErsatzsignal) {
      räummelderDauerlicht = false;
      räummelderState = true;
      if (signalId == 0) {
        // ZSM nur von der Signal A Seite aus aktivieren.
        zsmCount = -30;
      }
  }
  
  /**
   * Zug hat den Räumungsabschnitt verlassen
   */
  public void streckeGeraeumt() {
    räummelderState = false;
  }
  
  /**
   * Aktualisiert die Lampeneinstellung gemäß des aktuellen Zustands
   */
  private void updateView() {
    boolean besetzt = streckenState != StreckenState.FREE;
    config.connector.setOut(blockMelderRot, besetzt);
    config.connector.setOut(blockMelderWeiss, !besetzt);
    config.connector.setOut(räumungsmelderId, räummelderState & (räummelderDauerlicht || config.blinklicht.getBlink()));
    config.connector.setOut(festlegemelderId, festlegemelderState);
  }
  
  private boolean isFahrt() {
    return config.signale[signalId].isFahrt();
  }
  
  private boolean getBlockState() {
    if (simulationMode) {
      return simulatedBlockState;
    } else {
      return config.connector.isInSet(blockStatePort);
    }
  }
  
  @Override
  public void whenPressed(int taste1, int taste2) {
    switch (taste1) {
      case Const.BlGT:
        // Zug ist eingefahren, Strecke wird zurückgeblockt.
        if (isFahrt()) {
          config.alert("Zug noch nicht eingefahren, Signal noch auf Fahrt.");
        } else {
          startRückblock();
        } break;
      case Const.RbHGT:
        if (streckenState != StreckenState.FREE) {
          if (config.ersatzsignale[signalId].isFahrt() || config.signale[signalId].isFahrt() || config.signale[signalId].isSh1()) {
            config.alert("Signal oder Ersatzsignal noch auf Fahrt.");
          } else {
            startRückblock();
          }
        }
        break;
      case Const.AsT:
        räummelderDauerlicht = true;
        räummelderState = true;
        allowAsLT = true;
        break;
      case Const.AsLT:
        if (allowAsLT) {
          allowAsLT = false;
          räummelderDauerlicht = false;
          räummelderState = false;        }
        break;
      default:
        break;
    }
  }

  /**
   * Veranlasst ein Rückblocken.
   */
  public void startRückblock() {
    rückblockCount = simulationMode ? RÜCKBLOCK_SIMULATION_COUNT : RÜCKBLOCK_RELAIS_COUNT;
  }
  
  /**
   * Prüft nach, ob es einen aktiven Rückblockvorgang gibt und führt ihn aus.
   * @param count 
   */
  private void tickRueckblock(int count) {
    if (rückblockCount < 0) {
      // Rückblocken starten
      if (!simulationMode) {
        // Relaisblock anweisen das Rückblocken zu starten
        config.connector.setOut(rückblockPort, true);
      }
      
      rückblockCount = count - rückblockCount;
    } else if (rückblockCount < count) {
      // Ende der Rückblock-Zeit erreicht.
      räummelderState = false;
      if (simulationMode) {
        simulatedBlockState = false;
      } else {
        // Rückblocken beendet
        config.connector.setOut(rückblockPort, false);
      }
      
      config.connector.setOut(Const.ZSM, false);
      rückblockCount = Integer.MAX_VALUE;
      updateView();
    }
  }
  
  /**
   * Prüft nach, ob es einen aktiven Rückblockvorgang gibt und führt ihn aus.
   * @param count 
   */
  private void tickZSM(int count) {
    if (zsmCount < 0) {
      // Wartezeit bis ZSM aufleuchtet starten
      zsmCount = count - zsmCount;
    } else if (zsmCount < count) {
      // Ende der Wartezeit erreicht.
      config.connector.setOut(Const.ZSM, true);
      zsmCount = Integer.MAX_VALUE;
    }
  }
  
  /**
   * Prüft nach, ob sich der Zustand des Streckenblocks verändert hat.
   * @param count 
   */
  private void checkStreckenState() {
    var newStreckenState = getBlockState() ? StreckenState.WAIT_FOR_TRAIN : StreckenState.FREE;
    if (!newStreckenState.equals(streckenState)) {
      if (newStreckenState.equals(StreckenState.WAIT_FOR_TRAIN)) {
        // Block hat von Frei nach Besetzt gewechselt
        if (!isFahrt()) {
          config.stoerungsmelder.meldung();
        }
      }
      streckenState = newStreckenState;
      updateView();
    }
  }
  
  @Override
  public void tick(int count) {
    tickRueckblock(count);
    tickZSM(count);
    checkStreckenState();
    if (räummelderState) {
      updateView();
    }
  }

  
}
