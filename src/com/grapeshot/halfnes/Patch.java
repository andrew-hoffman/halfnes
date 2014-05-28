package com.grapeshot.halfnes;

/**
 * A patch object includes an address to modify and the value to write at this
 * address. Patch objects are immutable and then thread-safe.
 *
 * @author Thomas Lorblanches
 */
public class Patch {

    private final int type; //0 = no compare read, 1 = do
    private final int address;
    private final int data;
    private final int cmpData;

    /**
     * Creates a patch.
     *
     * @param address - in-memory address to be patched.
     * @param data - data to write in the address.
     */
    public Patch(int address, int data) {
        this.address = address;
        this.data = data;
        this.type = 0;
        this.cmpData = 0;
    }

    public Patch(int address, int data, int check) {
        this.address = address;
        this.data = data;
        this.type = 1;
        this.cmpData = check;
    }

    /**
     * Returns the address to patch.
     *
     * @return
     */
    public int getAddress() {
        return address;
    }

    /**
     * Returns the value to write in the address.
     *
     * @return
     */
    public int getData() {
        return data;
    }

    /**
     * Returns true if the check matches the data or it's a code type that
     * doesn't check data bus
     *
     * @return
     */
    public boolean matchesData(int data) {
        return (type == 0) || (data == cmpData);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + this.address;
        hash = 71 * hash + this.data;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Patch other = (Patch) obj;
        if (this.address != other.address) {
            return false;
        }
        if (this.data != other.data) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String addStr = Integer.toHexString(address);
        String datStr = Integer.toHexString(data & 0xFF);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6 - addStr.length(); i++) {
            builder.append("0");
        }
        builder.append(addStr);
        for (int i = 0; i < 2 - datStr.length(); i++) {
            builder.append("0");
        }
        builder.append(datStr);
        return builder.toString().toUpperCase();
    }
}
