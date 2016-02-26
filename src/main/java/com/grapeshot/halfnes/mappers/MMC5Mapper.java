/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.utils;
import com.grapeshot.halfnes.audio.MMC5SoundChip;
import java.util.Arrays;

/**
 *
 * @author Andrew
 */
public class MMC5Mapper extends Mapper {

    //the infamous kitchen sink mapper
    final int[] exram = new int[1024];
    private int exramMode, chrMode, prgMode;
    private int wramWrite1, wramWrite2, multiplier1, multiplier2;
    private int prgpage, chrOr, wrambank;
    boolean scanctrEnable, irqPend;
    private final int[] chrregsA = new int[8];
    private final int[] chrregsB = new int[4];
    private final int[] prgregs = new int[4];
    private final int[] chrmapB = new int[4];
    private final boolean[] romHere = new boolean[3];
    private int scanctrLine, irqCounter = 20;
    private final int[] fillnt = new int[1024];
    private MMC5SoundChip soundchip;
    private boolean inFrame = false;

    @Override
    public void loadrom() throws BadMapperException {
        //needs to be in every mapper. Fill with initial cfg
        super.loadrom();
        //on startup:
        prgregs[3] = (prgsize / 8192) - 1;
        prgregs[2] = (prgsize / 8192) - 1;
        prgregs[1] = (prgsize / 8192) - 1;
        prgregs[0] = (prgsize / 8192) - 1;
        prgMode = 3;
        setupPRG();

        for (int i = 0; i < 8; ++i) {
            chr_map[i] = 1024 * i;
        }
        prgram = new int[65536];
    }

