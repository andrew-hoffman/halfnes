package com.grapeshot.halfnes.mappers;

public class Mapper36 extends Mapper {
    
    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 1; i <= 32; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        for (int i = 1; i <= 8; ++i) {
            chr_map[8 - i] = chrsize - (1024 * i);
        }
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x8400 || addr > 0xfffe) {
            super.cartWrite(addr, data);
            return;
        }

        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * data)) & (chrsize - 1);
        }
        //remap PRG bank
        for (int i = 0; i < 32; ++i) {
            prg_map[i] = (1024 * (i + 32 * (data >> 4))) & (prgsize - 1);
        }
    }
}