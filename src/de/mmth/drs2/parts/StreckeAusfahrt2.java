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
public class StreckeAusfahrt2 implements TastenEvent, TickerEvent {
  private static final int VORBLOCK_RELAIS_COUNT = -20;
  private static final int VORBLOCK_SIMULATION_COUNT = -60;
  private static final int RÜCKBLOCK_SIMULATION_COUNT = -200;
  
  private static final int BLINK_WH_DURATION = 50;
  private static final int AUTO_RB_DELAY = 200;

  protected Config config;
  protected int blockMelderWeiss;
  protected int blockMelderRot;
  protected Doppeltaster streckeTaster;
  protected StreckenState streckenState;
  protected String name;
  protected int sperrmelderId;
  protected boolean useMJ1MJ2;
  protected int rueckblockenUntil;
  protected boolean festlegemelderState;
  protected Doppeltaster vbHT;
  protected int streckenTaste;
  protected int vorblockHilfsTaste;
  protected int festlegemelderId;
  protected boolean simulationMode = false;
  protected boolean isInbound;
  protected boolean sperrmelderState = false;
  private Doppeltaster ast;
  private Doppeltaster aslt;
  private Doppeltaster RbHG;

  protected int vorblockPort;
  protected int releaseBlockPort = Integer.MAX_VALUE;
  protected int signalId;

  private int endBlinkWHSperre = Integer.MAX_VALUE;
  private int startAutoRueckblock = Integer.MAX_VALUE;
  private int vorblockCount = Integer.MAX_VALUE;
  private int rückblockCount = Integer.MAX_VALUE;
  private boolean simulatedBlockState;
  private int blockStatePort;

