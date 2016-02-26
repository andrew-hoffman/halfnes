/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper48 extends Mapper {

    int prgbank0, prgbank1 = 0;
    int[] chrbank = {0, 0, 0, 0, 0, 0};
    private int irqctrreload = 0;
    private int irqctr = 0;
    private boolean irqenable = false;
    private boolean irqreload = false;
    private boolean interrupted = false;

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        //swappable bank
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
        }
        //fixed bank
        for (int i = 1; i <= 16; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x8000 || addr > 0xFFFF) {
            super.cartWrite(addr, data);
            return;
        } else if (addr <= 0x9FFF) {
            switch (addr & 3) {
                case 0:
                    prgbank0 = data;
                    setbanks();
                    break;
                case 1:
                    prgbank1 = data;
                    setbanks();
                    break;
                case 2:
                    chrbank[0] = data;
                    setbanks();
                    break;
                case 3:
                    chrbank[1] = data;
                    setbanks();
                    break;
            }
        } else if (addr <= 0xBFFF) {
            switch (addr & 3) {
                case 0:
                    chrbank[2] = data;
                    setbanks();
                    break;
                case 1:
                    chrbank[3] = data;
                    setbanks();
                    break;
                case 2:
                    chrbank[4] = data;
                    setbanks();
                    break;
                case 3:
                    chrbank[5] = data;
                    setbanks();
                    break;
            }
        } else if (addr <= 0xDFFF) {
            switch (addr & 3) {
                case 0:
                    //value written here used to reload irq counter _@ end of scanline_
                    irqctrreload = data & 0xFF;
                    irqreload = true;
                    break;
                case 1:
                    //any value here reloads irq counter
                    irqctr = data;
                    irqreload = true;
                    break;
                case 2:
                    //any value here enables interrupts
                    irqenable = true;
                    break;
                case 3:
                    //any value here disables IRQ and acknowledges
                    if (interrupted) {
                        --cpu.interrupt;
                    }
                    interrupted = false;
                    irqenable = false;
                    irqctr = irqctrreload;
                    break;
            }
        } else if (addr <= 0xFFFF) {
            switch (addr & 3) {
                case 0:
                    setmirroring(((data & (utils.BIT6)) != 0) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
                    break;
            }
        }

    }

    private void setbanks() {
        //map prg banks
        //last two banks fixed to the last two banks in ROM
        for (int i = 1; i <= 16; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        //first bank set to prg0 register
        for (int i = 0; i < 8; ++i) {
            prg_map[i] = (1024 * (i + 8 * prgbank0)) & (prgsize - 1);
        }
        //second bank set to prg1 register
        for (int i = 0; i < 8; ++i) {
            prg_map[i + 8] = (1024 * (i + 8 * prgbank1)) & (prgsize - 1);
        }

        //map chr banks
        setppubank(1, 4, chrbank[2]);
        setppubank(1, 5, chrbank[3]);
        setppubank(1, 6, chrbank[4]);
        setppubank(1, 7, chrbank[5]);

        setppubank(2, 0, chrbank[0]);
        setppubank(2, 2, chrbank[1]);
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * (i + (banksize * banknum))) & (chrsize - 1);
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
}
