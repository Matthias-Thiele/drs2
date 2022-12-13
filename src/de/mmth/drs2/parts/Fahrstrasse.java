/*
 * DRS2 Stellpultsteuerung für Raspberry Pi
 * (c) 2022 Matthias Thiele
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;
import de.mmth.drs2.io.Signal;

/**
 * Diese Klasse verwaltet die Funktionen zum
 * Festlegen, Auflösen und Betreiben einer
 * Fahrstraße.
 * 
 * @author pi
 */
public class Fahrstrasse implements TastenEvent{

    private Config config;
    private Weiche[] plusWeichen;
    private Weiche[] minusWeichen;
    private Doppeltaster taster;
    
    private boolean isLocked = false;
    private int gleisLampeWeiss;
    private String name;
    private Signal signal;
    private int gleisLampeRot;
    
    /**
     * Initialisiert die Parameter der Fahrstraße.
     * 
     * @param config
     * @param name
     * @param plusWeichen diese Weichen müssen in Plus Stellung stehen
     * @param minusWeichen diese Weichen müssen in Minus Stellung stehen
     * @param signalTaste
     * @param gleisTaste
     * @param gleisLampeWeiss diese Lampe zeigt an, dass die Fahrstraße aktiv ist.
     * @param signalNummer Ein- oder Ausfahrtsignal zu dieser Fahrstraße.
     */
    public void init(Config config, String name, int[] plusWeichen, int[] minusWeichen, int signalTaste, int gleisTaste, int gleisLampeWeiss, int gleisLampeRot, int signalNummer) {
        this.config = config;
        this.name = name;
        
        this.plusWeichen = new Weiche[plusWeichen.length];
        for (int i = 0; i < plusWeichen.length; i++) {
            this.plusWeichen[i] = config.weichen[plusWeichen[i]];    
        }
        
        this.minusWeichen = new Weiche[minusWeichen.length];
        for (int i = 0; i < minusWeichen.length; i++) {
            this.minusWeichen[i] = config.weichen[minusWeichen[i]];
        }
        
        taster = new Doppeltaster();
        if (signalTaste >= 0) {
            taster.init(config, this, signalTaste, gleisTaste);
        }
        
        this.gleisLampeWeiss = gleisLampeWeiss;
        this.gleisLampeRot = gleisLampeRot;
        this.signal = config.signale[signalNummer];
    }

    /**
     * Callback Funktion vom Doppeltaster.
     * Der Fahrdienstleiter hat die Signaltaste
     * und die Gleistaste betätigt. Das System
     * versucht nun die Fahrstraße einzurichten.
     */
    @Override
    public void whenPressed() {
        if (isLocked) {
            config.alert("Die Fahrstrasse " + name + " ist bereits verrigelt.");
            return;
        }
        
        // Weichenstellung prüfen.
        for (Weiche plusWeiche : plusWeichen) {
            if (!plusWeiche.isPlus()) {
                config.alert("Die Weiche " + plusWeiche.getName() + " ist nicht in Plus Stellung.");
                return;
            }
            if (plusWeiche.isRunning()) {
                config.alert("Die Weiche " + plusWeiche.getName() + " ist gestört oder läuft noch um.");
                return;
            }
        }
        
        for (Weiche minusWeiche : minusWeichen) {
            if (minusWeiche.isPlus()) {
                config.alert("Die Weiche " + minusWeiche.getName() + " ist nicht in Minus Stellung.");
                return;
            }
            if (minusWeiche.isRunning()) {
                config.alert("Die Weiche " + minusWeiche.getName() + " ist gestört oder läuft noch um.");
                return;
            }
        }
        
        // Weichen verrigeln
        for (Weiche plusWeiche : plusWeichen) {
            plusWeiche.lock();
        }

        for (Weiche minusWeiche : minusWeichen) {
            minusWeiche.lock();
        }
        
        isLocked = true;
        config.connector.setOut(gleisLampeWeiss, true);
        config.alert("Die Fahrstraße " + name + " wurde verschlossen.");
    }
    
    /**
     * Fahrstraße auflösen.
     * 
     * Dabei werden alle Weichensperren zurückgenommen
     * und die Anzeige DRS 2 aktualisiert.
     */
    public void unlock() {
        if (!isLocked) {
            config.alert("Die Fahrstraße ist nicht verschlossen.");
            return;
        }
        
        // Weichen entrigeln
        for (Weiche plusWeiche : plusWeichen) {
            plusWeiche.unlock();
        }

        for (Weiche minusWeiche : minusWeichen) {
            minusWeiche.unlock();
        }
        
        isLocked = false;
        config.connector.setOut(gleisLampeWeiss, false);
        config.alert("Die Fahrstraße " + name + " wurde aufgelöst.");
    }
    
    /**
     * Liefert den Namen der Fahrstraße zurück.
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Liefert zurück, ob die Fahrstraße verschlossen ist.
     * @return 
     */
    public boolean isLocked() {
        return isLocked;
    }
}
