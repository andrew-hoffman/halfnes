/*
 * HalfNES by Andrew Hoffman
 * Licensed under the GNU GPL Version 3. See LICENSE file
 */
package com.grapeshot.halfnes;

import static com.grapeshot.halfnes.PrefsSingleton.get;
import com.grapeshot.halfnes.mappers.Mapper;
import com.grapeshot.halfnes.ui.DebugUI;
import com.grapeshot.halfnes.ui.GUIInterface;
import static com.grapeshot.halfnes.utils.reverseByte;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_INT_BGR;
import java.util.Arrays;
import static java.util.Arrays.fill;

public class PPU {

    public Mapper mapper;
    private int oamaddr, oamstart, readbuffer = 0;
    private int loopyV = 0x0;//ppu memory pointer
    private int loopyT = 0x0;//temp pointer
    private int loopyX = 0;//fine x scroll
    public int scanline = 0;
    public int cycles = 0;
    private int framecount = 0;
    private int div = 2;
    private final int[] OAM = new int[256], secOAM = new int[32],
            spriteshiftregH = new int[8],
            spriteshiftregL = new int[8], spriteXlatch = new int[8],
            spritepals = new int[8], bitmap = new int[240 * 256];
    private int found, bgShiftRegH, bgShiftRegL, bgAttrShiftRegH, bgAttrShiftRegL;
    private final boolean[] spritebgflags = new boolean[8];
    private boolean even = true, bgpattern = true, sprpattern, spritesize, nmicontrol,
            grayscale, bgClip, spriteClip, bgOn, spritesOn,
            vblankflag, sprite0hit, spriteoverflow;
    private int emph;
    public final int[] pal;
    private DebugUI debuggui;
    private int vraminc = 1;
    private final static boolean PPUDEBUG = get().getBoolean("ntView", false);
    private BufferedImage nametableView;
    private final int[] bgcolors = new int[256];
    private int openbus = 0; //the last value written to the PPU
    private int nextattr;
    private int linelowbits;
    private int linehighbits;
    private int penultimateattr;
    private int numscanlines;
    private int vblankline;
    private int[] cpudivider;

    public PPU(final Mapper mapper) {
        this.pal = new int[]{0x09, 0x01, 0x00, 0x01, 0x00, 0x02, 0x02, 0x0D,
            0x08, 0x10, 0x08, 0x24, 0x00, 0x00, 0x04, 0x2C, 0x09, 0x01, 0x34,
            0x03, 0x00, 0x04, 0x00, 0x14, 0x08, 0x3A, 0x00, 0x02, 0x00, 0x20,
            0x2C, 0x08};
        /*
     power-up pallette checked by Blargg's power_up_palette test. Different
     revs of NES PPU might give different initial results but there's a test
     expecting this set of values and nesemu1, BizHawk, RockNES, MyNes use it
         */
        this.mapper = mapper;
        fill(OAM, 0xff);
        if (PPUDEBUG) {
            nametableView = new BufferedImage(512, 480, TYPE_INT_BGR);
            debuggui = new DebugUI(512, 480);
            debuggui.run();
        }
        setParameters();
    }

    final void setParameters() {
        //set stuff to NTSC or PAL or Dendy values
        switch (mapper.getTVType()) {
            case NTSC:
            default:
                numscanlines = 262;
                vblankline = 241;
                cpudivider = new int[]{3, 3, 3, 3, 3};
                break;
            case PAL:
                numscanlines = 312;
                vblankline = 241;
                cpudivider = new int[]{4, 3, 3, 3, 3};
                break;
            case DENDY:
                numscanlines = 312;
                vblankline = 291;
                cpudivider = new int[]{3, 3, 3, 3, 3};
                break;
        }
    }

    public void runFrame() {
        for (int line = 0; line < numscanlines; ++line) {
            clockLine(line);
        }
    }

