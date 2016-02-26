package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper182 extends Mapper {
    //Pirate MMC3 clone with scrambled registers

    private int whichbank = 0;
    private boolean prgconfig = false;
    private boolean chrconfig = false;
    private int irqctrreload = 0;
    private int irqctr = 0;
    private boolean irqenable = false;
    private boolean irqreload = false;
    private int bank6 = 0;
    private int[] chrreg = {0, 0, 0, 0, 0, 0};
    private boolean interrupted = false;

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
    public final void cartWrite(int addr, int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        //bankswitches here
        //different register for even/odd writes
        if (((addr & (utils.BIT0)) != 0)) {
            //odd registers
            if ((addr >= 0x8000) && (addr <= 0x9fff)) {
                //mirroring setup
                setmirroring(((data & (utils.BIT0)) != 0) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
            } else if ((addr >= 0xA000) && (addr <= 0xBFFF)) {
                //prg ram write protect
                //cpuram.setPrgRAMEnable(!utils.getbit(data, 7));
            } else if ((addr >= 0xC000) && (addr <= 0xDFFF)) {
                //any value here reloads irq counter _@ end of scanline_
                irqreload = true;
                irqctrreload = data;
            } else if ((addr >= 0xE000) && (addr <= 0xFFFF)) {
                //any value here enables interrupts
                irqenable = true;
            }
        } else {
            //even registers
            if ((addr >= 0xA000) && (addr <= 0xBFFF)) {
                //bank select
                whichbank = data & 7;
                prgconfig = ((data & (utils.BIT4)) != 0);
                //if bit is false, 8000-9fff swappable and c000-dfff fixed to 2nd to last bank
                //if bit is true, c000-dfff swappable and 8000-9fff fixed to 2nd to last bank
                chrconfig = ((data & (utils.BIT5)) != 0);
                //if false: 2 2k banks @ 0000-0fff, 4 1k banks in 1000-1fff
                //if true: 4 1k banks @ 0000-0fff, 2 2k banks @ 1000-1fff
                setupchr();
                setbank6(); //OOPS FORGOT THIS I GUESS
            } else if ((addr >= 0xC000) && (addr <= 0xDFFF)) {
                //bank select
                switch (whichbank) {
                    case 0:
                        chrreg[0] = data;
                        setupchr();
                        break;
                    case 1:
                        chrreg[3] = data;
                        setupchr();
                        break;
                    case 2:
                        chrreg[1] = data;
                        setupchr();
                        break;
                    case 3:
                        chrreg[5] = data;
                        setupchr();
                        break;
                    case 4:
                        bank6 = data;
                        setbank6();
                        break;
                    case 5:
                        //bank 5 always swappable, always in same place
                        for (int i = 0; i < 8; ++i) {
                            prg_map[i + 8] = (1024 * (i + (data * 8))) % prgsize;
                        }
                        break;
                    case 6:
                        chrreg[2] = data;
                        setupchr();
                        break;
                    case 7:
                        chrreg[4] = data;
                        setupchr();
                        break;
                }
            } else if ((addr >= 0xE000) && (addr <= 0xFFFF)) {
                //any value here disables IRQ and acknowledges
                if (interrupted) {
                    --cpu.interrupt;
                }
                interrupted = false;
                irqenable = false;
                irqctr = irqctrreload;
            }
        }
    }

    private void setupchr() {
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

    private void setbank6() {
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

    @Override
    public void notifyscanline(int scanline) {
        //Scanline counter
        if (scanline > 239 && scanline != 261) {
            //clocked on LAST line of vblank and all lines of frame. Not on 240.
            return;
        }
        if (!ppu.mmc3CounterClocking()) {
            return;
        }

        if (irqreload) {
            irqreload = false;
            irqctr = irqctrreload;
        }

        if (irqctr-- <= 0) {
            if (irqctrreload == 0) {
                return;
                //irqs stop being generated if reload set to zero
            }
            if (irqenable && !interrupted) {
                ++cpu.interrupt;
                interrupted = true;
            }
            irqctr = irqctrreload;
        }
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i)) % chrsize;
        }
    }
}
