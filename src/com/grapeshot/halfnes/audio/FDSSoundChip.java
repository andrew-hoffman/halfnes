/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.audio;

import com.grapeshot.halfnes.utils;

/**
 *
 * @author Andrew
 */
public class FDSSoundChip implements ExpansionSoundChip {
    //emulates the wavetable channel in the FDS 2C33 sound chip
    //(does anything ever read back from these registers? they aren't all write-only)

    //io enable: must be set for any other register to work
    boolean regEnable = true;
    //wavetable RAM (actually only 6 bits wide)
    int[] wavetable = new int[64];
    int waveAddr, waveOut, waveAccum;
    boolean waveWriteEnable; //holds channel at last output value when on and
    //allows access to wavetable RAM. does NOT stop wave unit. 

    //envelopes
    boolean volEnvDirection, volEnvEnable, modEnvDirection, modEnvEnable;
    int volEnvSpeed, modEnvSpeed, envClockMultiplier;
    //frequency (16 bits)
    int freq;
    //modulation
    boolean modEnable;
    int modCtr, //7 bits SIGNED +63 to -64
            modFreq; //12 bits
    int[] modTable = new int[64]; //3 bits wide
    int modTableAddr; //six bits, not settable from register
    //Volumes
    int masterVol, volGain, modGain;

    @Override
    public void clock(int cycles) {
        for (int i = 0; i < cycles; ++i) {
            //increment wave accumulator
            if (freq > 0 && !waveWriteEnable) {
                waveAccum += freq;
                if ((waveAccum & 0xffff) != waveAccum) {
                    //increment wave position on overflow
                    waveAccum &= 0xffff;
                    waveAddr = ++waveAddr & 63;
                }
            }
        }
        waveOut = wavetable[waveAddr];
    }

    @Override
    public void write(int register, int data) {
        System.err.println("R"+utils.hex(register)+ " D" + utils.hex(data));
        if (register == 0x4023) {
            //enable register, must be 1 for anything else to work
            regEnable = utils.getbit(data, 0);
        }
        if (regEnable) {
            if (register >= 0x4040 && register <= 0x407f) {
                //wavetable write
                if (waveWriteEnable) {
                    wavetable[(register - 0x4040) & 63] = (data & 63);
                }
            } else if (register == 0x4080) {
                //volume envelope enable and speed
                volEnvEnable = utils.getbit(data, 7); //ON when it's FALSE
                volEnvDirection = utils.getbit(data, 6);
                if (volEnvEnable) {
                    volGain = (data & 63);
                }
                volEnvSpeed = (data & 63);
            } else if (register == 0x4082) {
                //low 8 bits of wave frequency
                freq &= 0xf00;
                freq |= (data & 0xff);
            } else if (register == 0x4083) {
                //frequency high, wave reset and phase
                freq &= 0xff;
                freq |= (data & 0xf) << 8;

                //uh is it write 1 to enable or DISable here??
                //todo: enable
            } else if (register == 0x4084) {
                //modulator envelope enable and speed
                modEnvEnable = utils.getbit(data, 7);
                modEnvDirection = utils.getbit(data, 7);
                modEnvSpeed = data & 0x3f;
            } else if (register == 0x4085) {
                //set modulator counter directly
                modCtr = data & 0x7f;
            } else if (register == 0x4086) {
                //low 8 bits of mod freq
                modFreq &= 0xf00;
                modFreq |= (data & 0xff);
            } else if (register == 0x4087) {
                //high 4 bits of mod freq, reset and phase
                modFreq &= 0xff;
                modFreq |= (data & 0xf) << 4;
                //setting frequency to 0 disables modulation too
                //i think this is 1 to disable.
                modEnable = utils.getbit(data, 7);
            } else if (register == 0x4088) {
                //write data to 2 consecutive entries of modulator table
                if (modEnable) {
                    for (int i = 0; i < 2; ++i) {
                        modTable[modTableAddr] = data & 7;
                        modTableAddr = (modTableAddr + 1) & 63;
                    }
                }
            } else if (register == 0x4089) {
                //wave write protect and master vol
                masterVol = data & 3;
                waveWriteEnable = utils.getbit(data, 7);
            } else if (register == 0x408A) {
                //sets speed of volume and sweep envelopes
                //(or 0 to disable them)
                //normally 0xff
                envClockMultiplier = data;

            } else if (register == 0x4090) {
                //volume gain 
                volGain = (data & 63);
            } else if (register == 0x4092) {
                //modulator gain
                modGain = (data & 63);
            }
        }
    }

    @Override
    public int getval() {
        //todo: should be a lowpass on this
        return waveOut << 6;
    }

}