    /**
     * Performs a read from a PPU register, as well as causes any side effects
     * of reading that specific register.
     *
     * @param regnum register to read (address with 0x2000 already subtracted)
     * @return the data in the PPU register, or open bus (the last value written
     * to a PPU register) if the register is read only
     */
    public final int read(final int regnum) {
        switch (regnum) {
            case 2:
                even = true;
                if (scanline == 241) {
                    if (cycles == 1) {//suppress NMI flag if it was just turned on this same cycle
                        vblankflag = false;
                    }
                    //OK, uncommenting this makes blargg's NMI suppression test
                    //work but breaks Antarctic Adventure.
                    //I'm going to need a cycle accurate CPU to fix that...
//                    if (cycles < 4) {
//                        //show vblank flag but cancel pending NMI before the CPU
//                        //can actually do anything with it
//                        //TODO: use proper interface for this
//                        mapper.cpu.nmiNext = false;
//                    }
                }
                openbus = (vblankflag ? 0x80 : 0)
                        | (sprite0hit ? 0x40 : 0)
                        | (spriteoverflow ? 0x20 : 0)
                        | (openbus & 0x1f);
                vblankflag = false;
                break;
            case 4:
                // reading this is NOT reliable but some games do it anyways
                openbus = OAM[oamaddr];
                //System.err.println("codemasters?");
                if (renderingOn() && (scanline <= 240)) {
                    if (cycles < 64) {
                        return 0xFF;
                    } else if (cycles <= 256) {
                        return 0x00;
                    } //Micro Machines relies on this:
                    else if (cycles < 320) {
                        return 0xFF;
                    } //and this:
                    else {
                        return secOAM[0]; //is this the right value @ the time?
                    }
                }
                break;
            case 7:
                // PPUDATA
                // correct behavior. read is delayed by one
                // -unless- is a read from sprite pallettes
                final int temp;
                if ((loopyV & 0x3fff) < 0x3f00) {
                    temp = readbuffer;
                    readbuffer = mapper.ppuRead(loopyV & 0x3fff);
                } else {
                    readbuffer = mapper.ppuRead((loopyV & 0x3fff) - 0x1000);
                    temp = mapper.ppuRead(loopyV);
                }
                if (!renderingOn() || (scanline > 240 && scanline < (numscanlines - 1))) {
                    loopyV += vraminc;
                } else {
                    //if 2007 is read during rendering PPU increments both horiz
                    //and vert counters erroneously.
                    incLoopyVHoriz();
                    incLoopyVVert();
                }
                openbus = temp;
                break;

            // and don't increment on read
            default:
                return openbus; // last value written to ppu
        }
        return openbus;
    }

