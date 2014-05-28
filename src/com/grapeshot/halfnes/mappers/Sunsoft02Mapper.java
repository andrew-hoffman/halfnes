package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Sunsoft02Mapper extends Mapper {

    boolean m93;

    public Sunsoft02Mapper(int mappernum) {
        super();
        m93 = (mappernum == 93);
    }

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
        int prgselect;

        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        if (m93) {
            prgselect = (data >> 4) & 15;
        } else {
            prgselect = (data >> 4) & 7;
            setmirroring(utils.getbit(data, 3) ? MirrorType.SS_MIRROR1 : MirrorType.SS_MIRROR0);

            int chrselect = ((data & 7) | (data >> 7) * 8);

            //remap CHR bank
            for (int i = 0; i < 8; ++i) {
                chr_map[i] = (1024 * (i + 8 * chrselect)) & (chrsize - 1);
            }
        }

        //remap PRG bank
        for (int i = 0; i < 16; ++i) {
            prg_map[i] = (1024 * (i + 16 * prgselect)) & (prgsize - 1);
        }
    }
}
