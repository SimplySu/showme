package com.suwonsmartapp.hello.showme.detect.state;

import android.util.Log;

public class TestPakage {

    private static final String TAG = TestPakage.class.getSimpleName();

    private void showLog(String msg) {
        Log.d(TAG, msg);
    }

    public void testUnpack() {
        int[] data = new int[]{
                Pakage.pack4bits(0, 1, 2, 3, 4, 5, 6, 7),
                Pakage.pack4bits(8, 9, 10, 11, 12, 13, 14, 15)
        };

        Pakage pkg = new Pakage(
                Pakage.INDEX_SHIFT_4BITS,
                Pakage.SHIFT_MASK_4BITS,
                Pakage.BIT_SHIFT_4BITS,
                Pakage.UNIT_MASK_4BITS,
                data);

        for (int i = 0; i < 16; ++i) {
            int n = pkg.unpack(i);
            if (n != i) {
                showLog("Invalid packed value.");
            }
        }
    }
}