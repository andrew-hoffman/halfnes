/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.utils;
import com.grapeshot.halfnes.*;
import com.grapeshot.halfnes.audio.*;

/**
 *
 * @author Andrew
 */
public class FME7Mapper extends Mapper {

    private int commandRegister = 0;
    private int soundCommand = 0;
    private int[] charbanks = new int[8]; //8 1k char rom banks
    private int[] prgbanks = new int[4]; //4 8k prg banks - PLUS 1 8k fixed one
    private boolean ramEnable = true;
    private boolean ramSelect = false;
    private int irqcounter = 0xffff; //really needs to be unsigned but we'll cheese it
    private boolean irqenabled;
    private boolean irqclock;
    private boolean hasInitSound = false;
    private final ExpansionSoundChip sndchip = new Sunsoft5BSoundChip();
    private boolean interrupted = false;

    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        //on startup:
        prg_map = new int[40]; //(trollface)

        //fixed bank maps to last 8k of rom, set everything else to last chunk
        //as well.
        for (int i = 1; i <= 40; ++i) {
            prg_map[40 - i] = prgsize - (1024 * i);
        }

        for (int i = 0; i < 8; ++i) {
            chr_map[i] = 0;
        }
    }

    @Override
    public final int cartRead(int addr) {
        //five possible rom banks.
        if (addr >= 0x6000) {
            if (addr < 0x8000 && ramSelect) {
                if (ramEnable) {
                    return prgram[addr - 0x6000];
                } else {
                    return addr >> 8; //open bus
                }
            }
            return prg[prg_map[(addr - 0x6000) >> 10] + (addr & 1023)];
        }
        return addr >> 8; //open bus
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        if (addr == 0x8000) {
            //command register
            commandRegister = data & 0xf;
        } else if (addr == 0xc000) {
            //sound command register
            soundCommand = data & 0xf;
            if (!hasInitSound) {
                //only initialize the sound chip if anything writes a sound command.
                cpuram.apu.addExpnSound(sndchip);
                hasInitSound = true;
            }
        } else if (addr == 0xa000) {
            //mapper data register
            switch (commandRegister) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    //char bank switches
                    charbanks[commandRegister] = data;
                    setbanks();
                    break;
                case 8:
                    ramEnable = ((data & (utils.BIT7)) != 0);
                    ramSelect = ((data & (utils.BIT6)) != 0);
                    prgbanks[0] = data & 0x3f;
                    setbanks();
                    break;
                case 9:
                case 0xa:
                case 0xb:
                    //prg bank switch
                    prgbanks[commandRegister - 8] = data;
                    setbanks();
                    break;
                case 0xc:
                    //mirroring select
                    switch (data & 3) {
                        case 0:
                            setmirroring(Mapper.MirrorType.V_MIRROR);
                            break;
                        case 1:
                            setmirroring(Mapper.MirrorType.H_MIRROR);
                            break;
                        case 2:
                            setmirroring(Mapper.MirrorType.SS_MIRROR0);
                            break;
                        case 3:
                            setmirroring(Mapper.MirrorType.SS_MIRROR1);
                            break;
                    }
                case 0xd:
                    //irq - let's put this in and hope it works
                    irqclock = ((data & (utils.BIT7)) != 0);
                    //2015-05: test by Teppples says that any value written here
                    //will acknowledge a pending interrupt

                    irqenabled = ((data & (utils.BIT0)) != 0);

                    if (interrupted && cpu.interrupt > 0) {
                        --cpu.interrupt;
                    }
                    interrupted = false;
                    //System.err.println(cpu.interrupt);
                    break;
                case 0xe:
                    irqcounter &= 0xff00;
                    irqcounter |= data;
                    break;
                case 0xf:
                    irqcounter &= 0xff;
                    irqcounter |= (data << 8);
                    break;
            }
        } else if (addr == 0xe000) {
            sndchip.write(soundCommand, data);
        }

    }

    @Override
    public void cpucycle(int cycles) {
        if (irqclock) {
            if (irqcounter == 0) {
                irqcounter = 0xffff;
                if (irqenabled && !interrupted) {
                    interrupted = true;
                    ++cpu.interrupt;
                    //System.err.println("FME7 Interrupt");
                }
            } else {
                --irqcounter;
            }
        }
    }

    private void setbanks() {
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 4; ++j) {
                prg_map[i + 8 * j] = (1024 * (i + (prgbanks[j] * 8))) % prgsize;
            }
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * charbanks[i]) % chrsize;
        }
    }
}