    @Override
    public final void cartWrite(final int addr, final int data) {
        if (addr < 0x5c00) {
            //System.err.println("MMC5 Write register "+ utils.hex(addr) + " d " + utils.hex(data));
            switch (addr) {
                case 0x5000:
                case 0x5001:
                case 0x5002:
                case 0x5003:
                case 0x5004:
                case 0x5005:
                case 0x5006:
                case 0x5007:
                case 0x5010:
                case 0x5011:
                case 0x5015:
                    if (soundchip == null) {
                        soundchip = new MMC5SoundChip();
                        cpuram.apu.addExpnSound(soundchip);
                    }
                    //sound chip
                    soundchip.write(addr - 0x5000, data);
                    break;
                case 0x5100:
                    //prg mode select
                    prgMode = data & 3;
                    setupPRG();
                    break;
                case 0x5101:
                    //chr mode select
                    chrMode = data & 3;
                    setupCHR();
                    break;
                case 0x5102:
                    //wram write protect 1
                    wramWrite1 = data;
                    break;
                case 0x5103:
                    //wram write protect 2
                    wramWrite2 = data;
                    break;
                case 0x5104:
                    //exRAM mode - none of these are properly supported!
                    exramMode = data & 3;
                    break;
                case 0x5105:
                    //mirror mode
                    setMirroring(data, exram);
                    break;
                case 0x5106:
                    //fill tile
                    Arrays.fill(fillnt, 0, 32 * 30, data);
                    break;
                case 0x5107:
                    //fill attribute
                    Arrays.fill(fillnt, 32 * 30, fillnt.length, data & 0x3 + (data & 3) << 2 + (data & 3) << 4 + (data & 3) << 6);
                    break;
                case 0x5113:
                    //PRG RAM register
                    wrambank = data & 7;
                    break;
                case 0x5114:
                    //prg reg 1
                    prgregs[0] = data & 0x7f;
                    romHere[0] = ((data & (utils.BIT7)) != 0);
                    setupPRG();
                    break;
                case 0x5115:
                    //prg reg 2
                    prgregs[1] = data & 0x7f;
                    romHere[1] = ((data & (utils.BIT7)) != 0);
                    setupPRG();
                    break;
                case 0x5116:
                    //prg reg 3
                    prgregs[2] = data & 0x7f;
                    romHere[2] = ((data & (utils.BIT7)) != 0);
                    setupPRG();
                    break;
                case 0x5117:
                    //prg reg 4
                    prgregs[3] = data & 0x7f;
                    setupPRG();
                    break;
                case 0x5120:
                    chrregsA[0] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5121:
                    chrregsA[1] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5122:
                    chrregsA[2] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5123:
                    chrregsA[3] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5124:
                    chrregsA[4] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5125:
                    chrregsA[5] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5126:
                    chrregsA[6] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5127:
                    chrregsA[7] = data | chrOr;
                    setupCHR();
                    break;
                //chr regs A
                case 0x5128:
                    chrregsB[0] = data | chrOr;
                    setupCHR();
                    break;
                case 0x5129:
                    chrregsB[1] = data | chrOr;
                    setupCHR();
                    break;
                case 0x512a:
                    chrregsB[2] = data | chrOr;
                    setupCHR();
                    break;
                case 0x512b:
                    chrregsB[3] = data | chrOr;
                    setupCHR();
                    break;
                //chr regs b
                case 0x5130:
                    //chr bank high bits (CHR_OR)
                    //System.err.println(data);
                    chrOr = (data & 3) << 8;
                    break;
                case 0x5200:
                    //splitscreen control
                    if (((data & (utils.BIT7)) != 0)) {
                        System.err.println("Split screen mode not supported yet");
                    }
                    break;
                case 0x5201:
                    //splitscreen scroll
                    break;
                case 0x5202:
                    //splitscreen chr page
                    break;
                case 0x5203:
                    //irq trigger
                    scanctrLine = data;
                    break;
                case 0x5204:
                    //irq control
                    scanctrEnable = ((data & (utils.BIT7)) != 0);
                    break;
                case 0x5205:
                    multiplier1 = data;
                    break;
                case 0x5206:
                    multiplier2 = data;
                    break;
                default:
                    break;
            }
        } else if (addr < 0x6000) {
            //exram
            exram[addr - 0x5c00] = data;
        } else if (addr < 0x8000) {
            final int wramaddr = wrambank * 8192 + (addr - 0x6000);
            //System.err.println("wrote wram " + utils.hex(wramaddr));
            prgram[wramaddr] = data;
        } else if (addr < 0xA000 && !romHere[0] && prgMode == 3) {
            System.err.println("RAM write to 0x8000 area");
            prgram[((prgregs[0] & 7) * 8192) + (addr - 0x8000)] = data;
        } else if (addr < 0xC000 && !romHere[1]) {
            int subaddr = (prgMode == 3) ? 0xA000 : 0x8000;
            int prgbank = (prgMode == 3) ? (prgregs[1] & 7) : ((prgregs[1] & 7) >> 1);
            int ramaddr = (prgbank * ((prgMode == 3) ? 8192 : 16384)) + (addr - subaddr);
            ////System.err.println("RAM write to 0xA000 area " + utils.hex(addr) + " " + prgbank);
            //System.err.println(utils.hex(ramaddr));
            prgram[ramaddr] = data;
        } else if (addr < 0xE000 && !romHere[2]) {
            System.err.println("RAM write to 0xC000 area " + utils.hex(addr));
            prgram[((prgregs[2] & 7) * 8192) + (addr - 0xc000)] = data;
        } else {
            System.err.println("unsupported mmc5 write " + utils.hex(addr)
                    + romHere[0] + romHere[1] + romHere[2] + prgMode);
        }
    }