    /**
     * Die Initialisierung übergibt die Nummer der Streckenblock
     * Taste und die Nummern der weißen- und roten Pfeil-Lampe.
     * 
     * @param config
     * @param name
     * @param streckenTaste
     * @param blockMelderWeiss
     * @param blockMelderRot 
     * @param sperrmelderId 
     * @param vorblockHilfsTaste 
     * @param festlegemelder 
     * @param vorblockPort 
     * @param blockStatePort 
     */
    public void init(Config config, String name, 
            int streckenTaste, int blockMelderWeiss, int blockMelderRot, 
            int sperrmelderId, int vorblockHilfsTaste,
            int festlegemelder, int vorblockPort, int blockStatePort) {
        this.config = config;
        this.name = name;
        this.blockMelderWeiss = blockMelderWeiss;
        this.blockMelderRot = blockMelderRot;
        this.sperrmelderId = sperrmelderId;
        this.streckenTaste = streckenTaste;
        this.vorblockHilfsTaste = vorblockHilfsTaste;
        this.festlegemelderId = festlegemelder;
        this.vorblockPort = vorblockPort;
        this.blockStatePort = blockStatePort;
        
        this.streckeTaster = new Doppeltaster();
        this.streckeTaster.init(config, this, Const.BlGT, streckenTaste);
        config.ticker.add(streckeTaster);
        
        this.RbHG = new Doppeltaster();
        this.RbHG.init(config, this, Const.RbHGT, streckenTaste);
        config.ticker.add(RbHG);
        
        streckenState = StreckenState.FREE;
        updateView();
        rueckblockenUntil = Integer.MAX_VALUE;
        
        if (vorblockHilfsTaste >= 0) {
            vbHT = new Doppeltaster();
            vbHT.init(config, this,Const.BlGT, vorblockHilfsTaste);
            config.ticker.add(vbHT);                
        }
        
        ast = new Doppeltaster();
        ast.init(config, this, Const.AsT, streckenTaste, true);
        config.ticker.add(ast);
        
        aslt = new Doppeltaster();
        aslt.init(config, this, Const.AsLT, streckenTaste, true);
        
        useMJ1MJ2 = (streckenTaste == Const.BlockHIn) || (streckenTaste == Const.BlockHOut);
        config.ticker.add(aslt);
        
        config.ticker.add(this);
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
  
  public String getName() {
    return name;
  }
  
  /**
   * Zug ist auf der Strecke, sie wird automatisch vorgeblockt.
   */
  public void startVorblock() {
    vorblockCount = simulationMode ? VORBLOCK_SIMULATION_COUNT : VORBLOCK_RELAIS_COUNT;
  }
  
  public boolean tryVorblock() {
    if (simulationMode && sperrmelderState) {
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
   * Meldet zurück, ob der Ausfahrtsperrmelder aktiv ist.
   * @return 
   */
  public boolean isLocked() {
    return sperrmelderState;
  }
  
  /**
   * Mit der Fahrstraßenfestlegung wird der Festlegemelder aktiviert.
   */
  public void setFahrstrassenfestlegung() {
    festlegemelderState = true;
    updateView();
  }
  
  /**
   * Mit der Fahrstraßenauflösung wird der Melder blinkend und
   * durch das Rückblocken wird er zurückgesetzt.
   */
  public void fahrstrassenauflösung() {
    festlegemelderState = false;
    updateView();
  }

  private boolean getBlockState() {
    if (simulationMode) {
      return simulatedBlockState;
    } else {
      return config.connector.isInSet(blockStatePort);
    }
  }
  
  /**
   * Prüft nach, ob sich der Zustand des Streckenblocks verändert hat.
   * @param count 
   */
  private void checkStreckenState() {
    var newStreckenState = getBlockState() ? StreckenState.WAIT_FOR_TRAIN : StreckenState.FREE;
    if (!newStreckenState.equals(streckenState)) {
      if (newStreckenState.equals(StreckenState.FREE)) {
        // Block hat von Besetzt nach Frei gewechselt
        config.stoerungsmelder.meldung();
      }
      streckenState = newStreckenState;
      updateView();
    }
  }
  
  /**
   * Prüft nach, ob es einen aktiven Blockvorgang gibt und führt ihn aus.
   * @param count 
   */
  private void tickVorblock(int count) {
    if (vorblockCount < 0) {
      // Vorblocken starten
      if (!simulationMode) {
        // Relaisblock anweisen das Vorblocken zu starten
        config.connector.setOut(vorblockPort, true);
      }
      
      vorblockCount = count - vorblockCount;
    } else if (vorblockCount < count) {
      // Ende der Vorblock-Zeit erreicht.
      if (simulationMode) {
        simulatedBlockState = true;
        rückblockCount = RÜCKBLOCK_SIMULATION_COUNT;
      } else {
        // Vorblocken beendet
        config.connector.setOut(vorblockPort, false);
      }
      
      sperrmelderState = false;
      vorblockCount = Integer.MAX_VALUE;
      updateView();
    } else if (vorblockCount < Integer.MAX_VALUE) {
      config.connector.setOut(sperrmelderId, sperrmelderState & config.blinklicht.getBlink());
    }
  }
  
  private void tickRückblock(int count) {
    if (rückblockCount < 0) {
      rückblockCount = count - rückblockCount;
    } else if (rückblockCount < count) {
      simulatedBlockState = false;
      rückblockCount = Integer.MAX_VALUE;
      updateView();
    }
  }
    /**
     * Wenn das Signal hinter dem ausfahrenden Zug auf Halt fällt,
     * wird die Wiederholsperre gesetzt.
     */
    public void setWiederholsperre() {
        System.out.println("Wiederholsperre setzen.");
        sperrmelderState = true;
        updateView();
    }
    
    public void clearWiederholsperre() {
        System.out.println("Wiederholsperre löschen.");
        // ???pendingClear = true;
        updateView();
    }
    
  /**
   * Der ausfahrende Zug blockt automatisch vor und löst die Wiederholsperre. 
   * 
   * @param mitErsatzsignal
   */
  public void activateGleiskontakt(boolean mitErsatzsignal) {
      if (!mitErsatzsignal) {
        startVorblock();
      }

      clearWiederholsperre();
  }
  
  /**
   * Aktualisiert die Lampeneinstellung gemäß des aktuellen Zustands
   */
  private void updateView() {
    boolean besetzt = streckenState != StreckenState.FREE;
    config.connector.setOut(blockMelderRot, besetzt);
    config.connector.setOut(blockMelderWeiss, !besetzt);
    config.connector.setOut(sperrmelderId, sperrmelderState);
    config.connector.setOut(festlegemelderId, festlegemelderState);
  }
  
    @Override
    public void whenPressed(int taste1, int taste2) {
        switch (taste1) {
            case Const.AsT:
                setWiederholsperre();
                break;
                
            case Const.AsLT:
                sperrmelderState = false;
                updateView();
                break;
            
            case Const.BlGT:
                if (taste2 == vorblockHilfsTaste) {
                    if (sperrmelderState) {
                      config.alert("Wiederholsperre aktiv, manuelles Vorblocken nicht möglich.");
                      return;
                    }
                    // Bei der Ausfahrt über Hilfssignal muss manuell vorgeblockt werden.
                    startVorblock();
                    clearWiederholsperre();
                    rueckblockenUntil = 0;
                }
                break;
                
            default:
                
        }
    }

  @Override
  public void tick(int count) {
    tickVorblock(count);
    tickRückblock(count);
    checkStreckenState();
  }

}
