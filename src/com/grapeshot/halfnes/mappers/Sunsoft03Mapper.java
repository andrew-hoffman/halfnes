package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Sunsoft03Mapper extends Mapper {

    int[] chrbank = {0, 0, 0, 0};
    private int irqctr = 0;
    private boolean irqenable = false;
    private boolean interrupted = false;
    private boolean irqtoggle = false;

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
        if (addr >= 0x8800 && addr <= 0x8FFF) {
            chrbank[0] = data;
            setupchr();
        } else if (addr >= 0x9800 && addr <= 0x9FFF) {
            chrbank[1] = data;
            setupchr();
        } else if (addr >= 0xA800 && addr <= 0xAFFF) {
            chrbank[2] = data;
            setupchr();
        } else if (addr >= 0xB800 && addr <= 0xBFFF) {
            chrbank[3] = data;
            setupchr();
        } else if (addr >= 0xC800 && addr <= 0xCFFF) {
            if (!irqtoggle) {
                //first write
                irqctr = (irqctr & 0xFF) | (data << 8);
                irqtoggle = true;
            } else {
                //second write
                irqctr = (irqctr & 0xFF00) | (data & 0xFF);
                irqtoggle = false;
            }
        } else if (addr >= 0xD800 && addr <= 0xDFFF) {
            if (interrupted) {
                --cpu.interrupt;
                interrupted = false;
            }
            irqenable = utils.getbit(data, 4);
            irqtoggle = false;
        } else if (addr >= 0xE800 && addr <= 0xEFFF) {
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
        } else if (addr >= 0xF800 && addr <= 0xFFFF) {
            for (int i = 0; i < 16; ++i) {
                prg_map[i] = (1024 * (i + 16 * data)) & (prgsize - 1);
            }
        }
    }

    @Override
    public void cpucycle(final int cycles) {
        if (irqenable) {
            if (irqctr <= 0) {
                irqctr = 0xFFFF;
                if (!interrupted) {
                    ++cpu.interrupt;
                    interrupted = true;
                }
                irqenable = false;
            } else {
                irqctr -= cycles;
            }
        }
    }

    private void setupchr() {
        setppubank(2, 0, chrbank[0]);
        setppubank(2, 2, chrbank[1]);
        setppubank(2, 4, chrbank[2]);
        setppubank(2, 6, chrbank[3]);
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * (i + 2 * banknum)) % chrsize;
        }
    }
}
