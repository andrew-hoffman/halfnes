/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes.mappers;

import com.grapeshot.halfnes.*;
import com.grapeshot.halfnes.audio.*;
import java.util.Arrays;

/**
 *
 * @author Andrew
 */
public class NSFMapper extends Mapper {
    //a nsf playing mapper
    //TODO: add the extra bankswitches required when playing FDS

    private int load, init, play, song, numSongs;
    public boolean nsfBanking;
    public int[] nsfStartBanks = new int[10], nsfBanks = new int[10];
    private int sndchip;
    boolean vrc6 = false, vrc7 = false, mmc5 = false,
            n163 = false, s5b = false, hasInitSound = false, fds = false;
    private boolean n163autoincrement = false;
    private int n163soundAddr = 0;
    private int mmc5multiplier1, mmc5multiplier2;
    private int vrc7regaddr = 0;
    private int s5bSoundCommand = 0;
    private Namco163SoundChip n163Audio;
    private VRC6SoundChip vrc6Audio;
    private VRC7SoundChip vrc7Audio;
    private Sunsoft5BSoundChip s5bAudio;
    private MMC5SoundChip mmc5Audio;
    private static final String trackstr = "Track --- / ---          <-B A->";
    private FDSSoundChip fdsAudio;

    @Override
    public void loadrom() throws BadMapperException {
        loader.parseHeader();
        prgsize = loader.prgsize;
        mappertype = loader.mappertype;
        prgoff = loader.prgoff;
        for (int i = 0x70; i < 0x78; ++i) {
            if (loader.header[i] != 0) {
                nsfBanking = true;
                nsfStartBanks[i - 0x70] = loader.header[i];
            }
        }
        prgoff = 0;
        load = loader.header[0x08] + (loader.header[0x09] << 8);
        init = loader.header[0x0a] + (loader.header[0x0b] << 8);
        play = loader.header[0x0c] + (loader.header[0x0d] << 8);
        numSongs = loader.header[6] - 1;
        song = loader.header[7] - 1;
        if (loader.header[0x7a] == 1) {
            //pal only tune
            this.region = TVType.PAL;
            System.err.println("pal only tune");
        } else {
            this.region = TVType.NTSC;
        }
        chroff = 0;
        chrsize = 0;
        scrolltype = MirrorType.V_MIRROR;
        sndchip = loader.header[0x7B];

        if (!nsfBanking && load < 0x8000) {
            //no banking
            System.err.println("What do I do with this???");
            throw new BadMapperException("NSF with no banking loading low");
        }
        // pad to 4k bank size and copy in starting
        //from where the load addr is in a 4k bank
        //to the end of the file, padding the end to a 4k bank as well
        //so total number of banks can be 2 more than # of 4k
        //chunks in the file.
        int paddingLen = (nsfBanking) ? load & 0x0fff : load - 0x8000;
        prg = new int[1024 * 1024];
        System.arraycopy(loader.load(loader.romlen(), prgoff), 0, prg, paddingLen, loader.romlen());
        crc = crc32(prg);
        haschrram = true;
        chrsize = 8192;
        chr = new int[8192];
        prg_map = new int[(((sndchip & (utils.BIT2)) != 0)) ? 40 : 32];
        if (!nsfBanking) {
            //identity mapping from 1st loaded bank
            for (int i = 0; i < 8; ++i) {
                nsfStartBanks[i] = i;
            }

        }
        //additional headache for NSFs with FDS:
        if (((sndchip & (utils.BIT2)) != 0)) {
            //got to copy some stuff into 6000 - 7fff just because
            nsfStartBanks[8] = nsfStartBanks[6];
            nsfStartBanks[9] = nsfStartBanks[7];

        }
        chr_map = new int[8];
        for (int i = 0; i < 8; ++i) {
            chr_map[i] = (1024 * i) & (chrsize - 1);
        }
        cpuram = new CPURAM(this);
        cpu = new CPU(cpuram);
        ppu = new PPU(this);
        Arrays.fill(pput0, 0x00);
        setmirroring(scrolltype);
        //System.out.println(sndchip);

        //set up the PPU to display titles
        //pick a random color based on the tune's crc (why not?)
        ppu.pal[0] = 0x3f;
        ppu.pal[1] = 0x20 + (int) (crc % 12);
        ppu.pal[2] = 0x20 + (int) (crc % 12);
        ppu.pal[3] = 0x20 + (int) (crc % 12);

        chr = NSFPlayerFont.font;
    }

