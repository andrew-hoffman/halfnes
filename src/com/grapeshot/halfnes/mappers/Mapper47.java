/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

/**
 *
 * @author Andrew
 */
public class Mapper47 extends MMC3Mapper {

    //official Nintendo multicart mapper, mmc3 variant
    //used for super spike vball, nintendo world cup
    private int multibank = 1;

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 1; i <= 32; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }

        for (int i = 0; i < 8; ++i) {
            chr_map[i] = 0;
        }
        setbank6();
    }

    @Override
    public void cartWrite(int addr, int data) {
        if (addr < 0x6000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        //multicart bankswitches here
        if ((addr >= 0x6000) && (addr <= 0x7fff)) {
            multibank = data & 1;
            //setup all banks
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 8] = ((1024 * (i + (data * 8))) % 131072) + multibank * 131072;
            }
            setbank6();
            setupchr();
            for (int i = 1; i <= 8; ++i) {
                prg_map[32 - i] = 131072 - (1024 * i) + multibank * 131072;
            }
        }
        //different register for even/odd writes
        if (((addr & (utils.BIT0)) != 0)) {
            //odd registers
            if ((addr >= 0x8000) && (addr <= 0x9fff)) {
                //bank change
                //System.err.println("setting " + whichbank + " " + data + " " + prgconfig);
                if (whichbank <= 5) {
                    chrreg[whichbank] = data;
                    setupchr();
                } else if (whichbank == 6) {
                    bank6 = data;
                    setbank6();
                } else if (whichbank == 7) {
                    //bank 7 always swappable, always in same place
                    for (int i = 0; i < 8; ++i) {
                        prg_map[i + 8] = ((1024 * (i + (data * 8))) % 131072) + multibank * 131072;
                    }
                }
            } else if ((addr >= 0xA000) && (addr <= 0xbfff)) {
                //prg ram write protect
                //cpuram.setPrgRAMEnable(!utils.getbit(data, 7));
            } else if ((addr >= 0xc000) && (addr <= 0xdfff)) {
                //any value here reloads irq counter
                irqreload = true;

            } else if ((addr >= 0xe000) && (addr <= 0xffff)) {
                //iany value here enables interrupts
                irqenable = true;
            }
        } else {
            //even registers
            if ((addr >= 0x8000) && (addr <= 0x9fff)) {
                //bank select
                whichbank = data & 7;
                prgconfig = ((data & (utils.BIT6)) != 0);
                //if bit is false, 8000-9fff swappable and c000-dfff fixed to 2nd to last bank
                //if bit is true, c000-dfff swappable and 8000-9fff fixed to 2nd to last bank
                chrconfig = ((data & (utils.BIT7)) != 0);
                //if false: 2 2k banks @ 0000-0fff, 4 1k banks in 1000-1fff
                //if true: 4 1k banks @ 0000-0fff, 2 2k banks @ 1000-1fff
                setupchr();
                setbank6(); //OOPS FORGOT THIS I GUESS
            } else if ((addr >= 0xA000) && (addr <= 0xbfff)) {
                //mirroring setup
                if (scrolltype != MirrorType.FOUR_SCREEN_MIRROR) {
                    setmirroring(((data & (utils.BIT0)) != 0) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
                }
            } else if ((addr >= 0xc000) && (addr <= 0xdfff)) {
                //value written here used to reload irq counter _@ end of scanline_
                irqctrreload = data;
            } else if ((addr >= 0xe000) && (addr <= 0xffff)) {
                //any value here disables IRQ and acknowledges
                if (interrupted) {
                    --cpu.interrupt;
                }
                interrupted = false;
                irqenable = false;
            }
        }
    }

    protected void setbank6() {
        if (!prgconfig) {
            //map c000-dfff to last bank, 8000-9fff to selected bank
            for (int i = 0; i < 8; ++i) {
                prg_map[i] = ((1024 * (i + (bank6 * 8))) % 131072) + multibank * 131072;
                prg_map[i + 16] = ((131072 - 16384) + 1024 * (i + multibank * 128));
            }
        } else {
            //map 8000-9fff to last bank, c000 to dfff to selected bank
            for (int i = 0; i < 8; ++i) {
                prg_map[i] = ((131072 - 16384) + 1024 * (i + multibank * 128));
                prg_map[i + 16] = ((1024 * (i + (bank6 * 8))) % 131072) + multibank * 131072;
            }
        }
    }

    protected void setppubank(int banksize, int bankpos, int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = ((1024 * ((banknum) + i)) % (chrsize / 2)) + multibank * 131072;
        }
    }
}
