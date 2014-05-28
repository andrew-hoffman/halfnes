package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper225 extends Mapper {

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

        setmirroring(utils.getbit(addr, 0xD) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);

        int bank = addr >> 7 & 0x1F;

        if ((addr & 0x1000) != 0) {
            bank = (bank << 1) | (addr >> 6 & 1);
            for (int i = 0; i < 16; ++i) {
                prg_map[i] = (1024 * (i + 16 * bank)) & (prgsize - 1);
            }
            for (int i = 0; i < 16; ++i) {
                prg_map[i + 16] = (1024 * (i + 16 * bank)) & (prgsize - 1);
            }
        } else {
            for (int i = 0; i < 32; ++i) {
                prg_map[i] = (1024 * (i + 32 * bank)) & (prgsize - 1);
            }
        }
        //remap CHR bank
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * (i + 8 * addr)) & (chrsize - 1);
        }
    }
}
