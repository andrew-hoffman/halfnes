package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper72 extends Mapper {

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
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }

        if ((data & 0x40) != 0) {
            //remap CHR bank
            for (int i = 0; i < 8; ++i) {
                chr_map[i] = (1024 * (i + 8 * (data & 0xF))) & (chrsize - 1);
            }
        }

        if ((data & 0x80) != 0) {
            //remap PRG bank
            for (int i = 0; i < 16; ++i) {
                prg_map[i] = (1024 * (i + 16 * (data & 0xF))) & (prgsize - 1);
            }
        }
    }
}