    /**
     * Performs a write to a PPU register
     *
     * @param regnum register number from 0 to 7, memory addresses are decoded
     * to these elsewhere
     * @param data the value to write to the register (0x00 to 0xff valid)
     */
    public final void write(final int regnum, final int data) {
//        if (regnum != 4 /*&& regnum != 7*/) {
//            System.err.println("PPU write - wrote " + utils.hex(data) + " to reg "
//                    + utils.hex(regnum + 0x2000)
//                    + " frame " + framecount + " scanline " + scanline);
//        }
        //debugdraw();
        openbus = data;
        switch (regnum) {
            case 0: //PPUCONTROL (2000)
                //set 2 bits of vram address (nametable select)
                //bits 0 and 1 affect loopyT to change nametable start by 0x400
                loopyT &= ~0xc00;
                loopyT |= (data & 3) << 10;
                /*
                 SMB1 writes here at the end of its main loop and if this write
                 lands on one exact PPU clock, the address bits are set to 0.
                 This only happens on one CPU/PPU alignment of real hardware 
                 though so it only shows up ~33% of the time.
                 */
                vraminc = (((data & (utils.BIT2)) != 0) ? 32 : 1);
                sprpattern = ((data & (utils.BIT3)) != 0);
                bgpattern = ((data & (utils.BIT4)) != 0);
                spritesize = ((data & (utils.BIT5)) != 0);
                /*bit 6 is kind of a halt and catch fire situation since it outputs
                 ppu color data on the EXT pins that are tied to ground if set
                 and that'll make the PPU get very hot from sourcing the current. 
                 Only really useful for the NESRGB interposer board, kind of
                 useless for emulators. I will ignore it.
                 */
                nmicontrol = ((data & (utils.BIT7)) != 0);

                break;
            case 1: //PPUMASK (2001)
                grayscale = ((data & (utils.BIT0)) != 0);
                bgClip = !((data & (utils.BIT1)) != 0); //clip left 8 pixels when its on
                spriteClip = !((data & (utils.BIT2)) != 0);
                bgOn = ((data & (utils.BIT3)) != 0);
                spritesOn = ((data & (utils.BIT4)) != 0);
                emph = (data & 0xe0) << 1;
                if (numscanlines == 312) {
                    //if PAL switch position of red and green emphasis bits (6 and 5)
                    //red is bit 6 -> bit 7
                    //green is bit 7 -> bit 6
                    int red = (emph >> 6) & 1;
                    int green = (emph >> 7) & 1;
                    emph &= 0xf3f;
                    emph |= (red << 7) | (green << 6);
                }
                break;
            case 3:
                // PPUOAMADDR (2003)
                // most games just write zero and use the dma
                oamaddr = data & 0xff;
                break;
            case 4:
                // PPUOAMDATA(2004)
                if ((oamaddr & 3) == 2) {
                    OAM[oamaddr++] = (data & 0xE3);
                } else {
                    OAM[oamaddr++] = data;
                }
                oamaddr &= 0xff;
                // games don't usually write this directly anyway, it's unreliable
                break;

            // PPUSCROLL(2005)
            case 5:
                if (even) {
                    // update horizontal scroll
                    loopyT &= ~0x1f;
                    loopyX = data & 7;
                    loopyT |= data >> 3;

                    even = false;
                } else {
                    // update vertical scroll
                    loopyT &= ~0x7000;
                    loopyT |= ((data & 7) << 12);
                    loopyT &= ~0x3e0;
                    loopyT |= (data & 0xf8) << 2;
                    even = true;

                }
                break;

            case 6:
                // PPUADDR (2006)
                if (even) {
                    // high byte
                    loopyT &= 0xc0ff;
                    loopyT |= ((data & 0x3f) << 8);
                    loopyT &= 0x3fff;
                    even = false;
                } else {
                    loopyT &= 0xfff00;
                    loopyT |= data;
                    loopyV = loopyT;
                    even = true;
                }
                break;
            case 7:
                // PPUDATA             
                mapper.ppuWrite((loopyV & 0x3fff), data);
                if (!renderingOn() || (scanline > 240 && scanline < (numscanlines - 1))) {
                    loopyV += vraminc;
                } else if ((loopyV & 0x7000) == 0x7000) {
                    int YScroll = loopyV & 0x3E0;
                    loopyV &= 0xFFF;
                    switch (YScroll) {
                        case 0x3A0:
                            loopyV ^= 0xBA0;
                            break;
                        case 0x3E0:
                            loopyV ^= 0x3E0;
                            break;
                        default:
                            loopyV += 0x20;
                            break;
                    }
                } else {
                    // while rendering, it seems to drop by 1 line, regardless of increment mode
                    loopyV += 0x1000;
                }
                break;
            default:
                break;
        }
    }

    /**
     * PPU is on if either background or sprites are enabled
     *
     * @return true
     */
    public boolean renderingOn() {
        return bgOn || spritesOn;
    }

    /**
     * MMC3 scan line counter isn't clocked if background and sprites are using
     * the same half of the pattern table
     *
     * @return true if PPU is rendering and BG and sprites are using different
     * pattern tables
     */
    public final boolean mmc3CounterClocking() {
        return (bgpattern != sprpattern) && renderingOn();
    }

    /**
     * Runs the PPU emulation for one NES scan line.
     */
    public final void clockLine(int scanline) {
        //skip a PPU clock on line 0 of odd frames when rendering is on
        //and we are in NTSC mode (pal has no skip)
        int skip = (numscanlines == 262
                && scanline == 0
                && renderingOn()
                && !((framecount & (utils.BIT1)) != 0)) ? 1 : 0;
        for (cycles = skip; cycles < 341; ++cycles) {
            clock();
        }
    }

    private int tileAddr = 0;
    private int cpudividerctr = 0;

