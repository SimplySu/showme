package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineEUCTW extends StateMachine {

    public static final int EUCTW_CLASS_FACTOR = 7;

    public StateMachineEUCTW() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, euctwClassTable),
                EUCTW_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, euctwStateTable),
                euctwCharLenTable,
                Constants.CHARSET_EUC_TW
        );
    }

    private static int[] euctwClassTable = new int[]{
//        Pakage.pack4bits(0,2,2,2,2,2,2,2),  // 00 - 07
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 00 - 07
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 0, 0),  // 08 - 0f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 10 - 17
            Pakage.pack4bits(2, 2, 2, 0, 2, 2, 2, 2),  // 18 - 1f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 20 - 27
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 28 - 2f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 30 - 37
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 38 - 3f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 40 - 47
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 48 - 4f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 50 - 57
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 58 - 5f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 60 - 67
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 68 - 6f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 70 - 77
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 78 - 7f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 80 - 87
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 6, 0),  // 88 - 8f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 90 - 97
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 98 - 9f
            Pakage.pack4bits(0, 3, 4, 4, 4, 4, 4, 4),  // a0 - a7
            Pakage.pack4bits(5, 5, 1, 1, 1, 1, 1, 1),  // a8 - af
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // b0 - b7
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // b8 - bf
            Pakage.pack4bits(1, 1, 3, 1, 3, 3, 3, 3),  // c0 - c7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // c8 - cf
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // d0 - d7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // d8 - df
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // e0 - e7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // e8 - ef
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // f0 - f7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 0)   // f8 - ff
    };

    private static int[] euctwStateTable = new int[]{
            Pakage.pack4bits(ERROR, ERROR, START, 3, 3, 3, 4, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ITSME, ITSME),//08-0f
            Pakage.pack4bits(ITSME, ITSME, ITSME, ITSME, ITSME, ERROR, START, ERROR),//10-17
            Pakage.pack4bits(START, START, START, ERROR, ERROR, ERROR, ERROR, ERROR),//18-1f
            Pakage.pack4bits(5, ERROR, ERROR, ERROR, START, ERROR, START, START),//20-27
            Pakage.pack4bits(START, ERROR, START, START, START, START, START, START) //28-2f
    };

    private static int[] euctwCharLenTable = new int[]{
            0, 0, 1, 2, 2, 2, 3
    };
}