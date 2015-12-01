package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class NINA_001_Mapper extends Mapper {

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
        if (addr < 0x7ffd || addr > 0x7fff) {
            super.cartWrite(addr, data);
            return;
        }

        switch (addr) {
            case 0x7FFD:
                for (int i = 0; i < 32; ++i) {
                    prg_map[i] = (1024 * (i + 32 * data)) & (prgsize - 1);
                }
                break;
            case 0x7FFE:
                for (int i = 0; i < 4; ++i) {
                    chr_map[i] = (1024 * (i + 4 * data)) & (chrsize - 1);
                }
                break;
            case 0x7FFF:
                for (int i = 0; i < 4; ++i) {
                    chr_map[4 + i] = (1024 * (i + 4 * data)) & (chrsize - 1);
                }
                break;
        }
    }
}
