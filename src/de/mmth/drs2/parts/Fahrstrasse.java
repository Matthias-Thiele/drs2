/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.mmth.drs2.parts;

import de.mmth.drs2.Config;

/**
 *
 * @author pi
 */
public class Fahrstrasse implements TastenEvent{

    private Config config;
    private Weiche[] plusWeichen;
    private Weiche[] minusWeichen;
    private Doppeltaster taster;
    
    private boolean isLocked = false;
    private int gleisLampe;
    
    public void init(Config config, int[] plusWeichen, int[] minusWeichen, int signalTaste, int gleisTaste, int gleisLampe) {
        this.config = config;
        
        this.plusWeichen = new Weiche[plusWeichen.length];
        for (int i = 0; i < plusWeichen.length; i++) {
            this.plusWeichen[i] = config.weichen[plusWeichen[i]];    
        }
        
        this.minusWeichen = new Weiche[minusWeichen.length];
        for (int i = 0; i < minusWeichen.length; i++) {
            this.minusWeichen[i] = config.weichen[minusWeichen[i]];
        }
        
        taster = new Doppeltaster();
        taster.init(config, this, signalTaste, gleisTaste);
        
        this.gleisLampe = gleisLampe;
    }

    @Override
    public void whenPressed() {
        if (isLocked) {
            config.alert("Die Fahrstrasse ist bereits verrigelt.");
            return;
        }
        
        // Weichenstellung prüfen.
        for (Weiche plusWeiche : plusWeichen) {
            if (!plusWeiche.isPlus()) {
                config.alert("Die Weiche " + plusWeiche.getName() + " ist nicht in Plus Stellung.");
                return;
            }
            if (!plusWeiche.isRunning()) {
                config.alert("Die Weiche " + plusWeiche.getName() + " ist gestört oder läuft noch um.");
                return;
            }
        }
        
        for (Weiche minusWeiche : minusWeichen) {
            if (minusWeiche.isPlus()) {
                config.alert("Die Weiche " + minusWeiche.getName() + " ist nicht in Minus Stellung.");
                return;
            }
            if (!minusWeiche.isRunning()) {
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
        config.connector.setOut(gleisLampe, true);
    }
}