    @Override
    public final int cartRead(final int addr) {
        //hook for turning off PPU in frame flag since idk how the real thing works
        if (!ppu.renderingOn() || ppu.scanline > 241) {
            inFrame = false;
        }

        if (addr >= 0x8000) {
            //rom or maybe wram
            if (prgMode == 0
                    || ((prgMode == 1) && (addr >= 0xc000 || romHere[1]))
                    || ((prgMode == 2) && ((addr >= 0xe000 || (addr >= 0xc000 && romHere[2]) || romHere[1]))
                    || ((prgMode == 3) && (addr >= 0xe000
                    || (addr >= 0xc000 && romHere[2])
                    || (addr >= 0xa000 && romHere[1])
                    || romHere[0])))) {
                return prg[prg_map[((addr & 0x7fff)) >> 10] + (addr & 1023)];
            } else {
                //don't know quite how to deal with this yet
                System.err.println("MMC5 wants RAM at " + utils.hex(addr));
                return 0xffff;
            }

        } else if (addr >= 0x6000) {
            //wram
            int ramaddr = wrambank * 8192 + (addr - 0x6000);
            //System.err.println("reading prgram from " + utils.hex(ramaddr));
            return prgram[ramaddr];
        } else if (addr >= 0x5c00) {
            //exram
            return exram[addr - 0x5c00];
        } else {
            switch (addr) {
                case 0x5015:
                    //sound status
                    if (soundchip != null) {
                        return soundchip.status();
                    }
                    return addr >> 8;
                case 0x5204:
                    //irq status
                    int stat = (irqPend ? 0x80 : 0) + (inFrame ? 0x40 : 0);
                    if (irqPend) {
                        irqPend = false;
                        --cpu.interrupt;
                    }
                    return stat;
                case 0x5205:
                    return (multiplier1 * multiplier2) & 0xff;
                case 0x5206:
                    //multiplier
                    return ((multiplier1 * multiplier2) >> 8) & 0xff;
                default:
                    return addr >> 8;

            }
        }
    }

    public void setupPRG() {
        //does NOT support mapping RAM in yet!
        switch (prgMode) {
            default:
            case 0:
                setcpubank(32, 0, (prgregs[3] & 0x7f) >> 2);
                break;
            case 1:
                setcpubank(16, 16, (prgregs[3] & 0x7f) >> 1);
                setcpubank(16, 0, (prgregs[1] & 0x7f) >> 1);
                break;
            case 2:
                setcpubank(8, 24, prgregs[3] & 0x7f);
                setcpubank(8, 16, prgregs[2] & 0x7f);
                setcpubank(8, 8, (prgregs[1] & 0x7f) | 1);
                setcpubank(8, 0, (prgregs[1] & 0x7e));
                break;
            case 3:
                setcpubank(8, 24, prgregs[3] & 0x7f);
                setcpubank(8, 16, prgregs[2] & 0x7f);
                setcpubank(8, 8, prgregs[1] & 0x7f);
                setcpubank(8, 0, prgregs[0] & 0x7f);
                break;
        }
//        System.err.println(prgMode);
//        utils.printarray(prgregs);
//        utils.printarray(prg_map);
    }

    public void setupCHR() {
        switch (chrMode) {
            default:
            case 0:
                setppubank(8, 0, chrregsA[7]);
                setppubankB(4, 0, chrregsB[3]);
                break;
            case 1:
                setppubank(4, 4, chrregsA[7]);
                setppubank(4, 0, chrregsA[3]);
                setppubankB(4, 0, chrregsB[3]);
                break;
            case 2:
                setppubank(2, 6, chrregsA[7]);
                setppubank(2, 4, chrregsA[5]);
                setppubank(2, 2, chrregsA[3]);
                setppubank(2, 0, chrregsA[1]);
                setppubankB(2, 2, chrregsB[3]);
                setppubankB(2, 0, chrregsB[1]);
                break;
            case 3:
                setppubank(1, 7, chrregsA[7]);
                setppubank(1, 6, chrregsA[6]);
                setppubank(1, 5, chrregsA[5]);
                setppubank(1, 4, chrregsA[4]);
                setppubank(1, 3, chrregsA[3]);
                setppubank(1, 2, chrregsA[2]);
                setppubank(1, 1, chrregsA[1]);
                setppubank(1, 0, chrregsA[0]);
                setppubankB(1, 3, chrregsB[3]);
                setppubankB(1, 2, chrregsB[2]);
                setppubankB(1, 1, chrregsB[1]);
                setppubankB(1, 0, chrregsB[0]);
                break;
        }
    }

