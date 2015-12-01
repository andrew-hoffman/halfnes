package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper244 extends Mapper {

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
        if (addr < 0x8065 || addr > 0x80E4) {
            super.cartWrite(addr, data);
            return;
        }

        if (addr < 0x80A5) {
            //remap PRG bank
            for (int i = 0; i < 32; ++i) {
                prg_map[i] = (1024 * (i + 32 * ((addr - 0x8065) & 3))) & (prgsize - 1);
            }
        } else {
            //remap CHR bank
            for (int i = 0; i < 8; ++i) {
                chr_map[i] = (1024 * (i + 8 * ((addr - 0x80A5) & 7))) & (chrsize - 1);
            }
        }
    }
}