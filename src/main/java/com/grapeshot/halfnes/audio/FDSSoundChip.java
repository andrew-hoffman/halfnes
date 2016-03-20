/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
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
    int waveAddr, waveOut,
            waveAccum; //16 bits
    boolean waveWriteEnable; //holds channel at last output value when on and
    //allows access to wavetable RAM. does NOT stop wave unit. 

    //envelopes
    boolean volEnvDirection, volEnvDisable, modEnvDirection, modEnvDisable;
    int volEnvSpeed, modEnvSpeed, envClockMultiplier = 0xe8;//bios sets it thus?
    int pitch; //12 bits
    //modulation
    boolean modDisable;
    int modCtr, //7 bits SIGNED +63 to -64
            modFreq, //12 bits
            modAccum;
    int[] modTable = new int[64]; //3 bits wide
    int modTableAddr; //six bits, not settable from register
    //Volumes
    int masterVol, volGain, modGain;

    //the lowpass
    int lpaccum;
    int modout;

    boolean BothEnvDisable, haltWaveAndReset;

    @Override
    public void clock(int cycles) {
        for (int i = 0; i < cycles; ++i) {
            runUnits();
        }
    }

    private void runUnits() {
        //increment wave accumulator
        if ((pitch + modout) > 0 && !haltWaveAndReset) {
            waveAccum += (pitch + modout);
            if ((waveAccum & 0xffff) != waveAccum) {
                //increment wave position on overflow
                waveAccum &= 0xffff;
                waveAddr = (waveAddr + 1) & 63;
            }
        }

        //increment modulator
        if (modFreq > 0 && !modDisable) {
            modAccum += modFreq;
            if ((modAccum & 0xffff) != modAccum) {
                //and when that overflows run the rest of the modulation stuff
                modAccum &= 0xffff;
                CalculateModulator();
            }
        } else if (modDisable) {
            modAccum = 0;
            modout = 0;
        }
        if (!haltWaveAndReset && !BothEnvDisable && (envClockMultiplier != 0)) {
            CalculateEnvelopes();
        }
        if (!waveWriteEnable) {
            waveOut = wavetable[waveAddr];
        }
        int tmp = (volGain > 32) ? 32 : volGain;
        int out = (waveOut * tmp);
        //apply master volume attenuator
        switch (masterVol) {
            case 0:
            default:
                out *= 8;
                break;
            case 1:
                out *= 5;
                break;
            case 2:
                out *= 4;
                break;
            case 3:
                out *= 3;
                break;
        }
        //do a little lowpass (about 2khz)

        out += lpaccum;
        lpaccum -= out >> 6;
    }

    private void CalculateModulator() {
        switch (modTable[modTableAddr]) {
            case 0:
            default:
                modCtr += 0;
                break;
            case 1:
                modCtr += 1;
                break;
            case 2:
                modCtr += 2;
                break;
            case 3:
                modCtr += 4;
                break;
            case 4:
                modCtr = 0;
                break;
            case 5:
                modCtr -= 4;
                break;
            case 6:
                modCtr -= 2;
                break;
            case 7:
                modCtr -= 1;
                break;
        }
        modTableAddr = ++modTableAddr & 63;
        //wrap mod counter to 7 bits signed again
        modCtr = (modCtr << 25) >> 25;

        //apply modulator result (code pretty much from nesdev wiki)
        //thanks rainwarrior (thrainwarrior)
        // pitch   = $4082/4083 (12-bit unsigned pitch value)
        // counter = $4085 (7-bit signed mod counter)
        // gain    = $4084 (6-bit unsigned mod gain)
        // 1. multiply counter by gain, lose lowest 4 bits of result but "round" in a strange way
        int temp = modCtr * modGain;
        int remainder = temp & 0xF;
        temp >>= 4;
        if ((remainder > 0) && ((temp & 0x80) == 0)) {
            if (modCtr < 0) {
                temp -= 1;
            } else {
                temp += 2;
            }
        }
        // 2. wrap if a certain range is exceeded
        if (temp >= 192) {
            temp -= 256;
        } else if (temp < -64) {
            temp += 256;
        }
        // 3. multiply result by pitch, then round to nearest while dropping 6 bits
        temp = pitch * temp;
        remainder = temp & 0x3F;
        temp >>= 6;
        if (remainder >= 32) {
            temp += 1;
        }
        // final mod result is in temp
        modout = temp;
    }

    int modEnvAccum, volEnvAccum;

    private void CalculateEnvelopes() {
        if (!modEnvDisable) {
            ++modEnvAccum;
            if (modEnvAccum > (8 * envClockMultiplier * (modEnvSpeed + 1))) {
                modEnvAccum = 0;
                if (modEnvDirection) {
                    //increase
                    if (modGain < 32) {
                        ++modGain;
                    }
                } else {
                    //decrease
                    if (modGain > 0) {
                        --modGain;
                    }
                }
            }
        }

        if (!volEnvDisable) {
            ++volEnvAccum;
            if (volEnvAccum > (8 * envClockMultiplier * (volEnvSpeed + 1))) {
                volEnvAccum = 0;
                if (volEnvDirection) {
                    //increase
                    if (volGain < 32) {
                        ++volGain;
                    }

                } else {
                    //decrease
                    if (volGain > 0) {
                        --volGain;
                    }
                }
            }

        }
    }

    @Override
    public void write(int register, int data) {
        if (register == 0x4023) {
            //enable register, must be 1 for anything else to work
            regEnable = ((data & (utils.BIT0)) != 0);
        }
        if (regEnable) {
            if (register >= 0x4040 && register <= 0x407f) {
                //wavetable write
                if (waveWriteEnable) {
                    wavetable[(register - 0x4040) & 63] = (data & 63);
                }
            } else if (register == 0x4080) {
                //volume envelope enable and speed
                volEnvDisable = ((data & (utils.BIT7)) != 0); //ON when it's FALSE
                volEnvDirection = ((data & (utils.BIT6)) != 0);
                if (volEnvDisable) {
                    volGain = (data & 63);
                }
                volEnvSpeed = (data & 63);
                volEnvAccum = 0;
            } else if (register == 0x4082) {
                //low 8 bits of wave frequency
                pitch &= 0xf00;
                pitch |= (data & 0xff);
            } else if (register == 0x4083) {
                //frequency high, wave reset and phase
                pitch &= 0xff;
                pitch |= (data & 0xf) << 8;
                haltWaveAndReset = ((data & (utils.BIT7)) != 0);
                if (haltWaveAndReset) {
                    waveAccum = 0;
                    waveAddr = 0;
                }
                //uh is it write 1 to enable or DISable here??
                //todo: do something with envelope enables bit 6
                BothEnvDisable = ((data & (utils.BIT6)) != 0);
            } else if (register == 0x4084) {
                //modulator envelope enable and speed
                modEnvDisable = ((data & (utils.BIT7)) != 0);
                modEnvDirection = ((data & (utils.BIT6)) != 0);
                if (modEnvDisable) {
                    modGain = data & 0x3f;
                }
                modEnvSpeed = data & 0x3f;
                modAccum = 0;
                modEnvAccum = 0;
            } else if (register == 0x4085) {
                //set modulator counter directly
                //Bio Miracle Bokutte Opa uses this, requires very tight timing
                //sign extend    
                modCtr = ((data & 0x7f) << 25) >> 25;
                //modTableAddr = 0;
            } else if (register == 0x4086) {
                //low 8 bits of mod freq
                modFreq &= 0xf00;
                modFreq |= (data & 0xff);
            } else if (register == 0x4087) {
                //high 4 bits of mod freq, reset and phase
                modFreq &= 0xff;
                modFreq |= (data & 0xf) << 8;
                //setting frequency to 0 disables modulation too
                //i think this is 1 to disable.
                modDisable = ((data & (utils.BIT7)) != 0);
            } else if (register == 0x4088) {
                //write data to 2 consecutive entries of modulator table
                if (modDisable) {
                    for (int i = 0; i < 2; ++i) {
                        modTable[modTableAddr] = data & 7;
                        modTableAddr = (modTableAddr + 1) & 63;
                    }
                }
                modAccum = 0; //?
            } else if (register == 0x4089) {
                //wave write protect and master vol
                masterVol = data & 3;
                waveWriteEnable = ((data & (utils.BIT7)) != 0);
            } else if (register == 0x408A) {
                //sets speed of volume and sweep envelopes
                //(or 0 to disable them)
                //normally 0xff
                envClockMultiplier = data;

            }
        }
    }

    public int read(int register) {
        if ((register >= 0x4040) && (register < 0x4080)) {
            return wavetable[register - 0x4040] | 0x40;
        } else if (register == 0x4090) {
            //volume gain 
            return volGain;
        } else if (register == 0x4092) {
            //modulator gain
            return modGain;
        } else {
            //System.err.println("what goes here " + utils.hex(register));
            //why are nsfs reading from 4080 and 4084? there's nothing there
            return 0x40;
        }
    }

    @Override
    public int getval() {
        return lpaccum;
    }

}