    /**
     * runs the emulation for one PPU clock cycle.
     */
    public final void clock() {

        //cycle based ppu stuff will go here
        if (cycles == 1) {
            if (scanline == 0) {
                dotcrawl = renderingOn();
            }
            if (scanline < 240) {
                bgcolors[scanline] = pal[0];
            }
        }
        if (scanline < 240 || scanline == (numscanlines - 1)) {
            //on all rendering lines
            if (renderingOn()
                    && ((cycles >= 1 && cycles <= 256)
                    || (cycles >= 321 && cycles <= 336))) {
                //fetch background tiles, load shift registers
                bgFetch();
            } else if (cycles == 257 && renderingOn()) {
                //x scroll reset
                //horizontal bits of loopyV = loopyT
                loopyV &= ~0x41f;
                loopyV |= loopyT & 0x41f;

            } else if (cycles > 257 && cycles <= 341) {
                //clear the oam address from pxls 257-341 continuously
                oamaddr = 0;
            }
            if ((cycles == 340) && renderingOn()) {
                //read the same nametable byte twice
                //this signals the MMC5 to increment the scanline counter
                fetchNTByte();
                fetchNTByte();
            }
            if (cycles == 65 && renderingOn()) {
                oamstart = oamaddr;
            }
            if (cycles == 260 && renderingOn()) {
                //evaluate sprites for NEXT scanline (as long as either background or sprites are enabled)
                //this does in fact happen on scanline 261 but it doesn't do anything useful
                //it's cycle 260 because that's when the first important sprite byte is read
                //actually sprite overflow should be set by sprite eval somewhat before
                //so this needs to be split into 2 parts, the eval and the data fetches
                evalSprites();
            }
            if (scanline == (numscanlines - 1)) {
                if (cycles == 0) {// turn off vblank, sprite 0, sprite overflow flags
                    vblankflag = false;
                    sprite0hit = false;
                    spriteoverflow = false;
                } else if (cycles >= 280 && cycles <= 304 && renderingOn()) {
                    //loopyV = (all of)loopyT for each of these cycles
                    loopyV = loopyT;
                }
            }
        } else if (scanline == vblankline && cycles == 1) {
            //handle vblank on / off
            vblankflag = true;
        }
        if (!renderingOn() || (scanline > 240 && scanline < (numscanlines - 1))) {
            //HACK ALERT
            //handle the case of MMC3 mapper watching A12 toggle
            //even when read or write aren't asserted on the bus
            //needed to pass Blargg's mmc3 tests
            mapper.checkA12(loopyV & 0x3fff);
        }
        if (scanline < 240 && cycles >= 1 && cycles <= 256) {
            int bufferoffset = (scanline << 8) + (cycles - 1);
            //bg drawing
            if (bgOn) { //if background is on, draw a line of that
                final boolean isBG = drawBGPixel(bufferoffset);
                //sprite drawing
                drawSprites(scanline << 8, cycles - 1, isBG);

            } else if (spritesOn) {
                //just the sprites then
                int bgcolor = ((loopyV > 0x3f00 && loopyV < 0x3fff) ? mapper.ppuRead(loopyV) : pal[0]);
                bitmap[bufferoffset] = bgcolor;
                drawSprites(scanline << 8, cycles - 1, true);
            } else {
                //rendering is off, so draw either the background color OR
                //if the PPU address points to the palette, draw that color instead.
                int bgcolor = ((loopyV > 0x3f00 && loopyV < 0x3fff) ? mapper.ppuRead(loopyV) : pal[0]);
                bitmap[bufferoffset] = bgcolor;
            }
            //deal with the grayscale flag
            if (grayscale) {
                bitmap[bufferoffset] &= 0x30;
            }
            //handle color emphasis
            bitmap[bufferoffset] = (bitmap[bufferoffset] & 0x3f) | emph;

        }
        //handle nmi
        if (vblankflag && nmicontrol) {
            //pull NMI line on when conditions are right
            mapper.cpu.setNMI(true);
        } else {
            mapper.cpu.setNMI(false);
        }

        //clock CPU, once every 3 ppu cycles
        div = (div + 1) % cpudivider[cpudividerctr];
        if (div == 0) {
            mapper.cpu.runcycle(scanline, cycles);
            mapper.cpucycle(1);
            cpudividerctr = (cpudividerctr + 1) % cpudivider.length;
        }
        if (cycles == 257) {
            mapper.notifyscanline(scanline);
        } else if (cycles == 340) {
            scanline = (scanline + 1) % numscanlines;
            if (scanline == 0) {
                ++framecount;
            }
        }
    }

