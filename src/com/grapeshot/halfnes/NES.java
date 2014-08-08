package com.grapeshot.halfnes;

import com.grapeshot.halfnes.ui.*;
import com.grapeshot.halfnes.cheats.ActionReplay;
import com.grapeshot.halfnes.mappers.BadMapperException;
import com.grapeshot.halfnes.mappers.Mapper;

import java.util.prefs.Preferences;

/**
 *
 * @author Andrew Hoffman
 */
public class NES {

    private Mapper mapper;
    private APU apu;
    private CPU cpu;
    private CPURAM cpuram;
    private PPU ppu;
    private ControllerInterface controller1, controller2;
    final public static String VERSION = "058";
    public boolean runEmulation = false;
    private boolean dontSleep = false;
    public long frameStartTime, framecount, frameDoneTime;
    private boolean frameLimiterOn = true;
    private String curRomPath, curRomName;
    private final GUIInterface gui = new GUIImpl(this);
    private final FrameLimiterInterface limiter = new FrameLimiterImpl(this);
    // Pro Action Replay device
    private ActionReplay actionReplay;

    public NES() {
        try {
            java.awt.EventQueue.invokeAndWait(gui);
        } catch (InterruptedException e) {
            System.err.println("Could not initialize GUI. Exiting.");
            System.exit(-1);
        } catch (java.lang.reflect.InvocationTargetException f) {
            System.err.println(f.getCause().toString());
            //not sure how this could happen (thrown if run method causes exception)
            System.exit(-1);
        }
    }

    public void run(final String romtoload) {
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
        //set thread priority higher than the interface thread
        curRomPath = romtoload;
        loadROM(romtoload);
        run();
    }

    public void run() {
        while (true) {
            if (runEmulation) {
                frameStartTime = System.nanoTime();
                actionReplay.applyPatches();
                runframe();
                if (frameLimiterOn && !dontSleep) {
                    limiter.sleep();
                }
                frameDoneTime = System.nanoTime() - frameStartTime;
            } else {
                limiter.sleepFixed();
                if (ppu != null && framecount > 1) {
                    java.awt.EventQueue.invokeLater(render);
                }
            }
        }
    }
    Runnable render = new Runnable() {
        @Override
        public void run() {
            gui.render();
        }
    };

    private synchronized void runframe() {
        //the main method sequencing everything that has to happen in the nes each frame
        //loops unrolled a bit to avoid some conditionals every cycle
        for (int scanline = 0; scanline <= 240; ++scanline) {
            runLine(scanline);
        }

        //run for scanlines of vblank
        for (int scanline = 241; scanline < 262; ++scanline) {
            runLine(scanline);
        }

        //do end of frame stuff
        dontSleep = apu.bufferHasLessThan(1000);
        //if the audio buffer is completely drained, don't sleep for this frame
        //this is to prevent the emulator from getting stuck sleeping too much
        //on a slow system or when the audio buffer runs dry.

        apu.finishframe();
        cpu.modcycles();

        //run cpu, ppu for active drawing time

        //render the frame
        ppu.renderFrame(gui);
        if ((framecount & 2047) == 0) {
            //save sram every 30 seconds or so
            saveSRAM(true);
        }
        ++framecount;
        //System.err.println(framecount);
    }

    private void runLine(int scanline) {
        //System.err.println(scanline);
        ppu.clockLine(scanline);
    }

    public void setControllers(ControllerInterface controller1, ControllerInterface controller2) {
        this.controller1 = controller1;
        this.controller2 = controller2;
    }

    public void toggleFrameLimiter() {
        if (frameLimiterOn) {
            frameLimiterOn = false;
        } else {
            frameLimiterOn = true;
        }
    }

