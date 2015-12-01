package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper255 extends Mapper {

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
        final int mode = (~addr >> 12 & 1);
        final int bank = (addr >> 8 & 0x40) | (addr >> 6 & 0x3F);

        setmirroring(((addr & 0x2000) != 0) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);

        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * ((addr >> 8 & 0x40) | (addr & 0x3F)))) & (chrsize - 1);
        }
        //remap PRG banks
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * (i + 16 * (bank & ~mode))) & (prgsize - 1);
        }
        for (int i = 0; i < 16; ++i) {
            prg_map[16 + i] = (1024 * (i + 16 * (bank | mode))) & (prgsize - 1);
        }
    }
}