    private void bgFetch() {
        //fetch tiles for background
        //on real PPU this logic is repurposed for sprite fetches as well
        //System.err.println(hex(loopyV));
        bgAttrShiftRegH |= ((nextattr >> 1) & 1);
        bgAttrShiftRegL |= (nextattr & 1);
        //background fetches
        switch ((cycles - 1) & 7) {
            case 1:
                fetchNTByte();
                break;
            case 3:
                //fetch attribute (FIX MATH)
                penultimateattr = getAttribute(((loopyV & 0xc00) + 0x23c0),
                        (loopyV) & 0x1f,
                        (((loopyV) & 0x3e0) >> 5));
                break;
            case 5:
                //fetch low bg byte
                linelowbits = mapper.ppuRead((tileAddr)
                        + ((loopyV & 0x7000) >> 12));
                break;
            case 7:
                //fetch high bg byte
                linehighbits = mapper.ppuRead((tileAddr) + 8
                        + ((loopyV & 0x7000) >> 12));
                bgShiftRegL |= linelowbits;
                bgShiftRegH |= linehighbits;
                nextattr = penultimateattr;
                if (cycles != 256) {
                    incLoopyVHoriz();
                } else {
                    incLoopyVVert();
                }
                break;
            default:
                break;
        }
        if (cycles >= 321 && cycles <= 336) {
            bgShiftClock();
        }
    }

    private void incLoopyVVert() {
        //increment loopy_v to next row of tiles
        if ((loopyV & 0x7000) == 0x7000) {
            //reset the fine scroll bits and increment tile address to next row
            loopyV &= ~0x7000;
            int y = (loopyV & 0x03E0) >> 5;
            if (y == 29) {
                //if row is 29 zero fine scroll and bump to next nametable
                y = 0;
                loopyV ^= 0x0800;
            } else {
                //increment (wrap to 5 bits) but if row is already over 29
                //we don't bump loopyV to next nt.
                y = (y + 1) & 31;
            }
            loopyV = (loopyV & ~0x03E0) | (y << 5);
        } else {
            //increment the fine scroll
            loopyV += 0x1000;
        }
    }

    private void incLoopyVHoriz() {
        //increment horizontal part of loopyv
        if ((loopyV & 0x001F) == 31) // if coarse X == 31
        {
            loopyV &= ~0x001F; // coarse X = 0
            loopyV ^= 0x0400;// switch horizontal nametable
        } else {
            loopyV += 1;// increment coarse X
        }
    }

    private void fetchNTByte() {
        //fetch nt byte
        tileAddr = mapper.ppuRead(
                ((loopyV & 0xc00) | 0x2000) + (loopyV & 0x3ff)) * 16
                + (bgpattern ? 0x1000 : 0);
    }

    private boolean drawBGPixel(int bufferoffset) {
        //background drawing
        //loopyX picks bits
        final boolean isBG;
        if (bgClip && (bufferoffset & 0xff) < 8) {
            //left hand of screen clipping
            //(needs to be marked as BG and not cause a sprite hit)
            bitmap[bufferoffset] = pal[0];
            isBG = true;
        } else {
            final int bgPix = (((bgShiftRegH >> -loopyX + 16) & 1) << 1)
                    + ((bgShiftRegL >> -loopyX + 16) & 1);
            final int bgPal = (((bgAttrShiftRegH >> -loopyX + 8) & 1) << 1)
                    + ((bgAttrShiftRegL >> -loopyX + 8) & 1);
            isBG = (bgPix == 0);
            bitmap[bufferoffset] = isBG ? pal[0] : pal[(bgPal << 2) + bgPix];
        }
        bgShiftClock();
        return isBG;
    }

    private void bgShiftClock() {
        bgShiftRegH <<= 1;
        bgShiftRegL <<= 1;
        bgAttrShiftRegH <<= 1;
        bgAttrShiftRegL <<= 1;
    }

    boolean dotcrawl = true;
    private boolean sprite0here = false;

