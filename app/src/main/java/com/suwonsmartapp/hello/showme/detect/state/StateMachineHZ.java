package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineHZ extends StateMachine {

    public static final int HZS_CLASS_FACTOR = 6;

    public StateMachineHZ() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, hzsClassTable),
                HZS_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, hzsStateTable),
                hzsCharLenTable,
                Constants.CHARSET_HZ_GB_2312
        );
    }

    private static int[] hzsClassTable = new int[]{
            Pakage.pack4bits(1, 0, 0, 0, 0, 0, 0, 0),  // 00 - 07
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 08 - 0f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 10 - 17
            Pakage.pack4bits(0, 0, 0, 1, 0, 0, 0, 0),  // 18 - 1f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 20 - 27
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 28 - 2f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 30 - 37
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 38 - 3f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 40 - 47
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 48 - 4f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 50 - 57
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 58 - 5f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 60 - 67
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 68 - 6f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 70 - 77
            Pakage.pack4bits(0, 0, 0, 4, 0, 5, 2, 0),  // 78 - 7f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 80 - 87
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 88 - 8f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 90 - 97
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 98 - 9f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // a0 - a7
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // a8 - af
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // b0 - b7
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // b8 - bf
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // c0 - c7
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // c8 - cf
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // d0 - d7
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // d8 - df
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // e0 - e7
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // e8 - ef
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // f0 - f7
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1)   // f8 - ff
    };

    private static int[] hzsStateTable = new int[]{
            Pakage.pack4bits(START, ERROR, 3, START, START, START, ERROR, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ITSME, ITSME, ITSME, ITSME),//08-0f
            Pakage.pack4bits(ITSME, ITSME, ERROR, ERROR, START, START, 4, ERROR),//10-17
            Pakage.pack4bits(5, ERROR, 6, ERROR, 5, 5, 4, ERROR),//18-1f
            Pakage.pack4bits(4, ERROR, 4, 4, 4, ERROR, 4, ERROR),//20-27
            Pakage.pack4bits(4, ITSME, START, START, START, START, START, START) //28-2f
    };

    private static int[] hzsCharLenTable = new int[]{
            0, 0, 0, 0, 0, 0
    };
}