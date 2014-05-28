package com.grapeshot.halfnes.mappers;
//HalfNES, Copyright Andrew Hoffman, October 2010

import com.grapeshot.halfnes.*;

public class VRC4Mapper extends Mapper {

    int[][][] registerselectbits = {{{1, 2}, {6, 7}},
        {{2, 3}, {0, 1}},
        {{3, 2}, {1, 0}}};
    int[][] registers;
    int prgbank0, prgbank1 = 0;
    int[] chrbank = {0, 0, 0, 0, 0, 0, 0, 0};
    boolean prgmode, irqmode, irqenable, irqack, firedinterrupt = false;
    int irqreload, irqcounter = 22;

    public VRC4Mapper(int mappernum) {
        super();
        switch (mappernum) {
            //vrc4 has 3 different mapper numbers, for 3 different ways to assign the registers
            case 21:
                registers = registerselectbits[0];
                break;
            case 23:
                registers = registerselectbits[1];
                break;
            case 25:
                registers = registerselectbits[2];
                break;
            default:
                registers = registerselectbits[0];
                break;
        }
    }

    @Override
    public void loadrom() throws BadMapperException {
        super.loadrom();
        // needs to be in every mapper. Fill with initial cfg
        for (int i = 1; i <= 32; ++i) {
            //map last banks in to start off
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public final void cartWrite(int addr, int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        if (irqmode && irqenable) {
            //if irq prescaler is disabled should cause an irq every cycle
            //but i can't interrupt every cycle, so i'm settling for every ram read
            notifyscanline(999);
        }

        boolean bit0 = utils.getbit(addr, registers[0][0]) || utils.getbit(addr, registers[1][0]);
        boolean bit1 = utils.getbit(addr, registers[0][1]) || utils.getbit(addr, registers[1][1]);
        switch (addr >> 12) {
            case 0x8:
                prgbank0 = data & 0x1f;
                break;
            case 0x9:
                if (!bit1) {
                    switch (data & 3) {
                        case 0:
                            setmirroring(MirrorType.V_MIRROR);
                            break;
                        case 1:
                            setmirroring(MirrorType.H_MIRROR);
                            break;
                        case 2:
                            setmirroring(MirrorType.SS_MIRROR0);
                            break;
                        case 3:
                            setmirroring(MirrorType.SS_MIRROR1);
                            break;
                    }
                } else {
                    prgmode = utils.getbit(data, 1);
                }
                break;
            case 0xa:
                prgbank1 = data & 0x1f;
                break;
            case 0xb:
            case 0xc:
            case 0xd:
            case 0xe:
                //chr bank select. black magic
                data &= 0xf;
                int whichreg = ((addr - 0xb000) >> 11) + ((bit1) ? 1 : 0);
                int oldval = chrbank[whichreg];
                if (!bit0) {
                    oldval &= 0xf0;
                    oldval |= data;
                } else {
                    oldval &= 0xf;
                    oldval |= (data << 4);
                }
                chrbank[whichreg] = oldval;
                break;
            case 0xf:
                //irq registers.
                if (!bit1) {
                    if (!bit0) {
                        irqreload &= 0xf0;
                        irqreload |= data & 0xf;
                    } else {
                        irqreload &= 0xf;
                        irqreload |= (data & 0xf) << 4;
                    }
                    // System.err.println("reload set to " + irqreload);
                } else {
                    if (!bit0) {
                        irqack = utils.getbit(data, 0);
                        irqenable = utils.getbit(data, 1);
                        irqmode = utils.getbit(data, 2);
                        if (irqenable) {
                            irqcounter = irqreload;
                        }
                        if (firedinterrupt) {
                            --cpu.interrupt;
                        }
                        firedinterrupt = false;

                    } else {
                        irqenable = irqack;
                        if (firedinterrupt) {
                            --cpu.interrupt;
                        }
                        firedinterrupt = false;
                    }
                }
        }
        if (addr < 0xf000) {
            setbanks();
        }
    }

    private void setbanks() {
        //map prg banks
        if (!prgmode) {
            //last 2 banks fixed to last two in rom
            for (int i = 1; i <= 16; ++i) {
                prg_map[32 - i] = prgsize - (1024 * i);
            }
            //first bank set to prg0 register
            for (int i = 0; i < 8; ++i) {
                prg_map[i] = (1024 * (i + 8 * prgbank0)) % prgsize;
            }
            //second bank set to prg1 register
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 8] = (1024 * (i + 8 * prgbank1)) % prgsize;
            }
        } else {
            //fixed banks 1 and 4
            for (int i = 1; i <= 8; ++i) {
                prg_map[8 - i] = prgsize - (1024 * i);
            }
            for (int i = 1; i <= 8; ++i) {
                prg_map[32 - i] = prgsize - (1024 * i);
            }
            //second bank set to prg0 register
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 8] = (1024 * (i + 8 * prgbank0)) % prgsize;
            }
            //third bank set to prg1 register
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 16] = (1024 * (i + 8 * prgbank1)) % prgsize;
            }
        }
        //map chr banks
        for (int i = 0; i < 8; ++i) {
            setppubank(1, i, chrbank[i]);
        }

    }

    private void setppubank(int banksize, int bankpos, int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i)) % chrsize;
        }
//        utils.printarray(chr_map);
    }

    @Override
    public void notifyscanline(final int scanline) {
        if (irqenable) {
            if (irqcounter >= 255) {
                irqcounter = irqreload;
                //System.err.println("Interrupt @ Scanline " + scanline + " reload " + irqreload);
                if (!firedinterrupt) {
                    ++cpu.interrupt;
                }
                firedinterrupt = true;
            } else {
                ++irqcounter;
            }
        }
    }
}