    @Override
    public void init() {
        //now that we've set up the initial CPU state, do it all over again
        //in order to match the NSF spec.

        //set banks back to the way they were originally
        nsfBanks = nsfStartBanks.clone();
        setBanks();
        //clear all ram to 0
        for (int i = 0; i <= 0x7ff; ++i) {
            cpuram.write(i, 0);
        }
        //initialize sound registers
        for (int i = 0x4000; i <= 0x4013; ++i) {
            cpuram.write(i, 0);
        }
        cpuram.write(0x4015, 0x0f);
        //disable frame counter on APU
        cpuram.write(0x4017, 0x40);
        //simulate a jump to the play address
        cpu.push(0xff);
        cpu.push(0xfa);

        cpu.setPC(init);
        cpu.interrupt = -99999; //no interrupts for you
        cpu.setRegA(song);
        if (this.region == TVType.PAL) {
            cpu.setRegX(0x01);
        } else {
            cpu.setRegX(0x00);
        }

        //copy titles to ppu nametable
        for (int i = 0; i < 32 * 24; ++i) {
            //random pattern from basic one liner
            pput0[i] = (Math.random() > 0.5) ? 0x2f : 0x5c;
        }
        for (int i = 0; i < 96; ++i) {
            pput0[i + (32 * 25)] = loader.header[i + 0xe];
        }

        for (int i = 0; i < trackstr.length(); ++i) {
            pput0[i + (32 * 28)] = trackstr.charAt(i);
        }

        if (!hasInitSound) {
            setSoundChip();
            hasInitSound = true;
        }
        if (!fds) { //DON'T CLEAR THIS WHEN STUFF LOADS HERE
            for (int i = 0x6000; i <= 0x7fff; ++i) {
                cpuram.write(i, 0);
            }
        }
    }

    @Override
    public void reset() {
        song = loader.header[7] - 1;
        init();
        cpu.setPC(init);
    }

    //write into the cartridge's address space
    @Override
    public void cartWrite(final int addr, final int data) {
        if (n163 && addr == 0xF800) {
            n163autoincrement = ((data & (utils.BIT7)) != 0);
            n163soundAddr = data & 0x7f;
        } else if (n163 && addr == 0x4800) {
            n163Audio.write(n163soundAddr, data);
            if (n163autoincrement) {
                n163soundAddr = ++n163soundAddr & 0x7f;
            }
        } else if (s5b && addr == 0xE000) {
            s5bAudio.write(s5bSoundCommand, data);
        } else if (s5b && addr == 0xC000) {
            s5bSoundCommand = data & 0xF;
        } else if (vrc6 && addr >= 0xB000 && addr <= 0xB002) {
            vrc6Audio.write((addr & 0xf000) + (addr & 3), data);
        } else if (vrc6 && addr >= 0xA000 && addr <= 0xA002) {
            vrc6Audio.write((addr & 0xf000) + (addr & 3), data);
        } else if (vrc7 && addr == 0x9030) {
            vrc7Audio.write(vrc7regaddr, data);
        } else if (vrc7 && addr == 0x9010) {
            vrc7regaddr = data;
        } else if (vrc6 && addr >= 0x9000 && addr <= 0x9002) {
            vrc6Audio.write((addr & 0xf000) + (addr & 3), data);
        } else if (fds && nsfBanking && addr >= 0x6000) {
            if (addr < 0x8000) {
                int fuuu = prg_map[((addr - 0x6000) >> 10) + 32] + (addr & 1023);
                prg[fuuu] = data;
            } else {
                int fuuu = prg_map[((addr & 0x7fff)) >> 10] + (addr & 1023);
                prg[fuuu] = data;
            }
        } else if (fds && !nsfBanking && addr >= 0x6000) {
            if (addr < 0x8000) {
                prgram[addr - 0x6000] = data;
            } else {
                int fuuu = prg_map[((addr & 0x7fff)) >> 10] + (addr & 1023);
                prg[fuuu] = data;
            }
        } else if (addr >= 0x6000 && addr < 0x8000) {
            //default no-mapper operation just writes if in PRG RAM range
            prgram[addr & 0x1fff] = data;
        } else if ((addr >= 0x5ff8) && (addr < 0x6000)) {
            nsfBanks[addr - 0x5ff8] = data;
            //System.err.println(addr - 0x5ff8 + " " + data);
            setBanks();
        } else if (fds && nsfBanking && (addr == 0x5ff6)) {
            //System.err.println("fds request bank " + data + " in ram0");
            nsfBanks[8] = data;
            setBanks();
        } else if (fds && nsfBanking && (addr >= 0x5ff7)) {
            //System.err.println("fds request bank " + data + " in ram1");
            nsfBanks[9] = data;
            setBanks();
        } else if (mmc5 && (addr >= 0x5C00) && (addr <= 0x5FF5)) {
            prgram[addr - 0x5C00] = data; //RAM emulates ExRAM here
        } else if (mmc5 && (addr == 0x5206)) {
            mmc5multiplier2 = data;
        } else if (mmc5 && (addr == 0x5205)) {
            mmc5multiplier1 = data;
        } else if (mmc5 && (addr >= 0x5000) && (addr <= 0x5015)) {
            mmc5Audio.write(addr - 0x5000, data);
        } else if (fds && (addr >= 0x4040) && (addr <= 0x4092)) {
            fdsAudio.write(addr, data);
        } else {
            System.err.println("write to " + utils.hex(addr) + " goes nowhere");
        }
    }