    private void setppubank(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            chr_map[i + bankpos] = (1024 * ((banknum) + i)) % chrsize;
        }
    }

    private void setppubankB(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            chrmapB[i + bankpos] = (1024 * ((banknum) + i)) % chrsize;
        }
    }

    private void setcpubank(int banksize, int bankpos, int banknum) {
        for (int i = 0; i < banksize; ++i) {
            prg_map[i + bankpos] = (1024 * ((banknum * banksize) + i)) & (prgsize - 1);
        }
    }
    private int fetchcount, exlatch, lastfetch, prevfetch, prevprevfetch;
    private boolean spritemode = false;

    @Override
    public int ppuRead(final int addr) {
        //so how DO we detect which reads are which without
        //seeing the nametable reads?

        //well, as it turns out in the real NES, the MMC5 can in fact see everything
        //put on the PPU bus, whether or not the CS line is asserted for it.
        //must be something to do with 8x16 sprites, and with
        //the 34 reads per scanline of background
        //it reads 34 bg tiles (68 bytes) then 16 sprite tiles (32 bytes)
        if (addr < 0x2000) {
            // System.err.print("p");
            //pattern table read
            if (++fetchcount == 3) {
                spritemode = true;
                //System.err.println(" sprites");
            }
            if (spritemode) {
                return chr[chr_map[addr >> 10] + (addr & 1023)];
            } else {
                //bg mode
                //System.err.print("t");
                if (exramMode == 1) {
                    if (exlatch == 2) {
                        //fetch 3: tile bitmap a
                        ++exlatch;
                        return chr[((chrOr * 1024) | ((exram[lastfetch] & 0x3f) * 4096) | (addr & 4095)) % chr.length];
                    } else if (exlatch == 3) {
                        //fetch 4: tile bitmap b (+ 8 bytes from tile bitmap a)
                        exlatch = 0;
                        return chr[((chrOr * 1024) | ((exram[lastfetch] & 0x3f) * 4096) | (addr & 4095)) % chr.length];
                    }
                }
                return chr[chrmapB[(addr >> 10) & 3] + (addr & 1023)];
            }
        } else {
            // System.err.print("n");
            //nametable read
            if (prevfetch == prevprevfetch && prevprevfetch == addr) {
                //last 3 fetches are the same and that's the signal
                //to increment the scan line counter
                //unfortunately I don't know how the MMC5 resets the counter when PPU is off yet           
                incScanline();
                exlatch = 0;
            }
            prevprevfetch = prevfetch;
            prevfetch = addr;
            spritemode = false;
            fetchcount = 0;
            //  System.err.println(" bg");
            if (exramMode == 1) {
                if (exlatch == 0) {
                    //fetch 1: nametable fetch
                    ++exlatch;
                    lastfetch = addr & 0x3ff;
                } else if (exlatch == 1) {
                    ++exlatch;
                    //fetch 2: attribute table fetch
                    int theone = exram[lastfetch];
                    return ((theone & 0xc0) >> 6) | ((theone & 0xc0) >> 4) | ((theone & 0xc0) >> 2) | (theone & 0xc0);
                }
            }
            return super.ppuRead(addr);
        }
    }

    public void incScanline() {
        if (!inFrame) {
            inFrame = true;
            irqCounter = 0;
            if (irqPend) {
                irqPend = false;
                --cpu.interrupt;
            }
        } else {
            if (irqCounter++ == scanctrLine) {
                irqPend = true;

            }
            if (irqPend && scanctrEnable) {
                ++cpu.interrupt;
            }
        }
    }

    public void setMirroring(int ntsetup, int[] exram) {
        //hook for the MMC5
        switch (ntsetup & 3) {
            case 0:
            default:
                nt0 = pput0;
                break;
            case 1:
                nt0 = pput1;
                break;
            case 2:
                nt0 = exram;
                break;
            case 3:
                nt0 = fillnt;
        }
        ntsetup >>= 2;
        switch (ntsetup & 3) {
            case 0:
            default:
                nt1 = pput0;
                break;
            case 1:
                nt1 = pput1;
                break;
            case 2:
                nt1 = exram;
                break;
            case 3:
                nt1 = fillnt;
                break;
        }
        ntsetup >>= 2;
        switch (ntsetup & 3) {
            case 0:
            default:
                nt2 = pput0;
                break;
            case 1:
                nt2 = pput1;
                break;
            case 2:
                nt2 = exram;
                break;
            case 3:
                nt2 = fillnt;
                break;
        }
        ntsetup >>= 2;
        switch (ntsetup & 3) {
            case 0:
            default:
                nt3 = pput0;
                break;
            case 1:
                nt3 = pput1;
                break;
            case 2:
                nt3 = exram;
                break;
            case 3:
                nt3 = fillnt;
                break;
        }
    }
}
