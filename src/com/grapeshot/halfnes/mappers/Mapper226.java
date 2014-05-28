package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper226 extends Mapper {

    int[] reg = {0, 0};

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
        if (addr < 0x8000 || addr > 0xFFFF) {
            super.cartWrite(addr, data);
            return;
        }

        reg[addr & 1] = data;

        int bank = ((reg[0] >> 1 & 0x0F) | (reg[0] >> 3 & 0x10) | (reg[1] << 5 & 0x20));

        setmirroring(utils.getbit(reg[0], 6) ? MirrorType.V_MIRROR : MirrorType.H_MIRROR);

        if ((reg[0] & 0x20) != 0) {
            bank = (bank << 1) | (reg[0] & 1);
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
    }
}
