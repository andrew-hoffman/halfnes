package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class IremH3001Mapper extends Mapper {

    private int[] chrbank = {0, 0, 0, 0, 0, 0, 0, 0};
    private int irqctr, irqreload = 0;
    private boolean irqenable, interrupted = false;

    @Override
    public void loadrom() throws BadMapperException {
        // needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 1; i <= 32; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public final void cartWrite(int addr, int data) {
        if (addr < 0x8000 || addr > 0xCFFF) {
            super.cartWrite(addr, data);
            return;
        }

        if (addr >= 0x8000 && addr <= 0x8FFF) { //PRG Reg 0
            for (int i = 0; i < 8; ++i) {
                prg_map[i] = (1024 * (i + (data * 8))) & (prgsize - 1);
            }
        } else if (addr == 0x9001) {    //Mirroring
            setmirroring(utils.getbit(data, 7) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
        } else if (addr == 0x9003) {    //IRQ Enable
            irqenable = utils.getbit(data, 7);
            if (interrupted) {
                --cpu.interrupt;
                interrupted = false;
            }
        } else if (addr == 0x9004) {    //IRQ Reload
            irqctr = irqreload;
            if (interrupted) {
                --cpu.interrupt;
                interrupted = false;
            }
        } else if (addr == 0x9005) {    //High 8 bits of IRQ Reload
            irqreload = (irqreload & 0x00FF) | (data << 8);
        } else if (addr == 0x9006) {    //Low 8 bits of IRQ Reload
            irqreload = (irqreload & 0xFF00) | data;
        } else if (addr >= 0xA000 && addr <= 0xAFFF) {  //PRG Reg 1
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 8] = (1024 * (i + data * 8)) & (prgsize - 1);
            }
        } else if (addr >= 0xB000 && addr <= 0xBFFF) {  //CHR Regs
            chrbank[addr & 7] = data;
            setppubank(1, (addr & 7), chrbank[addr & 7]);
        } else if (addr >= 0xC000 && addr <= 0xCFFF) {  //PRG Reg 2
            for (int i = 0; i < 8; ++i) {
                prg_map[i + 16] = (1024 * (i + data * 8)) & (prgsize - 1);
            }
        }
    }

    @Override
    public void cpucycle(final int cycles) {
        if (irqenable) {
            if (irqctr <= 0) {
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

    private void setppubank(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * (banknum + i)) & (chrsize - 1);
        }
    }
}
