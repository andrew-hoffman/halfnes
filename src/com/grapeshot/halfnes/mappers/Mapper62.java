package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper62 extends Mapper {

    boolean prg_mode;
    int prgselect, chrselect;

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 0; i < 32; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        prg_mode = utils.getbit(addr, 5);
        prgselect = (addr & 0x40) | ((addr >> 8) & 0x3F);
        chrselect = (addr << 2) | (data & 3);

        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * chrselect)) & (chrsize - 1);
        }
        //remap PRG bank
        if (prg_mode) {
            for (int i = 0; i < 16; ++i) {
                prg_map[i] = (1024 * (i + 16 * prgselect)) & (prgsize - 1);
            }
            for (int i = 0; i < 16; ++i) {
                prg_map[16 + i] = (1024 * (i + 16 * prgselect)) & (prgsize - 1);
            }
        } else {
            for (int i = 0; i < 32; ++i) {
                prg_map[i] = (1024 * (i + 32 * (prgselect >> 1))) & (prgsize - 1);
            }
        }

        setmirroring(utils.getbit(addr, 7) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
    }
}
