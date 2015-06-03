package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineEUCJP extends StateMachine {

    public static final int EUCJP_CLASS_FACTOR = 6;

    public StateMachineEUCJP() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, eucjpClassTable),
                EUCJP_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, eucjpStateTable),
                eucjpCharLenTable,
                Constants.CHARSET_EUC_JP
        );
    }

    private static int[] eucjpClassTable = new int[]{
//        Pakage.pack4bits(5,4,4,4,4,4,4,4),  // 00 - 07
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 00 - 07
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 5, 5),  // 08 - 0f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 10 - 17
            Pakage.pack4bits(4, 4, 4, 5, 4, 4, 4, 4),  // 18 - 1f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 20 - 27
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 28 - 2f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 30 - 37
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 38 - 3f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 40 - 47
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 48 - 4f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 50 - 57
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 58 - 5f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 60 - 67
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 68 - 6f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 70 - 77
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 78 - 7f
            Pakage.pack4bits(5, 5, 5, 5, 5, 5, 5, 5),  // 80 - 87
            Pakage.pack4bits(5, 5, 5, 5, 5, 5, 1, 3),  // 88 - 8f
            Pakage.pack4bits(5, 5, 5, 5, 5, 5, 5, 5),  // 90 - 97
            Pakage.pack4bits(5, 5, 5, 5, 5, 5, 5, 5),  // 98 - 9f
            Pakage.pack4bits(5, 2, 2, 2, 2, 2, 2, 2),  // a0 - a7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // a8 - af
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // b0 - b7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // b8 - bf
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // c0 - c7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // c8 - cf
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // d0 - d7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // d8 - df
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // e0 - e7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // e8 - ef
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // f0 - f7
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 5)   // f8 - ff
    };

    private static int[] eucjpStateTable = new int[]{
            Pakage.pack4bits(3, 4, 3, 5, START, ERROR, ERROR, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ITSME, ITSME, ITSME, ITSME),//08-0f
            Pakage.pack4bits(ITSME, ITSME, START, ERROR, START, ERROR, ERROR, ERROR),//10-17
            Pakage.pack4bits(ERROR, ERROR, START, ERROR, ERROR, ERROR, 3, ERROR),//18-1f
            Pakage.pack4bits(3, ERROR, ERROR, ERROR, START, START, START, START) //20-27
    };

    private static int[] eucjpCharLenTable = new int[]{
            2, 2, 2, 3, 1, 0
    };
}