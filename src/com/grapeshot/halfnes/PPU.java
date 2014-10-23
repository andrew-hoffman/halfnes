package com.grapeshot.halfnes;
//HalfNES by Andrew Hoffman

import com.grapeshot.halfnes.mappers.Mapper;
import com.grapeshot.halfnes.ui.DebugUI;
import com.grapeshot.halfnes.ui.GUIInterface;
import static com.grapeshot.halfnes.utils.getbit;
import static com.grapeshot.halfnes.utils.getbitI;
import static com.grapeshot.halfnes.utils.reverseByte;
import static com.grapeshot.halfnes.utils.setbit;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class PPU {

    public Mapper mapper;
    private int oamaddr, readbuffer = 0;
    private int loopyV = 0x0;//ppu memory pointer
    private int loopyT = 0x0;//temp pointer
    private int loopyX = 0;//fine x scroll
    public int scanline = 0;
    public int cycles = 0;
    private int framecount = 0;
    private int div = 2;
    private final int[] OAM = new int[256], spriteshiftregH = new int[8],
            spriteshiftregL = new int[8], spriteXlatch = new int[8],
            spritepals = new int[8], bitmap = new int[240 * 256];
    int bgShiftRegH, bgShiftRegL, bgAttrShiftRegH, bgAttrShiftRegL;
    private final boolean[] spritebgflags = new boolean[8];
    private boolean even = true, bgpattern = true, sprpattern = false;
    private final int[] ppuregs = new int[0x8];
    public final int[] pal = {0x09, 0x01, 0x00, 0x01, 0x00, 0x02, 0x02, 0x0D,
        0x08, 0x10, 0x08, 0x24, 0x00, 0x00, 0x04, 0x2C, 0x09, 0x01, 0x34, 0x03,
        0x00, 0x04, 0x00, 0x14, 0x08, 0x3A, 0x00, 0x02, 0x00, 0x20, 0x2C, 0x08};
    /*
     power-up pallette checked by Blargg's power_up_palette test. Different
     revs of NES PPU might give different initial results but there's a test
     expecting this set of values and nesemu1, BizHawk, RockNES, MyNes use it
     */
    private DebugUI debuggui;
    private int vraminc = 1;
    private final static boolean PPUDEBUG = PrefsSingleton.get().getBoolean("ntView", false);
    private BufferedImage nametableView;
    private final int[] bgcolors = new int[256];
    private int openbus = 0; //the last value written to the PPU
    private int nextattr;
    private int linelowbits;
    private int linehighbits;
    private int penultimateattr;

    public PPU(final Mapper mapper) {
        this.mapper = mapper;
        Arrays.fill(OAM, 0xff);
        Arrays.fill(ppuregs, 0x00);
        if (PPUDEBUG) {
            nametableView = new BufferedImage(512, 480, BufferedImage.TYPE_INT_BGR);
            debuggui = new DebugUI(512, 480);
            debuggui.run();
        }
    }

    /**
     * Performs a read from a PPU register, as well as causes any side effects
     * of reading that specific register.
     *
     * @param regnum
     * @return the data in the PPU register, or open bus (the last value written
     * to a PPU register) if the register is read only
     */
    public final int read(final int regnum) {
        switch (regnum) {
            case 2:
                even = true;
                if (scanline == 241) {
                    if (cycles == 1) {//suppress NMI flag if it was just turned on this same cycle
                        setvblankflag(false);
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
                final int tmp = (ppuregs[2] & ~0x1f) + (openbus & 0x1f);
                setvblankflag(false);
                openbus = tmp;
                break;
            case 4:
                // reading this is NOT reliable but some games do it anyways
                openbus = OAM[oamaddr];
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
                if (!ppuIsOn() || (scanline > 240 && scanline < 261)) {
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
        //System.err.println("PPU write - wrote " + data + " to reg " + regnum);
        //debugdraw();
        openbus = data;
        switch (regnum) {
            case 0:
                ppuregs[0] = data;
                vraminc = (getbit(data, 2) ? 32 : 1);
                //set 2 bits of vram address (nametable select)
                loopyT &= ~0xc00;
                loopyT += (data & 3) << 10;
                /*
                 SMB1 writes here at the end of its main loop and if this write
                 lands on one exact PPU clock, the address bits are set to 0.
                 This only happens on one CPU/PPU alignment of real hardware 
                 though so it only shows up ~33% of the time.
                 */
                break;
            case 1:
                ppuregs[1] = data;
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
                    loopyT += data >> 3;

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
                    loopyT += ((data & 0x3f) << 8);
                    loopyT &= 0x3fff;
                    even = false;
                } else {
                    loopyT &= 0xff00;
                    loopyT += data;
                    loopyV = loopyT;
                    even = true;
                }
                break;
            case 7:
                // PPUDATA
                mapper.ppuWrite((loopyV & 0x3fff), data);
                if (!ppuIsOn() || (scanline > 240 && scanline < 261)) {
                    loopyV += vraminc;
                } else {
                    //if 2007 is read during rendering PPU increments both horiz
                    //and vert counters erroneously.
                    if (((cycles - 1) & 7) != 7) {
                        incLoopyVHoriz();
                        incLoopyVVert();
                    }
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
    public boolean ppuIsOn() {
        return getbit(ppuregs[1], 3) || getbit(ppuregs[1], 4);
    }

    /**
     * MMC3 scan line counter isn't clocked if background and sprites are using
     * the same half of the pattern table
     *
     * @return true if PPU is rendering and BG and sprites are using different
     * pattern tables
     */
    public final boolean mmc3CounterClocking() {
        return (bgpattern != sprpattern) && ppuIsOn();
    }

    /**
     * Runs the PPU emulation for one NES scan line.
     *
     * @param scanline
     */
    public final void clockLine(int scanline) {
        //skip a PPU clock on line 0 of odd frames when rendering is on
        int skip = (scanline == 0
                && getbit(ppuregs[1], 3)
                && !getbit(framecount, 1)) ? 1 : 0;
        for (cycles = skip; cycles < 341; ++cycles) {
            clock();
        }
    }

    private int tileAddr = 0;

    /**
     * runs the emulation for one PPU clock cycle.
     */
    public final void clock() {
        bgpattern = getbit(ppuregs[0], 4);
        sprpattern = getbit(ppuregs[0], 3);
        //cycle based ppu stuff will go here
        if (cycles == 1) {
            if (scanline == 0) {
                dotcrawl = ppuIsOn();
            }
            if (scanline < 240) {
                bgcolors[scanline] = pal[0];
            }
        }
        if (scanline < 240 || scanline == 261) {
            //on all rendering lines
            if (ppuIsOn()
                    && ((cycles >= 1 && cycles <= 256)
                    || (cycles >= 321 && cycles <= 336))) {
                //fetch background tiles, load shift registers
                bgFetch();
            } else if (cycles == 257 && ppuIsOn()) {
                //horizontal bits of loopyV = loopyT
                loopyV &= ~0x41f;
                loopyV |= loopyT & 0x41f;

            } else if (cycles > 257 && cycles <= 341) {
                //clear the oam address from pxls 257-341 continuously
                ppuregs[3] = 0;
            }
            if ((cycles == 340) && ppuIsOn()) {
                //read the same nametable byte twice
                //this signals the MMC5 to increment the scanline counter
                fetchNTByte();
                fetchNTByte();
            }
            if (cycles == 260 && ppuIsOn()) {
                //evaluate sprites for NEXT scanline (as long as either background or sprites are enabled)
                //this does in fact happen on scanine 261 but it doesn't do anything useful
                //it's cycle 260 because that's when the first important sprite byte is read
                //actually sprite overflow should be set by sprite eval somewhat before
                //so this needs to be split into 2 parts, the eval and the data fetches
                evalSprites();
            }
            if (scanline == 261) {
                if (cycles == 0) {// turn off vblank, sprite 0, sprite overflow flags
                    setvblankflag(false);
                    ppuregs[2] &= 0x9F;
                } else if (cycles >= 280 && cycles <= 304 && ppuIsOn()) {
                    //loopyV = (all of)loopyT for each of these cycles
                    loopyV = loopyT;
                }
            }
        } else if (scanline == 241 && cycles == 1) {
            //handle vblank on / off
            setvblankflag(true);
        }
        if (!ppuIsOn() || (scanline > 240 && scanline < 261)) {
            //HACK ALERT
            //handle the case of MMC3 mapper watching A12 toggle
            //even when read or write aren't asserted on the bus
            //needed to pass Blargg's mmc3 tests
            mapper.checkA12(loopyV & 0x3fff);
        }
        if (scanline < 240) {
            if (cycles >= 1 && cycles <= 256) {
                int bufferoffset = (scanline << 8) + (cycles - 1);
                //bg drawing
                if (getbit(ppuregs[1], 3)) { //if background is on, draw a line of that
                    final boolean isBG = drawBGPixel(bufferoffset);
                    //sprite drawing
                    drawSprites(scanline << 8, cycles - 1, isBG);

                } else {
                    //rendering is off, so draw either the background color OR
                    //if the PPU address points to the palette, draw that color instead.
                    int bgcolor = ((loopyV > 0x3f00 && loopyV < 0x3fff) ? mapper.ppuRead(loopyV) : pal[0]);
                    bitmap[bufferoffset] = bgcolor;
                }
                //deal with the grayscale flag
                if (getbit(ppuregs[1], 0)) {
                    bitmap[bufferoffset] &= 0x30;
                }
                //handle color emphasis
                final int emph = (ppuregs[1] & 0xe0) << 1;
                bitmap[bufferoffset] = bitmap[bufferoffset] & 0x3f | emph;

            }
        }
        //handle nmi
        if (vblankflag && getbit(ppuregs[0], 7)) {
            //pull NMI line on when conditions are right
            mapper.cpu.setNMI(true);
        } else {
            mapper.cpu.setNMI(false);
        }

        //clock CPU, once every 3 ppu cycles
        div = (div + 1) % 3;
        if (div == 0) {
            mapper.cpu.runcycle(scanline, cycles);
            mapper.cpucycle(1);
        }
        if (cycles == 257) {
            mapper.notifyscanline(scanline);
        } else if (cycles == 340) {
            scanline = (scanline + 1) % 262;
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
        int newfinescroll = (loopyV & 0x7000) + 0x1000;
        loopyV &= ~0x7000;
        if (newfinescroll > 0x7000) {
            //reset the fine scroll bits and increment tile address to next row
            loopyV += 32;
        } else {
            //increment the fine scroll
            loopyV += newfinescroll;
        }
        if (((loopyV >> 5) & 0x1f) == 30) {
            //if incrementing loopy_v to the next row pushes us into the next
            //nametable, zero the "row" bits and go to next nametable
            loopyV &= ~0x3e0;
            loopyV ^= 0x800;
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
        if (!getbit(ppuregs[1], 1) && (bufferoffset & 0xff) < 8) {
            //left hand of screen clipping
            //(needs to be marked as BG and not cause a sprite hit)
            bitmap[bufferoffset] = pal[0];
            isBG = true;
        } else {
            final int bgPix = (getbitI(bgShiftRegH, -loopyX + 16) << 1)
                    + getbitI(bgShiftRegL, -loopyX + 16);
            final int bgPal = (getbitI(bgAttrShiftRegH, -loopyX + 8) << 1)
                    + getbitI(bgAttrShiftRegL, -loopyX + 8);
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
    private int off, y, index, sprpxl, found;
    private boolean sprite0here = false;

    /**
     * evaluates PPU sprites for the NEXT scanline
     */
    private void evalSprites() {
        sprite0here = false;
        bgpattern = getbit(ppuregs[0], 4);
        sprpattern = getbit(ppuregs[0], 3);
        int ypos, offset, tilefetched;
        found = 0;
        final boolean spritesize = getbit(ppuregs[0], 5);
        //primary evaluation
        //need to emulate behavior when OAM address is set to nonzero here
        for (int spritestart = 0; spritestart < 255; spritestart += 4) {
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
                ppuregs[2] |= 0x20;
                break; //also the real PPU does strange stuff on sprite overflow.
            } else {
                //set up ye sprite for rendering
                final int oamextra = OAM[spritestart + 2];
                //bg flag
                spritebgflags[found] = getbit(oamextra, 5);
                //x value
                spriteXlatch[found] = OAM[spritestart + 3];
                spritepals[found] = ((oamextra & 3) + 4) * 4;
                if (getbit(oamextra, 7)) {
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
        final boolean hflip = getbit(oamextra, 6);
        if (!hflip) {
            spriteshiftregL[found] = reverseByte(mapper.ppuRead(tilefetched));
            spriteshiftregH[found] = reverseByte(mapper.ppuRead(tilefetched + 8));
        } else {
            spriteshiftregL[found] = mapper.ppuRead(tilefetched);
            spriteshiftregH[found] = mapper.ppuRead(tilefetched + 8);
        }
    }

    /**
     * draws appropriate lines of the sprites selected by sprite evaluation
     */
    private void drawSprites(int bufferoffset, int x, boolean bgflag) {
        final int startdraw = getbit(ppuregs[1], 2) ? 0 : 8;//sprite left 8 pixels clip
        sprpxl = 0;
        index = 7;
        //per pixel in de line that could have a sprite
        for (y = found - 1; y >= 0; --y) {
            off = x - spriteXlatch[y];
            if (off >= 0 && off <= 8) {
                if ((spriteshiftregH[y] & 1) + (spriteshiftregL[y] & 1) != 0) {
                    index = y;
                    sprpxl = 2 * (spriteshiftregH[y] & 1) + (spriteshiftregL[y] & 1);
                }
                spriteshiftregH[y] >>= 1;
                spriteshiftregL[y] >>= 1;
            }
        }
        if (sprpxl == 0 || x < startdraw || !getbit(ppuregs[1], 4)) {
            //no opaque sprite pixel here
            return;
        }

        if (sprite0here && (index == 0) && !bgflag
                && x < 255) {
            //sprite 0 hit!
            ppuregs[2] |= 0x40;
            //ppuregs[1] |= 1;//debug
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
        if (getbit(tileY, 1)) {
            if (getbit(tileX, 1)) {
                return (base >> 6) & 3;
            } else {
                return (base >> 4) & 3;
            }
        } else {
            if (getbit(tileX, 1)) {
                return (base >> 2) & 3;
            } else {
                return base & 3;
            }
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
     * @param offset
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
                        = ((getbit(mapper.ppuRead(i + offset), 7 - j))
                        ? 0x555555 : 0)
                        + ((getbit(mapper.ppuRead(i + offset + 8), 7 - j))
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
        gui.setFrame(bitmap, bgcolors, dotcrawl);

    }
    private boolean vblankflag = false;

    /**
     * Sets both the internal PPU vblank flag and the one visible to the NES
     *
     * @param b value of the flag
     */
    private void setvblankflag(boolean b) {
        vblankflag = b;
        ppuregs[2] = setbit(ppuregs[2], 7, b);
    }
}
