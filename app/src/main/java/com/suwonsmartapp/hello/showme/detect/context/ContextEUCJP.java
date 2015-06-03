package com.suwonsmartapp.hello.showme.detect.context;

public class ContextEUCJP extends ContextJIS {

    public static final int HIRAGANA_HIGHBYTE = 0xA4;
    public static final int HIRAGANA_LOWBYTE_BEGIN = 0xA1;
    public static final int HIRAGANA_LOWBYTE_END = 0xF3;
    public static final int SINGLE_SHIFT_2 = 0x8E;
    public static final int SINGLE_SHIFT_3 = 0x8F;
    public static final int FIRSTPLANE_HIGHBYTE_BEGIN = 0xA1;
    public static final int FIRSTPLANE_HIGHBYTE_END = 0xFE;

    public ContextEUCJP() {
        super();
    }

    @Override
    protected void getOrder(Order order, final byte[] buf, int offset) {
        order.order = -1;
        order.charLength = 1;

        int firstByte = buf[offset] & 0xFF;
        if (firstByte == SINGLE_SHIFT_2 ||
                (firstByte >= FIRSTPLANE_HIGHBYTE_BEGIN &&
                        firstByte <= FIRSTPLANE_HIGHBYTE_END)) {
            order.charLength = 2;
        } else if (firstByte == SINGLE_SHIFT_3) {
            order.charLength = 3;
        }

        if (firstByte == HIRAGANA_HIGHBYTE) {
            int secondByte = buf[offset + 1] & 0xFF;
            if (secondByte >= HIRAGANA_LOWBYTE_BEGIN &&
                    secondByte <= HIRAGANA_LOWBYTE_END) {
                order.order = (secondByte - HIRAGANA_LOWBYTE_BEGIN);
            }
        }
    }

    @Override
    protected int getOrder(final byte[] buf, int offset) {
        int highbyte = buf[offset] & 0xFF;
        if (highbyte == HIRAGANA_HIGHBYTE) {
            int lowbyte = buf[offset + 1] & 0xFF;
            if (lowbyte >= HIRAGANA_LOWBYTE_BEGIN &&
                    lowbyte <= HIRAGANA_LOWBYTE_END) {
                return (lowbyte - HIRAGANA_LOWBYTE_BEGIN);
            }
        }

        return -1;
    }
}