package com.grapeshot.halfnes;
//HalfNES, Copyright Andrew Hoffman, October 2010

import static com.grapeshot.halfnes.utils.*;
import com.grapeshot.halfnes.ui.GUIInterface;
import com.grapeshot.halfnes.ui.DebugUI;
import com.grapeshot.halfnes.mappers.Mapper;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public final class PPU {

    public Mapper mapper;
    private int oamaddr, sprite0x, readbuffer = 0;
    private int loopyV = 0x0;//ppu memory pointer
    private int loopyT = 0x0;//temp pointer
    private int loopyX = 0;//fine x scroll
    private int scanline = 0;
    private boolean firstcycle;
    private int framecount = 0;
    private int div = 2;
    private final int[] OAM = new int[256], spriteshiftregH = new int[8],
            spriteshiftregL = new int[8], spriteXlatch = new int[8],
            spritepals = new int[8], bitmap = new int[240 * 256];
    private final boolean[] spritebgflags = new boolean[8];
    private boolean sprite0hit = false, even = true, bgpattern = true, sprpattern = false;
    private int ppureg0, ppureg1, ppureg2, ppureg3;
    public final int[] pal = //power-up pallette verified by Blargg's power_up_palette test 
            {0x09, 0x01, 0x00, 0x01, 0x00, 0x02, 0x02, 0x0D, 0x08, 0x10, 0x08, 0x24, 0x00, 0x00, //palette *might* be different on every NES
                0x04, 0x2C, 0x09, 0x01, 0x34, 0x03, 0x00, 0x04, 0x00, 0x14, 0x08, 0x3A, 0x00, 0x02, //but some emulators (nesemu1, BizHawk, RockNES, MyNes)
                0x00, 0x20, 0x2C, 0x08};    //use it anyway
    public final int[] packedpal = new int[8];
    private DebugUI debuggui;
    private int vraminc = 1;
    private final static boolean PPUDEBUG = PrefsSingleton.get().getBoolean("ntView", false);
    private BufferedImage nametableView;
    private final int[] bgcolors = new int[256];
    private int openbus = 0; //the last value written to the PPU

    public PPU(final Mapper mapper) {
        this.mapper = mapper;
        Arrays.fill(OAM, 0xff);
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
                    if (firstcycle) {//suppress NMI flag if it was just turned on this same cycle
                        clearvblankflag();
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
                final int tmp = (ppureg2 & ~0x1f) + (openbus & 0x1f);
                clearvblankflag();
                return openbus = tmp;
            case 4:
                // reading this is NOT reliable but some games do it anyways
                return openbus = OAM[oamaddr];
            case 7:
                // PPUDATA
                // correct behavior. read is delayed by one
                // -unless- is a read from sprite pallettes
                if ((loopyV & 0x3fff) < 0x3f00) {
                    final int temp = readbuffer;
                    readbuffer = mapper.ppuRead(loopyV & 0x3fff);
                    loopyV += vraminc;
                    return openbus = temp;
                } else {
                    readbuffer = mapper.ppuRead((loopyV & 0x3fff) - 0x1000);
                    final int temp = mapper.ppuRead(loopyV);
                    loopyV += vraminc;
                    return openbus = temp;
                }

            // and don't increment on read
            default:
                return openbus; // last value written to ppu 
        }
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
                ppureg0 = data;
                vraminc = ((data & BIT2) != 0 ? 32 : 1);
                //set 2 bits of vram address (nametable select)
                loopyT &= ~0xc00;
                loopyT += (data & 3) << 10;
                //ppureg1 ^= 0xe0; //DEBUG: why does SMB write $2000 in midframe?
                break;
            case 1:
                ppureg1 = data;
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
                // games don't write this directly anyway
                break;

            // PPUSCROLL(2005)
            case 5:
                if (even) {
                    // horizontal scroll
                    loopyT &= ~0x1f;
                    loopyX = data & 7;
                    loopyT += data >> 3;

                    even = false;
                } else {
                    // vertical scroll
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
                loopyV += vraminc;
                // increments on write but NOT on read
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
    private boolean ppuIsOn() {
        return (ppureg1 & (BIT3 | BIT4)) != 0;
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
     * Runs the PPU emulation for the entire screen.
     *
     * @param scanline
     */
    public final void draw(boolean render) {
        firstcycle = true;
        for (scanline = 0; scanline < 262; ++scanline) {
            //skip a PPU clock on line 0 of odd frames when rendering is on
            final int skip = scanline == 0
                    && (ppureg1 & BIT3) != 0
                    && (framecount & BIT1) == 0 ? 1 : 0;
            drawLine(render);
            for (int cycle = skip; cycle < 341; ++cycle) {
                //handle sprite 0
                if (sprite0hit && sprite0x == (cycle + 1)) {
                    sprite0hit = false;
                    ppureg2 |= 0x40;
                }
                //handle vblank on / off
                if (scanline < 240 || scanline == 261) {
                    //on all rendering lines
                    if (cycle >= 257 && cycle <= 341) {
                        //clear the oam address from pxls 257-341 continuously
                        ppureg3 = 0;
                    }
                    if (scanline == 261 && cycle == 0) {
                        // turn off vblank, sprite 0, sprite overflow flags
                        clearvblankflag();
                        ppureg2 &= 0x9F;
                    }
                } else if (scanline == 241 && cycle == 1) {
                    markvblankflag();
                }
                //handle nmi
                if (vblankflag && (ppureg0 & utils.BIT7) != 0) {
                    //pull NMI line on when conditions are right
                    mapper.cpu.setNMI(true);
                } else {
                    mapper.cpu.setNMI(false);
                }

                //clock CPU, once every 3 ppu cycle
                if (++div == 3) {
                    div = 0;
                    mapper.cpu.runcycle(scanline, cycle);
                }
                if (cycle == 257) {
                    mapper.notifyscanline(scanline);
                }
                firstcycle = false;
            }
        }
        ++framecount;
    }

    int bgcolor;
    boolean dotcrawl = true;

    /**
     * Causes the PPU to draw one line of video.
     */
    private void drawLine(boolean render) {
        //System.err.println("SCANLINE " + scanline);
        //this contains probably more magic numbers than the rest of the program combined.
        //TODO: define some static bitmasks to manipulate the address through, instead
        if (scanline == 0) {
            dotcrawl = ppuIsOn();
        }
        if (scanline >= 240) {
            return;
        }
        final int bufferoffset = scanline << 8;
        if (render) {
            bgpattern = (ppureg0 & utils.BIT4) != 0;
            sprpattern = (ppureg0 & utils.BIT3) != 0;
            //TODO: Simplify Logic
            bgcolor = pal[0] + 256; //plus 256 is to give indication it IS the bgcolor
            //because bg color is special
            bgcolors[scanline] = pal[0];
            computePackedpal();
            if ((ppureg1 & utils.BIT3) != 0) { //if background is on, draw a line of that
                drawBG(bufferoffset);
            } else {
                //rendering is off, so draw either the background color OR
                //if the PPU address points to the palette, draw that color instead.
                bgcolor = ((loopyV > 0x3f00 && loopyV < 0x3fff) ? mapper.ppuRead(loopyV) : pal[0]);
                Arrays.fill(bitmap, bufferoffset, bufferoffset + 256, bgcolor);
            }
        }
        //draw sprites on top of whatever we had
        drawSprites(render);
        //evaluate sprites for NEXT scanline (as long as either background or sprites are enabled)
        if (ppuIsOn()) {
            evalSprites(render);
        }
        if (render) {
            //deal with the grayscale flag
            if ((ppureg1 & utils.BIT0) != 0) {
                for (int i = bufferoffset; i < (bufferoffset + 256); ++i) {
                    bitmap[i] &= 0x30;
                }
            }
            //handle color emphasis
            final int emph = (ppureg1 & 0xe0) << 1;
            for (int i = bufferoffset; i < (bufferoffset + 256); ++i) {
                bitmap[i] = bitmap[i] & 0x3f | emph;
            }
        }
    }

    /**
     * Draws one line of PPU backgrounds
     *
     * @param bufferoffset where in the video buffer to draw to (should be a
     * multiple of 256)
     */
    private void drawBG(final int bufferoffset) {
        // if bg is on, draw tiles.
        if (scanline == 0) {
            //update whole scroll
            loopyV = loopyT;
        } else {
            //update horizontal scroll bits only
            //actually some of these may be updated DURING the line
            loopyV &= ~0x41f;
            loopyV |= loopyT & 0x41f;
        }
        //draw background
        int ntoffset = (loopyV & 0xc00) | 0x2000;
        int attroffset = ntoffset + 0x3c0;
        boolean horizWrap = false;
        int xpos = -loopyX;
        final int ntcheckoff = (((loopyV & 0x1f) << 3) + loopyX);
        final int bgpatternoff = (bgpattern ? 0x1000 : 0);
        final int numtiles = loopyX == 0 ? 32 : 33;
        final int loopyVOff = (loopyV & 0x7000) >> 12;
        for (int tilenum = 0; tilenum < numtiles; ++tilenum, xpos += 8) {
            //for each tile in row
            if ((tilenum * 8 + ntcheckoff) > 255
                    && !horizWrap) {
                //if scrolling off the side of the nametable, bump address to next nametable
                ntoffset ^= 0x400;
                ntoffset -= 32;
                attroffset ^= 0x400;
                horizWrap = true;
            }
            final int tileaddr = mapper.ppuRead(ntoffset
                    + (loopyV & 0x3ff) + tilenum) * 16
                    + bgpatternoff;
            //get palette number from attribute table byte
            final int palettenum = getAttribute(attroffset, (loopyV + tilenum) & 0x1f,
                    (((ntoffset + loopyV + tilenum) & 0x3e0) >> 5));
            final int[] tile = getTile(tileaddr, palettenum, loopyVOff);
            //now put inna buffer
            if (tilenum == 0 && loopyX > 0) {
                System.arraycopy(tile, loopyX, bitmap, xpos + bufferoffset + loopyX, 8 - loopyX);
            } else if (tilenum == 32 && loopyX > 0) {
                System.arraycopy(tile, 0, bitmap, xpos + bufferoffset, loopyX);
            } else {
                System.arraycopy(tile, 0, bitmap, xpos + bufferoffset, 8);
            }
        }
        //increment loopy_v to next row of tiles
        int newfinescroll = loopyV & 0x7000;
        newfinescroll += 0x1000;
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
            //ntoffset += 0x440;
            //attroffset += 0x7c0;
        }
        //hide leftmost 8 pixels if that flag is on
        if ((ppureg1 & BIT1) == 0) {
            for (int i = 0; i < 8; ++i) {
                bitmap[i + bufferoffset] = bgcolor;
            }
        }
    }
    private int off, y, index, sprpxl, found;
    private boolean sprite0here = false;

    /**
     * evaluates PPU sprites for the NEXT scanline
     */
    private void evalSprites(boolean render) {
        sprite0here = false;
        bgpattern = (ppureg0 & BIT4) != 0;
        sprpattern = (ppureg0 & BIT3) != 0;
        int ypos, offset, tilefetched;
        found = 0;
        final boolean spritesize = (ppureg0 & BIT5) != 0;
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
                ppureg2 |= 0x20;
                break; //also the real PPU does strange stuff on sprite overflow.
            } else {
                //set up ye sprite for rendering
                final int oamextra = OAM[spritestart + 2];
                //bg flag
                spritebgflags[found] = (oamextra & BIT5) != 0;
                //x value
                spriteXlatch[found] = OAM[spritestart + 3];
                spritepals[found] = ((oamextra & 3) + 4) * 4;
                if ((oamextra & BIT7) != 0) {
                    //if sprite is flipped vertically, reverse the offset
                    offset = (spritesize ? 15 : 7) - offset;
                }
                //now correction for the fact that 8x16 tiles are 2 separate tiles
                if (offset > 7) {
                    offset += 8;
                }
                //get tile address (8x16 sprites can use both pattern tbl pages but only the even tiles)
                final int tilenum = OAM[spritestart + 1];
                if (spritesize) {
                    tilefetched = ((tilenum & 1) * 0x1000)
                            + (tilenum & 0xfe) * 16;
                } else {
                    tilefetched = tilenum * 16
                            + ((sprpattern) ? 0x1000 : 0);
                }
                tilefetched += offset;
                //now load up the shift registers for said sprite
                final boolean hflip = (oamextra & BIT6) != 0;
                if (!hflip) {
                    spriteshiftregL[found] = reverseByte(mapper.ppuRead(tilefetched));
                    spriteshiftregH[found] = reverseByte(mapper.ppuRead(tilefetched + 8));
                } else {
                    spriteshiftregL[found] = mapper.ppuRead(tilefetched);
                    spriteshiftregH[found] = mapper.ppuRead(tilefetched + 8);
                }
                ++found;
            }
        }
        for (int i = found; i < 8; ++i) {
            //fill unused sprite registers with zeros
            spriteshiftregL[i] = 0;
            spriteshiftregH[i] = 0;
        }
    }

    /**
     * draws appropriate lines of the sprites selected by sprite evaluation
     */
    private void drawSprites(boolean render) {
        //profiler doesn't see how slow it is though. fix that!
        if (found == 0) {
            //no sprites to draw on line.
            return;
        }
        final int bufferoffset = scanline << 8;
        final int startdraw = (ppureg1 & BIT2) != 0 ? 0 : 8;//sprite left 8 pixels clip
        for (int x = 0; x < 256; ++x) {
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
            if (sprpxl == 0 || x < startdraw || (ppureg1 & BIT4) == 0) {
                //no opaque sprite pixel here
                continue;
            }

            if (sprite0here && (index == 0) && bitmap[bufferoffset + x] != bgcolor
                    && x < 255) {
                //sprite 0 hit!
                sprite0hit = true;
                sprite0x = x;
                // ppureg1 |= 1;//debug
            }
            //now, FINALLY, drawing.
            if ((!spritebgflags[index] || (bitmap[bufferoffset + x] == bgcolor)) && render) {
                bitmap[bufferoffset + x] = pal[spritepals[index] + sprpxl];
            }
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
        if ((tileY & utils.BIT1) != 0) {
            if ((tileX & utils.BIT1) != 0) {
                return (base >> 6) & 3;
            } else {
                return (base >> 4) & 3;
            }
        } else {
            if ((tileX & utils.BIT1) != 0) {
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
    private final int[] tiledata = new int[8];
    private final int[] tilepal = new int[4];

    /**
     * Returns an 8 pixel line of tile data from given PPU ram location with
     * given offset and given palette. (color expressed as NES color number)
     *
     * NES fetches for each tile are done in the order of: Nametable byte,
     * Attribute table byte, tile data high bits, tile data low bits
     *
     * @param tileptr
     * @param paletteindex which of the 8 3 color plus BG color palettes to use
     * @param off PPU base address
     * @return int array with 8 NES color numbers
     */
    private int[] getTile(final int tileptr, final int palettenum, final int off) {
        final int tilepal = packedpal[palettenum];
        final int linelowbits = mapper.ppuRead(off + tileptr);
        final int linehighbits = mapper.ppuRead(off + tileptr + 8) << 1;
        tiledata[7] = tilepal >>> ((linehighbits & 0x2 | linelowbits & 0x1) << 3) & 0xFF;
        tiledata[6] = tilepal >>> ((linehighbits & 0x4 | linelowbits & 0x2) << 2) & 0xFF;
        tiledata[5] = tilepal >>> ((linehighbits & 0x8 | linelowbits & 0x4) << 1) & 0xFF;
        tiledata[4] = tilepal >>> ((linehighbits & 0x10 | linelowbits & 0x8)) & 0xFF;
        tiledata[3] = tilepal >>> ((linehighbits & 0x20 | linelowbits & 0x10) >> 1) & 0xFF;
        tiledata[2] = tilepal >>> ((linehighbits & 0x40 | linelowbits & 0x20) >> 2) & 0xFF;
        tiledata[1] = tilepal >>> ((linehighbits & 0x80 | linelowbits & 0x40) >> 3) & 0xFF;
        tiledata[0] = tilepal >>> ((linehighbits & 0x100 | linelowbits & 0x80) >> 4) & 0xFF;
        return tiledata;
    }

    private boolean vblankflag = false;

    /**
     * Marks both the internal PPU vblank flag and the one visible to the NES
     *
     * @param b value of the flag
     */
    private void markvblankflag() {
        vblankflag = true;
        ppureg2 |= BIT7;
    }

    /**
     * Clears both the internal PPU vblank flag and the one visible to the NES
     *
     * @param b value of the flag
     */
    private void clearvblankflag() {
        vblankflag = false;
        ppureg2 &= ~BIT7;
    }

    private void computePackedpal() {
        for (int i=0; i<8; i++) {
            packedpal[i] = pal[0]
                | pal[i*4 + 1] << 8
                | pal[i*4 + 2] << 16
                | pal[i*4 + 3] << 24;
        }
    }
}
