/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.Const;
import de.mmth.drs2.TickerEvent;
import de.mmth.drs2.io.UartCommand;
import de.mmth.drs2.parts.state.StreckenState;

/**
 *
 * @author root
 */
public abstract class Strecke implements TastenEvent, TickerEvent {
    protected Config config;
    protected int streckeWeiss;
    protected int streckeRot;
    protected Doppeltaster streckeTaster;
    protected StreckenState streckenState;
    protected String name;
    protected int sperrRaeumungsmelder;
    protected boolean useMJ1MJ2;
    protected int rueckblockenUntil;
    protected boolean festlegemelder;
    protected Doppeltaster vbHT;
    protected int streckenTaste;
    protected int vorblockHilfsTaste;
    protected UartCommand blockCommand;
    protected int festlegemelderId;
    
    protected boolean sperrRaeummelder = false;
    private Doppeltaster ast;
    private Doppeltaster aslt;
    private Doppeltaster RbHG;
    
    /**
     * Die Initialisierung übergibt die Nummer der Streckenblock
     * Taste und die Nummern der weißen- und roten Pfeil-Lampe.
     * 
     * @param config
     * @param name
     * @param streckenTaste
     * @param streckeWeiss
     * @param streckeRot 
     * @param sperrRaeumungsmelder 
     * @param vorblockHilfsTaste 
     * @param festlegemelder 
     * @param blockCommand 
     */
    public void init(Config config, String name, 
            int streckenTaste, int streckeWeiss, int streckeRot, 
            int sperrRaeumungsmelder, int vorblockHilfsTaste,
            int festlegemelder, UartCommand blockCommand) {
        this.config = config;
        this.name = name;
        this.streckeWeiss = streckeWeiss;
        this.streckeRot = streckeRot;
        this.sperrRaeumungsmelder = sperrRaeumungsmelder;
        this.streckenTaste = streckenTaste;
        this.vorblockHilfsTaste = vorblockHilfsTaste;
        this.festlegemelderId = festlegemelder;
        this.blockCommand = blockCommand;
        
        this.streckeTaster = new Doppeltaster();
        this.streckeTaster.init(config, this, Const.BlGT, streckenTaste);
        config.ticker.add(streckeTaster);
        
        this.RbHG = new Doppeltaster();
        this.RbHG.init(config, this, Const.RbHGT, streckenTaste);
        config.ticker.add(RbHG);
        
        streckenState = StreckenState.FREE;
        markStrecke();
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
        config.ticker.add(aslt);
        
        config.ticker.add(this);
    }

    /**
     * Meldet zurück ob die Strecke frei ist.
     * @return 
     */
    public boolean isFree() {
        return streckenState == StreckenState.FREE;
    }
    
    /**
     * Meldet zurück, ob der Ausfahrtsperrmelder bzw. der Räumungsmelder aktiv ist.
     * @return 
     */
    public boolean isLocked() {
        return sperrRaeummelder;
    }
    
    /**
     * Setzt beim Vorblocken der Strecke die Wiederholsperre zurück.
     */
    public void unlock() {
        sperrRaeummelder = false;
        markStrecke();
    }
    
    /**
     * Mit der Fahrstraßenfestlegung wird der Festlegemelder aktiviert.
     */
    public void setFahrstrassenfestlegung() {
        festlegemelder = true;
        markStrecke();
    }
    
    /**
     * Setzt den Streckenpfeil auf Weiß oder Rot, je nachdem
     * ob die Strecke belegt ist.
     * 
     */
    protected void markStrecke() {
        boolean besetzt = streckenState != StreckenState.FREE;
        config.connector.setOut(streckeRot, besetzt);
        config.connector.setOut(streckeWeiss, !besetzt);
        config.connector.setOut(sperrRaeumungsmelder, sperrRaeummelder);
        config.connector.setOut(festlegemelderId, festlegemelder);
    }
    
    abstract public void fahrstrassenauflösung();
    abstract public void updateStreckenblock(boolean isInUse);
    abstract public void activateGleiskontakt(boolean mitErsatzsignal);
    abstract public void setWiederholsperre();
    abstract public void clearWiederholsperre();

    String getName() {
        return name;
    }

    void fahrstrasseFHT() {
        festlegemelder = false;
        streckenState = StreckenState.FREE;
        markStrecke();
    }
    
    public void trainArrived() {
        streckenState = StreckenState.TRAIN_ARRIVED;
    }
    
    public void tick(int count) {
        if (rueckblockenUntil == 0) {
            rueckblockenUntil = count + Const.KURBELINDUKTOR_RUNDEN;
            useMJ1MJ2 = !useMJ1MJ2;
            config.connector.setOut(useMJ1MJ2 ? Const.MJ1 : Const.MJ2, true);
            config.alert("Rückblocken " + name + " gestartet.");
        } else if (rueckblockenUntil != Integer.MAX_VALUE) {
            if (count > rueckblockenUntil) {
                config.connector.setOut(Const.MJ1, false);
                config.connector.setOut(Const.MJ2, false);
                config.alert("Rückblocken " + name + " beendet.");
                rueckblockenUntil = Integer.MAX_VALUE;
            }
        }
    }

}