    @Override
    public int cartRead(final int addr) {
        // by default has wram at 0x6000 and cartridge at 0x8000-0xfff
        // but some mappers have different so override for those
        if (addr >= 0x8000) {
            if (addr > 0xfffa) {
                //reads of last part of RAM should always
                //give the reset vectors here, no matter what
                //NSF bank is mapped there.
                switch (addr) {
                    case 0xfffb:
                        return 0x4c;
                    case 0xfffc:
                        return 0xfb;
                    case 0xfffd:
                        return 0xff;
                    default:
                        return 0x00;
                }
            }
            int fuuu = prg_map[((addr & 0x7fff)) >> 10] + (addr & 1023);
            return prg[fuuu];
        } else if (addr >= 0x6000 && hasprgram) {
            if (fds && nsfBanking) {
                int fuuu = prg_map[((addr - 0x6000) >> 10) + 32] + (addr & 1023);
                return prg[fuuu];
            } else {
                return prgram[addr & 0x1fff];
            }
        } else if ((addr >= 0x5ff8)) {
            return nsfBanks[addr - 0x5ff8];
        } else if (fds && nsfBanking && (addr == 0x5ff6)) {
            return nsfBanks[8];
        } else if (fds && nsfBanking && (addr == 0x5ff7)) {
            return nsfBanks[9];
        } else if (mmc5 && addr >= 0x5C00) {
            return prgram[addr - 0x5C00]; //RAM emulates ExRAM here
        } else if (mmc5 && addr == 0x5206) {
            return ((mmc5multiplier1 * mmc5multiplier2) >> 8) & 0xff;
        } else if (mmc5 && addr == 0x5205) {
            return (mmc5multiplier1 * mmc5multiplier2) & 0xff;
        } else if (mmc5 && addr == 0x5015) {
            return mmc5Audio.status();
        } else if (n163 && addr == 0x4800) {
            System.err.println("readback");
            int retval = n163Audio.read(n163soundAddr);
            if (n163autoincrement) {
                n163soundAddr = ++n163soundAddr & 0x7f;
            }
            return retval;
        } else if (fds && (addr >= 0x4040) && (addr < 0x4093)) {
            return fdsAudio.read(addr);
        }
        System.err.println("reading open bus " + utils.hex(addr));
        return addr >> 8; //open bus
    }

    @Override
    public void ppuWrite(int addr, final int data) {
    }

    int control, prevcontrol;
    int unfinishedcounter = 0;
    int time = 4;

    @Override
    public void notifyscanline(final int scanline) {
        if (scanline == 240) {
            //make sure init isn't still running
            if (cpu.PC != 0xFFFB) {
                //if not in idle loop
                if (unfinishedcounter < time) {
                    ++unfinishedcounter;
                    System.err.println("Init routine hasn't returned in "
                            + unfinishedcounter + " frames");
                    return;
                } else if (unfinishedcounter == time) {
                    ++unfinishedcounter;
                    System.err.println("giving up");
                }
                //if we've given it a few frames
                //and it still hasn't returned from init, then it probably
                //isn't going to (supernsf)
                //so we move blithely forward.
            } else {
                unfinishedcounter = 0;
            }
            //set PPU registers to enable rendering
            ppu.write(6, 0);
            ppu.write(6, 0);
            ppu.write(5, 0);
            ppu.write(0, 0);
            ppu.write(1, utils.BIT1 | utils.BIT3 | utils.BIT4);

            //write track number to screen
            writeTracks();

            //todo: visualization effects
            //read the controller
            prevcontrol = control;
            control = 0;
            //strobe
            cpuram.write(0x4016, 1);
            cpuram.write(0x4016, 0);
            //read each button out
            for (int i = 0; i < 8; ++i) {
                control = (control << 1) + (((cpuram.read(0x4016) & 3) != 0) ? 1 : 0);
            }
            //change song number if we get a button press
            if (((control & 0x80) != 0) && ((prevcontrol & 0x80) == 0)) {
                ++song;
                if (song > numSongs) {
                    song = 0;
                }
                //System.err.println("next song");
                init();
            } else if (((control & 0x40) != 0) && ((prevcontrol & 0x40) == 0)) {
                --song;
                if (song < 0) {
                    song = numSongs;
                }
                //System.err.println("previous song");
                init();
            } else //fake a jsr to the play address from wherever 
            //unless this is a supernsf
             if (unfinishedcounter <= time) {
                    cpu.push((cpu.PC - 1) >> 8);
                    cpu.push((cpu.PC - 1) & 0xff);
                    cpu.setPC(play);
                }
        }
    }

