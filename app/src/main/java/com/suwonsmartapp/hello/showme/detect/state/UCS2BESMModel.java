package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class UCS2BESMModel extends StateMachine {

    public static final int UCS2BE_CLASS_FACTOR = 6;

    public UCS2BESMModel() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, ucs2beClassTable),
                UCS2BE_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, ucs2beStateTable),
                ucs2beCharLenTable,
                Constants.CHARSET_UTF_16BE
        );
    }

    private static int[] ucs2beClassTable = new int[]{
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 00 - 07
            Pakage.pack4bits(0, 0, 1, 0, 0, 2, 0, 0),  // 08 - 0f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 10 - 17
            Pakage.pack4bits(0, 0, 0, 3, 0, 0, 0, 0),  // 18 - 1f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 20 - 27
            Pakage.pack4bits(0, 3, 3, 3, 3, 3, 0, 0),  // 28 - 2f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 30 - 37
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 38 - 3f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 40 - 47
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 48 - 4f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 50 - 57
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 58 - 5f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 60 - 67
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 68 - 6f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 70 - 77
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 78 - 7f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 80 - 87
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 88 - 8f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 90 - 97
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 98 - 9f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // a0 - a7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // a8 - af
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // b0 - b7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // b8 - bf
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // c0 - c7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // c8 - cf
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // d0 - d7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // d8 - df
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // e0 - e7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // e8 - ef
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // f0 - f7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 4, 5)   // f8 - ff
    };

    private static int[] ucs2beStateTable = new int[]{
            Pakage.pack4bits(5, 7, 7, ERROR, 4, 3, ERROR, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ITSME, ITSME, ITSME, ITSME),//08-0f
            Pakage.pack4bits(ITSME, ITSME, 6, 6, 6, 6, ERROR, ERROR),//10-17
            Pakage.pack4bits(6, 6, 6, 6, 6, ITSME, 6, 6),//18-1f
            Pakage.pack4bits(6, 6, 6, 6, 5, 7, 7, ERROR),//20-27
            Pakage.pack4bits(5, 8, 6, 6, ERROR, 6, 6, 6),//28-2f
            Pakage.pack4bits(6, 6, 6, 6, ERROR, ERROR, START, START) //30-37
    };

    private static int[] ucs2beCharLenTable = new int[]{
            2, 2, 2, 0, 2, 2
    };
}