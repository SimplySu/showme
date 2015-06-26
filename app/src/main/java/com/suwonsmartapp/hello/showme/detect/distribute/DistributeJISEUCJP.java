package com.suwonsmartapp.hello.showme.detect.distribute;

public class DistributeJISEUCJP extends DistributeJIS {

    public static final int HIGHBYTE_BEGIN = 0xA1;
    public static final int HIGHBYTE_END = 0xFE;
    public static final int LOWBYTE_BEGIN = 0xA1;
    public static final int LOWBYTE_END = 0xFE;

    public DistributeJISEUCJP() {
        super();
    }

    @Override
    protected int getOrder(final byte[] buf, int offset) {
        int highbyte = buf[offset] & 0xFF;
        if (highbyte >= HIGHBYTE_BEGIN) {
            int lowbyte = buf[offset + 1] & 0xFF;
            return (94 * (highbyte - HIGHBYTE_BEGIN) + lowbyte - LOWBYTE_BEGIN);
        } else {
            return -1;
        }
    }
}
