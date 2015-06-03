package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineSJIS extends StateMachine {

    public static final int SJIS_CLASS_FACTOR = 6;

    public StateMachineSJIS() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, sjisClassTable),
                SJIS_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, sjisStateTable),
                sjisCharLenTable,
                Constants.CHARSET_SHIFT_JIS
        );
    }

    private static int[] sjisClassTable = new int[]{
//        Pakage.pack4bits(0,1,1,1,1,1,1,1),  // 00 - 07
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 00 - 07
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 0, 0),  // 08 - 0f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 10 - 17
            Pakage.pack4bits(1, 1, 1, 0, 1, 1, 1, 1),  // 18 - 1f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 20 - 27
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 28 - 2f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 30 - 37
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 38 - 3f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 40 - 47
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 48 - 4f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 50 - 57
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 58 - 5f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 60 - 67
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 68 - 6f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 70 - 77
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 1),  // 78 - 7f
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // 80 - 87
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // 88 - 8f
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // 90 - 97
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // 98 - 9f
//        0xa0 is illegal in sjis encoding, but some pages does
//        contain such byte. We need to be more error forgiven.
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // a0 - a7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // a8 - af
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // b0 - b7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // b8 - bf
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // c0 - c7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // c8 - cf
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // d0 - d7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // d8 - df
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // e0 - e7
            Pakage.pack4bits(3, 3, 3, 3, 3, 4, 4, 4),  // e8 - ef
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // f0 - f7
            Pakage.pack4bits(4, 4, 4, 4, 4, 0, 0, 0)   // f8 - ff
    };

    private static int[] sjisStateTable = new int[]{
            Pakage.pack4bits(ERROR, START, START, 3, ERROR, ERROR, ERROR, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ITSME, ITSME, ITSME, ITSME),//08-0f
            Pakage.pack4bits(ITSME, ITSME, ERROR, ERROR, START, START, START, START) //10-17
    };

    private static int[] sjisCharLenTable = new int[]{
            0, 1, 1, 2, 0, 0
    };
}