    @Override
    public String getrominfo() {
        return "NSF INFO: \n"
                + "Filename:     " + loader.name + "\n"
                + "Size:     " + loader.romlen() / 1024 + " K\n"
                + "Expansion Sound:  " + expSound() + "\n"
                + "Track: " + (song + 1) + " / " + (numSongs + 1) + "\n"
                + "Load Address:  " + utils.hex(load) + "\n"
                + "Init Address:  " + utils.hex(init) + "\n"
                + "Play Address:  " + utils.hex(play) + "\n"
                + "Banking?      " + (nsfBanking ? "Yes" : "No") + "\n"
                + "CRC:          " + utils.hex(this.crc);
    }

    private String expSound() {
        String chips = "";
        if (vrc6) {
            chips += "VRC6 ";
        }
        if (vrc7) {
            chips += "VRC7 ";
        }
        if (n163) {
            chips += "Namco 163 ";
        }
        if (mmc5) {
            chips += "MMC5 ";
        }
        if (s5b) {
            chips += "Sunsoft 5B ";
        }
        if (fds) {
            chips += "FDS ";
        }
        return ((chips.length() > 0) ? chips : "None");
    }

    private void setBanks() {
        for (int i = 0; i < prg_map.length; ++i) {
            prg_map[i] = (4096 * nsfBanks[i / 4]) + (1024 * (i % 4));
            if ((prg_map[i]) >= prg.length) {
                //System.err.println("broken banks");
                prg_map[i] %= prg.length; //probably a bad idea in general though
                //but who knows what a NSF wants when it tries
                //to switch a bank not in the file?
                //there's no "alias it to a power of 2" for nsfs since the file
                //size doesn't need to be a power of 2 and you can start loading
                //the file halfway into a bank.
            }
        }
        //utils.printarray(prg_map);
        //utils.printarray(nsfStartBanks);
    }

    private void setSoundChip() {
        if (((sndchip & (utils.BIT0)) != 0)) {
            //VRC6 audio
            vrc6 = true;
            vrc6Audio = new VRC6SoundChip();
            cpuram.apu.addExpnSound(vrc6Audio);
        }
        if (((sndchip & (utils.BIT1)) != 0)) {
            //VRC7 audio
            vrc7 = true;
            vrc7Audio = new VRC7SoundChip();
            cpuram.apu.addExpnSound(vrc7Audio);
        }
        if (((sndchip & (utils.BIT2)) != 0)) {
            //FDS audio, not yet implemented
            fds = true;
            fdsAudio = new FDSSoundChip();
            cpuram.apu.addExpnSound(fdsAudio);
        }
        if (((sndchip & (utils.BIT3)) != 0)) {
            //MMC5 audio
            mmc5 = true;
            mmc5Audio = new MMC5SoundChip();
            cpuram.apu.addExpnSound(mmc5Audio);
        }
        if (((sndchip & (utils.BIT4)) != 0)) {
            //Namco 163 audio
            n163 = true;
            n163Audio = new Namco163SoundChip();
            cpuram.apu.addExpnSound(n163Audio);
        }
        if (((sndchip & (utils.BIT5)) != 0)) {
            //Sunsoft 5B audio
            s5b = true;
            s5bAudio = new Sunsoft5BSoundChip();
            cpuram.apu.addExpnSound(s5bAudio);
        }
    }

    private void writeTracks() {
        String cur = String.format("%3d / %-3d", song + 1, numSongs + 1);
        for (int i = 0; i < cur.length(); ++i) {
            pput0[i + (32 * 28) + 6] = cur.charAt(i);
        }
    }
}
