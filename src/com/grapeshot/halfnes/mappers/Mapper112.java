package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper112 extends Mapper {
    //Chinese variant of MIMIC mapper

    private int whichbank = 0;
    private int[] chrreg = {0, 0, 0, 0, 0, 0, 0, 0};

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
    }

    @Override
    public final void cartWrite(int addr, int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        //bankswitches here
        //System.err.println(utils.hex(addr) + " " + utils.hex(data));
        if (addr == 0xA000) {
            if (whichbank >= 2) {
                chrreg[whichbank - 2] = data;
                setupchr();
            } else if (whichbank == 0) {
                for (int i = 0; i < 8; ++i) {
                    prg_map[i] = (1024 * (i + (data * 8))) % prgsize;
                }
            } else if (whichbank == 1) {
                //System.err.println(data * 8);
                //bank 7 always swappable, always in same place
                for (int i = 0; i < 8; ++i) {
                    prg_map[i + 8] = (1024 * (i + (data * 8))) % prgsize;
                }
            }
        } else if (addr == 0x8000) {
            //bank select
            whichbank = data & 7;
        } else if (addr == 0xE000) {
            setmirroring(utils.getbit(data, 0) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
        }
    }

    private void setupchr() {
        setppubank(1, 4, chrreg[2]);
        setppubank(1, 5, chrreg[3]);
        setppubank(1, 6, chrreg[4]);
        setppubank(1, 7, chrreg[5]);

        setppubank(2, 0, (chrreg[0] >> 1) << 1);
        setppubank(2, 2, (chrreg[1] >> 1) << 1);
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i)) % chrsize;
        }
    }
}
