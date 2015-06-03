package com.suwonsmartapp.hello.showme.detect.context;

public class ContextSJIS extends ContextJIS {

    public static final int HIRAGANA_HIGHBYTE = 0x82;
    public static final int HIRAGANA_LOWBYTE_BEGIN = 0x9F;
    public static final int HIRAGANA_LOWBYTE_END = 0xF1;
    public static final int HIGHBYTE_BEGIN_1 = 0x81;
    public static final int HIGHBYTE_END_1 = 0x9F;
    public static final int HIGHBYTE_BEGIN_2 = 0xE0;
    public static final int HIGHBYTE_END_2 = 0xEF;

    public ContextSJIS() {
        super();
    }

    @Override
    protected void getOrder(Order order, final byte[] buf, int offset) {
        order.order = -1;
        order.charLength = 1;

        int highbyte = buf[offset] & 0xFF;
        if ((highbyte >= HIGHBYTE_BEGIN_1 && highbyte <= HIGHBYTE_END_1) ||
                (highbyte >= HIGHBYTE_BEGIN_2 && highbyte <= HIGHBYTE_END_2)) {
            order.charLength = 2;
        }

        if (highbyte == HIRAGANA_HIGHBYTE) {
            int lowbyte = buf[offset + 1] & 0xFF;
            if (lowbyte >= HIRAGANA_LOWBYTE_BEGIN &&
                    lowbyte <= HIRAGANA_LOWBYTE_END) {
                order.order = (lowbyte - HIRAGANA_LOWBYTE_BEGIN);
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