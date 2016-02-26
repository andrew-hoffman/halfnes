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
public class MMC3Mapper extends Mapper {

    protected int whichbank = 0;
    protected boolean prgconfig = false;
    protected boolean chrconfig = false;
    protected int irqctrreload = 0;
    protected int irqctr = 0;
    protected boolean irqenable = false;
    protected boolean irqreload = false;
    protected int bank6 = 0;
    protected int[] chrreg = {0, 0, 0, 0, 0, 0};
    protected boolean interrupted = false;

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
        //cpuram.setPrgRAMEnable(false);
    }

    @Override
    public void cartWrite(int addr, int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        //bankswitches here
        //different register for even/odd writes
        //System.err.println("mmc3 write " + utils.hex(addr) + " " + utils.hex(data));
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
                        prg_map[i + 8] = (1024 * (i + (data * 8))) % prgsize;
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

    protected void setupchr() {
        if (chrconfig) {

            setppubank(1, 0, chrreg[2]);
            setppubank(1, 1, chrreg[3]);
            setppubank(1, 2, chrreg[4]);
            setppubank(1, 3, chrreg[5]);
            //Lowest bit of bank number IS IGNORED for the 2k banks
            setppubank(2, 4, (chrreg[0] >> 1) << 1);
            setppubank(2, 6, (chrreg[1] >> 1) << 1);

        } else {
            setppubank(1, 4, chrreg[2]);
            setppubank(1, 5, chrreg[3]);
            setppubank(1, 6, chrreg[4]);
            setppubank(1, 7, chrreg[5]);

            setppubank(2, 0, (chrreg[0] >> 1) << 1);
            setppubank(2, 2, (chrreg[1] >> 1) << 1);
        }
    }

    protected void setbank6() {
        if (!prgconfig) {
            //map c000-dfff to last bank, 8000-9fff to selected bank
            for (int i = 0; i < 8; ++i) {
                prg_map[i] = (1024 * (i + (bank6 * 8))) % prgsize;
                prg_map[i + 16] = ((prgsize - 16384) + 1024 * i);
            }
        } else {
            //map 8000-9fff to last bank, c000 to dfff to selected bank
            for (int i = 0; i < 8; ++i) {
                prg_map[i] = ((prgsize - 16384) + 1024 * i);
                prg_map[i + 16] = (1024 * (i + (bank6 * 8))) % prgsize;
            }
        }
    }

    private boolean lastA12 = false;

    @Override
    public int ppuRead(int addr) {
        //note: to pass blargg's mmc3 tests the vram address is read
        //in a loop while the PPU is not rendering
        //actually the read signal is not asserted then
        //but I have no other way to call into the mapper code when
        //the address changes.
        checkA12(addr);
        return super.ppuRead(addr);
    }

    @Override
    public void ppuWrite(int addr, int data) {
        checkA12(addr);
        super.ppuWrite(addr, data);
    }

    int a12timer = 0;

    @Override
    public void checkA12(int addr) {
        //run on every PPU cycle (wasteful...)
        //clocks scanline counter every time A12 line goes from low to high
        //on PPU address bus, _except_ when it has been less than 8 PPU cycles 
        //since the line last went low.
        boolean a12 = ((addr & (utils.BIT12)) != 0);
        if (a12 && (!lastA12)) {
            //rising edge
            if ((a12timer <= 0)) {
                clockScanCounter();
            }
        } else if (!a12 && lastA12) {
            //falling edge
            a12timer = 8;
        }

        --a12timer;
        lastA12 = a12;
    }

    private void clockScanCounter() {
        if (irqreload || (irqctr == 0)) {
            //System.err.println(ppu.scanline + "reloading" + irqctrreload);
            irqctr = irqctrreload;
            irqreload = false;
        } else {
            --irqctr;
        }
        if ((irqctr == 0) && irqenable && !interrupted) {
            ++cpu.interrupt;
            interrupted = true;
            //System.err.println("interrupt line " + ppu.scanline + " reload " + irqctrreload);
        }

    }

    protected void setppubank(int banksize, int bankpos, int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i)) % chrsize;
        }
    }
}
