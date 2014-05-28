package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

public class Mapper15 extends Mapper {

    @Override
    public void loadrom() throws BadMapperException {
        // needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 0; i < 32; ++i) {
            prg_map[i] = (1024 * i) & (prgsize - 1);
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

        int prgbank = (data << 1) & 0xFE;
        int prgflip = data >> 7;
        setmirroring(utils.getbit(data, 6) ? MirrorType.H_MIRROR : MirrorType.V_MIRROR);


        switch (addr & 0xFFF) {
            case 0x000:
                for (int i = 0; i < 8; ++i) {
                    prg_map[i] = (1024 * (i + 8 * (prgbank | 0 ^ prgflip))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[8 + i] = (1024 * (i + 8 * (prgbank | 1 ^ prgflip))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[16 + i] = (1024 * (i + 8 * (prgbank | 2 ^ prgflip))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[24 + i] = (1024 * (i + 8 * (prgbank | 3 ^ prgflip))) & (prgsize - 1);
                }
                break;
            case 0x001:
                for (int i = 0; i < 8; ++i) {
                    prg_map[i] = (1024 * (i + 8 * (prgbank | (0 ^ prgflip)))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[8 + i] = (1024 * (i + 8 * (prgbank | (1 ^ prgflip)))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[16 + i] = (1024 * (i + 8 * (0x7E | (0 ^ prgflip)))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[24 + i] = (1024 * (i + 8 * (0x7F | (1 ^ prgflip)))) & (prgsize - 1);
                }
                break;
            case 0x002:
                prgbank |= prgflip;

                for (int i = 0; i < 8; ++i) {
                    prg_map[i] = (1024 * (i + 8 * prgbank)) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[8 + i] = (1024 * (i + 8 * prgbank)) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[16 + i] = (1024 * (i + 8 * prgbank)) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[24 + i] = (1024 * (i + 8 * prgbank)) & (prgsize - 1);
                }
                break;
            case 0x003:
                prgbank |= prgflip;

                for (int i = 0; i < 8; ++i) {
                    prg_map[i] = (1024 * (i + 8 * prgbank)) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[8 + i] = (1024 * (i + 8 * (prgbank + 1))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[16 + i] = (1024 * (i + 8 * (prgbank + (~addr >> 1 & 1)))) & (prgsize - 1);
                }
                for (int i = 0; i < 8; ++i) {
                    prg_map[24 + i] = (1024 * (i + 8 * (prgbank + 1))) & (prgsize - 1);
                }
                break;
            default:
                break;
        }
    }
}
