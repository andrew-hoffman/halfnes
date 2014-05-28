package com.grapeshot.halfnes.mappers;
//HalfNES, Copyright Andrew Hoffman, October 2010

import com.grapeshot.halfnes.*;

public class MMC1Mapper extends Mapper {

    private int mmc1shift = 0;
    private int mmc1latch = 0;
    private int mmc1ctrl = 0xc;
    private int mmc1chr0 = 0;
    private int mmc1chr1 = 0;
    private int mmc1prg = 0;
    private boolean soromlatch = false;
    private double cpucycleprev = 0; // for Bill and Ted fix
    private long framecountprev = 0;

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
        setbanks();
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x8000 || addr > 0xffff) {
            super.cartWrite(addr, data);
            return;
        }
        if (cpu.cycles == cpucycleprev && cpuram.apu.nes.framecount == framecountprev) {
            return; //bill and ted fix - prevents 2 writes too close together
            //from being acknowledged
            //if I ever go to a cycle based core instead of opcode based this needs to change.
        }
        framecountprev = cpuram.apu.nes.framecount;
        //and this is extremely ugly/likely to break
        //but is needed to prevent the Bill+Ted fix from breaking Dr Mario intro.
        cpucycleprev = cpu.cycles;
        if (utils.getbit(data, 7)) {
            // reset shift register
            mmc1shift = 0;
            mmc1latch = 0;
            mmc1ctrl |= 0xc;
            setbanks();
            return;
        }

        mmc1shift = (mmc1shift >> 1) + (data & 1) * 16;
        ++mmc1latch;
        // mmc1shift &= 0x1f;
        if (mmc1latch < 5) {
            return; // no need to do anything
        } else {
            if (addr >= 0x8000 && addr <= 0x9fff) {
                // mmc1control
                mmc1ctrl = mmc1shift & 0x1f;
                MirrorType mirtype;
                switch (mmc1ctrl & 3) {
                    case 0:
                        mirtype = MirrorType.SS_MIRROR0;
                        break;
                    case 1:
                        mirtype = MirrorType.SS_MIRROR1;
                        break;
                    case 2:
                        mirtype = MirrorType.V_MIRROR;
                        break;
                    default:
                        mirtype = MirrorType.H_MIRROR;
                        break;
                }
                setmirroring(mirtype);

            } else if (addr >= 0xa000 && addr <= 0xbfff) {
                // mmc1chr0
                mmc1chr0 = mmc1shift & 0x1f;
                if (prgsize > 262144) {
                    //SOROM boards use the high bit of CHR to switch between 1st and last
                    //256k of the PRG ROM
                    mmc1chr0 &= 0xf;
                    soromlatch = utils.getbit(mmc1shift, 4);
                }
            } else if (addr >= 0xc000 && addr <= 0xdfff) {
                // mmc1chr1
                mmc1chr1 = mmc1shift & 0x1f;
                if (prgsize > 262144) {
                    mmc1chr1 &= 0xf;
                }
            } else if (addr >= 0xe000 && addr <= 0xffff) {
                // mmc1prg
                mmc1prg = mmc1shift & 0xf;
            }
            // System.err.println("Mapper Bankswitch: Write " + utils.hex(mmc1shift) + " @ " + utils.hex(addr));
            setbanks();
            mmc1latch = 0;
            mmc1shift = 0;
        }

    }

    private void setbanks() {
        // chr bank 0
        if (utils.getbit(mmc1ctrl, 4)) {
            // 4k bank mode
            for (int i = 0; i < 4; ++i) {
                chr_map[i] = (1024 * (i + 4 * mmc1chr0)) % chrsize;
            }
            for (int i = 0; i < 4; ++i) {
                chr_map[i + 4] = (1024 * (i + 4 * mmc1chr1)) % chrsize;
            }
        } else {
            // 8k bank mode
            for (int i = 0; i < 8; ++i) {
                chr_map[i] = (1024 * (i + 8 * (mmc1chr0 >> 1))) % chrsize;
            }
        }

        // prg bank
        if (!utils.getbit(mmc1ctrl, 3)) {
            // 32k switch
            // ignore low bank bit
            for (int i = 0; i < 32; ++i) {
                prg_map[i] = (1024 * i + 32768 * (mmc1prg >> 1)) % prgsize;
            }

        } else if (!utils.getbit(mmc1ctrl, 2)) {
            // fix 1st bank, 16k switch 2nd bank
            for (int i = 0; i < 16; ++i) {
                prg_map[i] = (1024 * i);
            }
            for (int i = 0; i < 16; ++i) {
                prg_map[i + 16] = (1024 * i + 16384 * mmc1prg) % prgsize;
            }
        } else {
            // fix last bank, switch 1st bank
            for (int i = 0; i < 16; ++i) {
                prg_map[i] = (1024 * i + 16384 * mmc1prg) % prgsize;
            }
            for (int i = 1; i <= 16; ++i) {
                prg_map[32 - i] = (prgsize - (1024 * i));
                if ((prg_map[32 - i]) > 262144) {
                    prg_map[32 - i] -= 262144;
                }
            }
        }
        //if more thn 256k ROM AND SOROM latch is on
        if (soromlatch && (prgsize > 262144)) {
            //add 256k to all of the prg bank #s
            for (int i = 0; i < prg_map.length; ++i) {
                prg_map[i] += 262144;
            }
        }
        //utils.printarray(prg_map);
    }
}