    /**
     * evaluates PPU sprites for the NEXT scanline
     */
    private void evalSprites() {
        sprite0here = false;
        int ypos, offset;
        found = 0;
        Arrays.fill(secOAM, 0xff);
        //primary evaluation
        //need to emulate behavior when OAM address is set to nonzero here
        for (int spritestart = oamstart; spritestart < 255; spritestart += 4) {
            //for each sprite, first we cull the non-visible ones
            ypos = OAM[spritestart];
            offset = scanline - ypos;
            if (ypos > scanline || offset > (spritesize ? 15 : 7)) {
                //sprite is out of range vertically
                continue;
            }
            //if we're here it's a valid renderable sprite
            if (spritestart == 0) {
                sprite0here = true;
            }
            //actually which sprite is flagged for sprite 0 depends on the starting
            //oam address which is, on the real thing, not necessarily zero.
            if (found >= 8) {
                //if more than 8 sprites, set overflow bit and STOP looking
                //todo: add "no sprite limit" option back
                spriteoverflow = true;
                break; //also the real PPU does strange stuff on sprite overflow
                //todo: emulate register trashing that happens when overflow
            } else {
                //set up ye sprite for rendering
                secOAM[found * 4] = OAM[spritestart];
//                secOAM[found * 4 + 1] = OAM[spritestart + 1];
//                secOAM[found * 4 + 2] = OAM[spritestart + 2];
//                secOAM[found * 4 + 3] = OAM[spritestart + 3];
                final int oamextra = OAM[spritestart + 2];

                //bg flag
                spritebgflags[found] = ((oamextra & (utils.BIT5)) != 0);
                //x value
                spriteXlatch[found] = OAM[spritestart + 3];
                spritepals[found] = ((oamextra & 3) + 4) * 4;
                if (((oamextra & (utils.BIT7)) != 0)) {
                    //if sprite is flipped vertically, reverse the offset
                    offset = (spritesize ? 15 : 7) - offset;
                }
                //now correction for the fact that 8x16 tiles are 2 separate tiles
                if (offset > 7) {
                    offset += 8;
                }
                //get tile address (8x16 sprites can use both pattern tbl pages but only the even tiles)
                final int tilenum = OAM[spritestart + 1];
                spriteFetch(spritesize, tilenum, offset, oamextra);
                ++found;
            }
        }
        for (int i = found; i < 8; ++i) {
            //fill unused sprite registers with zeros
            spriteshiftregL[found] = 0;
            spriteshiftregH[found] = 0;
            //also, we need to do 8 reads no matter how many sprites we found
            //dummy reads are to sprite 0xff
            spriteFetch(spritesize, 0xff, 0, 0);
        }
    }

    private void spriteFetch(final boolean spritesize, final int tilenum, int offset, final int oamextra) {
        int tilefetched;
        if (spritesize) {
            tilefetched = ((tilenum & 1) * 0x1000)
                    + (tilenum & 0xfe) * 16;
        } else {
            tilefetched = tilenum * 16
                    + ((sprpattern) ? 0x1000 : 0);
        }
        tilefetched += offset;
        //now load up the shift registers for said sprite
        final boolean hflip = ((oamextra & (utils.BIT6)) != 0);
        if (!hflip) {
            spriteshiftregL[found] = reverseByte(mapper.ppuRead(tilefetched));
            spriteshiftregH[found] = reverseByte(mapper.ppuRead(tilefetched + 8));
        } else {
            spriteshiftregL[found] = mapper.ppuRead(tilefetched);
            spriteshiftregH[found] = mapper.ppuRead(tilefetched + 8);
        }
    }

    /**
     * draws appropriate pixel of the sprites selected by sprite evaluation
     */
    private void drawSprites(int bufferoffset, int x, boolean bgflag) {
        final int startdraw = !spriteClip ? 0 : 8;//sprite left 8 pixels clip
        int sprpxl = 0;
        int index = 7;
        //per pixel in de line that could have a sprite
        for (int y = found - 1; y >= 0; --y) {
            int off = x - spriteXlatch[y];
            if (off >= 0 && off <= 8) {
                if ((spriteshiftregH[y] & 1) + (spriteshiftregL[y] & 1) != 0) {
                    index = y;
                    sprpxl = 2 * (spriteshiftregH[y] & 1) + (spriteshiftregL[y] & 1);
                }
                spriteshiftregH[y] >>= 1;
                spriteshiftregL[y] >>= 1;
            }
        }
        if (sprpxl == 0 || x < startdraw || !spritesOn) {
            //no opaque sprite pixel here
            return;
        }

        if (sprite0here && (index == 0) && !bgflag
                && x < 255) {
            //sprite 0 hit!
            sprite0hit = true;
        }
        //now, FINALLY, drawing.
        if (!spritebgflags[index] || bgflag) {
            bitmap[bufferoffset + x] = pal[spritepals[index] + sprpxl];
        }
    }

