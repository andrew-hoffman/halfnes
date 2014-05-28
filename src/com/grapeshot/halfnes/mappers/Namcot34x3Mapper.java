package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Namcot34x3Mapper extends Mapper {
    //MIMIC variant with increased support for CHR up to 128 kB
    //NAMCOT-3433 / NAMCOT-3443 (mapper 88) - no mirroring
    //NAMCOT-3453 (mapper 154) - single-screen mirroring

    private boolean mirroring = false;
    private int whichbank = 0;
    private int[] chrreg = {0, 0, 0, 0, 0, 0};

    public Namcot34x3Mapper(int mappernum) {
        super();
        mirroring = (mappernum == 154);
    }

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
        if (addr == 0x8001) {
            if (whichbank <= 5) {
                chrreg[whichbank] = data & 0x3f;
                setupchr();
            } else if (whichbank == 6) {
                for (int i = 0; i < 8; ++i) {
                    prg_map[i] = (1024 * (i + (data * 8))) % prgsize;
                }
            } else if (whichbank == 7) {
                //System.err.println(data * 8);
                //bank 7 always swappable, always in same place
                for (int i = 0; i < 8; ++i) {
                    prg_map[i + 8] = (1024 * (i + (data * 8))) % prgsize;
                }
            }
        } else if (addr == 0x8000) {
            //bank select
            whichbank = data & 7;
        }

        if (mirroring) {
            setmirroring(utils.getbit(data, 6) ? MirrorType.SS_MIRROR1 : MirrorType.SS_MIRROR0);
        }
    }

    private void setupchr() {
        setppubank(1, 4, (chrreg[2] | 0x40));
        setppubank(1, 5, (chrreg[3] | 0x40));
        setppubank(1, 6, (chrreg[4] | 0x40));
        setppubank(1, 7, (chrreg[5] | 0x40));

        setppubank(2, 0, (chrreg[0] >> 1) << 1);
        setppubank(2, 2, (chrreg[1] >> 1) << 1);
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * (banknum + i)) % chrsize;
        }
    }
}
