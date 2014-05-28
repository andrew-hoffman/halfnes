package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper231 extends Mapper {

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

        setmirroring(utils.getbit(addr, 7) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);

        int prg = addr & 0x1E;

        //remap PRG bank
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * (i + 16 * prg)) & (prgsize - 1);
        }
        for (int i = 0; i < 16; ++i) {
            prg_map[i + 16] = (1024 * (i + 16 * (prg | (addr >> 5 & 1)))) & (prgsize - 1);
        }
    }
}
