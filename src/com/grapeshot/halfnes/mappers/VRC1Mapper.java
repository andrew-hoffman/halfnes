package com.grapeshot.halfnes.mappers;
//HalfNES, Copyright Andrew Hoffman, October 2010

import com.grapeshot.halfnes.*;

public class VRC1Mapper extends Mapper {

    int prgbank0, prgbank1, prgbank2 = 0;
    int[] chrbank = {0, 0};

    @Override
    public void loadrom() throws BadMapperException {
        // needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        //swappable bank
        for (int i = 0; i < 24; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
        }
        //fixed bank
        for (int i = 1; i <= 8; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
    }

    @Override
    public final void cartWrite(int addr, int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }

        switch (addr >> 12) {
            case 0x8:
                prgbank0 = data & 0xf;
                setbanks();
                break;
            case 0x9:
                setmirroring(utils.getbit(data, 0) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);
                chrbank[0] = (chrbank[0] & 0xf) | ((data << 3) & 0x10);
                chrbank[1] = (chrbank[1] & 0xf) | ((data << 2) & 0x10);
                setbanks();
                break;
            case 0xA:
                prgbank1 = data & 0xf;
                setbanks();
                break;
            case 0xC:
                prgbank2 = data & 0xf;
                setbanks();
                break;
            case 0xE:
                chrbank[0] = (chrbank[0] & 0x10) | (data & 0xf);
                setbanks();
                break;
            case 0xF:
                chrbank[1] = (chrbank[1] & 0x10) | (data & 0xf);
                setbanks();
                break;
        }
    }

    private void setbanks() {
        //map prg banks
        //last bank fixed to the last bank in ROM
        for (int i = 1; i <= 8; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }
        //first bank set to prg0 register
        for (int i = 0; i < 8; ++i) {
            prg_map[i] = (1024 * (i + 8 * prgbank0)) % (prgsize - 1);
        }
        //second bank set to prg1 register
        for (int i = 0; i < 8; ++i) {
            prg_map[i + 8] = (1024 * (i + 8 * prgbank1)) % (prgsize - 1);
        }
        //third bank set to prg2 register
        for (int i = 0; i < 8; ++i) {
            prg_map[i + 16] = (1024 * (i + 8 * prgbank2)) % (prgsize - 1);
        }

        //map chr banks
        setppubank(4, 0, chrbank[0]);
        setppubank(4, 4, chrbank[1]);
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * (i + 4 * banknum)) % (chrsize - 1);
        }
    }
}