    /**
     * Read the appropriate color attribute byte for the current tile. this is
     * fetched 2x as often as it really needs to be, the MMC5 takes advantage of
     * that for ExGrafix mode.
     *
     * @param ntstart //start of the current attribute table
     * @param tileX //x position of tile (0-31)
     * @param tileY //y position of tile (0-29)
     * @return attribute table value (0-3)
     */
    private int getAttribute(final int ntstart, final int tileX, final int tileY) {
        final int base = mapper.ppuRead(ntstart + (tileX >> 2) + 8 * (tileY >> 2));
        if (((tileY & (utils.BIT1)) != 0)) {
            if (((tileX & (utils.BIT1)) != 0)) {
                return (base >> 6) & 3;
            } else {
                return (base >> 4) & 3;
            }
        } else if (((tileX & (utils.BIT1)) != 0)) {
            return (base >> 2) & 3;
        } else {
            return base & 3;
        }
    }

    /**
     * draw all 4 nametables/tileset/pallette to debug window. (for the
     * nametable viewer)
     */
    private void debugDraw() {
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 30; ++j) {
                nametableView.setRGB(i * 8, j * 8, 8, 8,
                        debugGetTile(mapper.ppuRead(0x2000 + i + 32 * j) * 16
                                + (bgpattern ? 0x1000 : 0)), 0, 8);
            }
        }
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 30; ++j) {
                nametableView.setRGB(i * 8 + 255, j * 8, 8, 8,
                        debugGetTile(mapper.ppuRead(0x2400 + i + 32 * j) * 16
                                + (bgpattern ? 0x1000 : 0)), 0, 8);
            }
        }
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 30; ++j) {
                nametableView.setRGB(i * 8, j * 8 + 239, 8, 8,
                        debugGetTile(mapper.ppuRead(0x2800 + i + 32 * j) * 16
                                + (bgpattern ? 0x1000 : 0)), 0, 8);
            }
        }
        for (int i = 0; i < 32; ++i) {
            for (int j = 0; j < 30; ++j) {
                nametableView.setRGB(i * 8 + 255, j * 8 + 239, 8, 8,
                        debugGetTile(mapper.ppuRead(0x2C00 + i + 32 * j) * 16
                                + (bgpattern ? 0x1000 : 0)), 0, 8);
            }
        }

        //draw the tileset
//        for (int i = 0; i < 16; ++i) {
//            for (int j = 0; j < 32; ++j) {
//                nametableView.setRGB(i * 8, j * 8, 8, 8,
//                        debugGetTile((i + 16 * j) * 16), 0, 8);
//            }
//        }
        //draw the palettes on the bottom.
//        for (int i = 0; i < 32; ++i) {
//            for (int j = 0; j < 16; ++j) {
//                for (int k = 0; k < 16; ++k) {
//                    nametableView.setRGB(j + i * 16, k + 256, nescolor[0][pal[i]]);
//                }
//            }
//        }
        debuggui.setFrame(nametableView);
        //debugbuff.clear();
    }

    /**
     * Fetches 8x8 NES tile stored at the given offset. This is an artifact of
     * the first renderer I wrote which drew 8 scanlines at a time.
     *
     * @return an 8x8 array with colors stored as RGB packed in int
     */
    private int[] debugGetTile(final int offset) {
        //read one whole tile from nametable and convert from bitplane to packed
        //only used for debugging
        int[] dat = new int[64];
        for (int i = 0; i < 8; ++i) {
            //per line of tile ( 1 byte)
            for (int j = 0; j < 8; ++j) {
                //per pixel(1 bit)
                dat[8 * i + j]
                        = ((((mapper.ppuRead(i + offset) & (utils.BIT7 - j)) != 0))
                        ? 0x555555 : 0)
                        + ((((mapper.ppuRead(i + offset + 8) & (utils.BIT7 - j)) != 0))
                        ? 0xaaaaaa : 0);
            }
        }
        return dat;
    }

    /**
     * Sends off a frame of NES video to be rendered by the GUI. also includes
     * dot crawl flag and BG color to be displayed around edges which are needed
     * for the NTSC renderer.
     *
     * @param gui the GUI window to render to
     */
    public final void renderFrame(GUIInterface gui) {
        if (PPUDEBUG) {
            debugDraw();
        }
        if (gui != null) {
            gui.setFrame(bitmap, bgcolors, dotcrawl);
        }

    }
}
