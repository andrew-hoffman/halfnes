package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper97 extends Mapper {

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        //fixed bank
        for (int i = 1; i <= 16; ++i) {
            prg_map[16 - i] = prgsize - (1024 * i);
        }
        //swappable bank
        for (int i = 0; i < 16; ++i) {
            prg_map[16 + i] = (1024 * i) & (prgsize - 1);
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
        int prgselect = (byte) (data & 0xF);

        //remap PRG bank
        for (int i = 0; i < 16; ++i) {
            prg_map[16 + i] = (1024 * (i + 16 * prgselect)) & (prgsize - 1);
        }

        int mirroring = data >> 6;
        switch (mirroring) {
            case 0:
                setmirroring(MirrorType.SS_MIRROR0);
                break;
            case 1:
                setmirroring(MirrorType.H_MIRROR);
                break;
            case 2:
                setmirroring(MirrorType.V_MIRROR);
                break;
            case 3:
                setmirroring(MirrorType.SS_MIRROR1);
                break;
        }
    }
}
