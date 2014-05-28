package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper152 extends Mapper {

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
        int prgselect = (byte) ((data >> 4) & 0xF);
        int chrselect = (data & 0xF);

        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * chrselect)) & (chrsize - 1);
        }
        //remap PRG bank
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * (i + 16 * prgselect)) & (prgsize - 1);
        }

        setmirroring(utils.getbit(data, 7) ? MirrorType.SS_MIRROR1 : MirrorType.SS_MIRROR0);
    }
}
