/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;

/**
 *
 * @author Andrew
 */
public class Mapper76 extends Mapper {
    //a stripped down mmc3 clone for namco/tengen games.
    //almost everything using this is marked as mapper 4 and works fine like that

    private int whichbank = 0;
    private int[] chrreg = {0, 0, 0, 0, 0, 0};

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        for (int i = 1; i <= 32; ++i) {
            prg_map[32 - i] = prgsize - (1024 * i);
        }

        for (int i = 0; i < 8; ++i) {
            chr_map[i] = 0;
        }
    }

    @Override
    public final void cartWrite(int addr, int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        //bankswitches here
        if (addr == 0x8001) {
            if (whichbank <= 5) {
                chrreg[whichbank] = data;
                setupchr();
            } else if (whichbank == 6) {
                for (int i = 0; i < 8; ++i) {
                    prg_map[i] = (1024 * (i + (data * 8))) % prgsize;
                }
            } else if (whichbank == 7) {
                //bank 7 always swappable, always in same place
                for (int i = 0; i < 8; ++i) {
                    prg_map[i + 8] = (1024 * (i + (data * 8))) % prgsize;
                }
            }
        } else if (addr == 0x8000) {
            //bank select
            whichbank = data & 7;

        }
    }

    private void setupchr() {
        setppubank(2, 0, (chrreg[2]) << 1);
        setppubank(2, 2, (chrreg[3]) << 1);
        setppubank(2, 4, (chrreg[4]) << 1);
        setppubank(2, 6, (chrreg[5]) << 1);
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
//        System.err.println(banksize + ", " + bankpos + ", "+ banknum);
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i)) & (chrsize - 1);
        }
    }
}