    public synchronized void loadROM(final String filename) {
        runEmulation = false;
        if (FileUtils.exists(filename)
                && (FileUtils.getExtension(filename).equalsIgnoreCase(".nes")
                || FileUtils.getExtension(filename).equalsIgnoreCase(".nsf"))) {
            Mapper newmapper;
            try {
                final ROMLoader loader = new ROMLoader(filename);
                loader.parseHeader();
                newmapper = Mapper.getCorrectMapper(loader);
                newmapper.setLoader(loader);
                newmapper.loadrom();
            } catch (BadMapperException e) {
                gui.messageBox("Error Loading File: ROM is"
                        + " corrupted or uses an unsupported mapper.\n" + e.getMessage());
                return;
            } catch (Exception e) {
                gui.messageBox("Error Loading File: ROM is"
                        + " corrupted or uses an unsupported mapper.\n" + e.toString() + e.getMessage());
                e.printStackTrace();
                return;
            }
            if (apu != null) {
                //if rom already running save its sram before closing
                apu.destroy();
                saveSRAM(false);
                //also get rid of mapper etc.
                mapper.destroy();
                cpu = null;
                cpuram = null;
                ppu = null;
            }
            mapper = newmapper;
            //now some annoying getting of all the references where they belong
            cpuram = mapper.getCPURAM();
            actionReplay = new ActionReplay(cpuram);
            cpu = mapper.cpu;
            ppu = mapper.ppu;
            apu = new APU(this, cpu, cpuram);
            cpuram.setAPU(apu);
            cpuram.setPPU(ppu);
            curRomPath = filename;
            curRomName = FileUtils.getFilenamefromPath(filename);

            framecount = 0;
            //if savestate exists, load it
            if (mapper.hasSRAM()) {
                loadSRAM();
            }
            //and start emulation
            cpu.init();
            mapper.init();
            runEmulation = true;
        } else {
            gui.messageBox("Could not load file:\nFile " + filename + "\n"
                    + "does not exist or is not a valid NES game.");
        }
    }

    private void saveSRAM(final boolean async) {
        if (mapper != null && mapper.hasSRAM() && mapper.supportsSaves()) {
            if (async) {
                FileUtils.asyncwritetofile(mapper.getPRGRam(), FileUtils.stripExtension(curRomPath) + ".sav");
            } else {
                FileUtils.writetofile(mapper.getPRGRam(), FileUtils.stripExtension(curRomPath) + ".sav");
            }
        }
    }

    private void loadSRAM() {
        final String name = FileUtils.stripExtension(curRomPath) + ".sav";
        if (FileUtils.exists(name) && mapper.supportsSaves()) {
            mapper.setPRGRAM(FileUtils.readfromfile(name));
        }

    }

    public void quit() {
        //save SRAM and quit
        if (cpu != null && curRomPath != null) {
            runEmulation = false;
            saveSRAM(false);
        }
        System.exit(0);
    }

    public synchronized void reset() {
        if (cpu != null) {
            mapper.reset();
            cpu.reset();
            runEmulation = true;
            apu.pause();
            apu.resume();
        }
        //reset frame counter as well because PPU is reset
        //on Famicom, PPU is not reset when Reset is pressed
        //but some NES games expect it to be and you get garbage.
        framecount = 0;
    }

    public synchronized void reloadROM() {
        loadROM(curRomPath);
    }

    public synchronized void pause() {
        if (apu != null) {
            apu.pause();
        }
        runEmulation = false;
    }

    public long getFrameTime() {
        return frameDoneTime;
    }

    public String getrominfo() {
        if (mapper != null) {
            return mapper.getrominfo();
        }
        return null;
    }

    public synchronized void frameAdvance() {
        runEmulation = false;
        if (cpu != null) {
            runframe();
        }
    }

    public synchronized void resume() {
        if (apu != null) {
            apu.resume();
        }
        if (cpu != null) {
            runEmulation = true;
        }
    }

    public String getCurrentRomName() {
        return curRomName;
    }

    public boolean isFrameLimiterOn() {
        return frameLimiterOn;
    }

    public void messageBox(final String string) {
        gui.messageBox(string);
    }

    public ControllerInterface getcontroller1() {
        return controller1;
    }

    public ControllerInterface getcontroller2() {
        return controller2;
    }

    public void setApuVol() {
        if (apu != null) {
            apu.setParameters();
        }
    }

    /**
     * Access to the Pro Action Replay device.
     *
     * @return
     */
    public synchronized ActionReplay getActionReplay() {
        return actionReplay;
    }
}
