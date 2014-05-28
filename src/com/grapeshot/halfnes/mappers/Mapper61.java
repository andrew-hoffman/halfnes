package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper61 extends Mapper {

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
    public void reset() {
        cartWrite(0x8000, cartRead(0x8000));
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }

        switch (addr & 0x30) {
            case 0x00:
            case 0x30:
                for (int i = 0; i < 32; ++i) {
                    prg_map[i] = (1024 * (i + 32 * (addr & 0xF))) & (prgsize - 1);
                }
                break;
            case 0x10:
            case 0x20:
                int prgselect = (addr << 1 & 0x1E) | (addr >> 4 & 2);
                for (int i = 0; i < 16; ++i) {
                    prg_map[i] = (1024 * (i + 32 * prgselect)) & (prgsize - 1);
                }
                for (int i = 0; i < 16; ++i) {
                    prg_map[i + 16] = (1024 * (i + 32 * prgselect)) & (prgsize - 1);
                }
        }

        setmirroring(utils.getbit(addr, 7) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
    }
}