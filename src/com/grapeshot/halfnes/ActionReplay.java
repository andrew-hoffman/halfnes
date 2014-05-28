package com.grapeshot.halfnes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Emulation of the Pro Action Replay device. This device allows to apply "RAM
 * codes" to have extra lives, ammo, time, etc...
 *
 * @author Thomas Lorblanches
 */
public class ActionReplay {

    private static final int RAM_SIZE = 0x07FF;
    private final CPURAM cpuram;
    // Memory patches for Pro Action Replay codes
    private final HashMap<Integer, Patch> patches = new HashMap<Integer, Patch>();
    // List of addresses for the "find code" feature
    private final List<Integer> foundAddresses = new ArrayList<Integer>();

    /**
     * Creates a new Pro Action Replay device which will act on the given
     * memory.
     *
     * @param cpuram - memory
     */
    public ActionReplay(CPURAM cpuram) {
        this.cpuram = cpuram;
    }

    /**
     * Get the list of patches currently applied.
     *
     * @return
     */
    public HashMap<Integer, Patch> getPatches() {
        return patches;
    }

    /**
     * Add a memory patch. The patch is permanent (the value is constantly
     * written into memory until a new game is loaded).
     */
    public void addMemoryPatch(Patch patch) {
        if (!patches.containsKey(patch.getAddress())) {
            patches.put((Integer) patch.getAddress(), patch);
        }
    }

    /**
     * Patches the memory with Pro Action Replay codes.
     */
    public void applyPatches() {
        cpuram.setPatches(patches);
    }

    /**
     * Remove all the patches.
     */
    public void clear() {
        patches.clear();
    }

    /**
     * Find where in RAM can be found the given value. This method begins a new
     * search.
     *
     * @param value - value to be found.
     * @return the list of addresses where the value were found.
     */
    public List<Integer> newSearchInMemory(byte value) {
        foundAddresses.clear();
        for (int address = 0; address < RAM_SIZE; address++) {
            if ((cpuram.read(address) & 0xFF) == (value & 0xFF)) {
                foundAddresses.add(address);
            }
        }
        return foundAddresses;
    }

    /**
     * Gets the list memory addresses of the current search.
     *
     * @return
     */
    public List<Integer> getFoundAddresses() {
        return foundAddresses;
    }

    /**
     * Find where at the previously found addresses can be found the given
     * value. This method continue a previously started search.
     *
     * @param value - value to be found.
     * @return the list of addresses where the value were found.
     */
    public List<Integer> continueSearch(byte value) {
        List<Integer> addressesToRemove = new ArrayList<Integer>();
        for (int address : foundAddresses) {
            if ((cpuram.read(address) & 0xFF) != (value & 0xFF)) {
                addressesToRemove.add(address);
            }
        }
        foundAddresses.removeAll(addressesToRemove);
        return foundAddresses;
    }
}
