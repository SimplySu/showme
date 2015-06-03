package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineBig5 extends StateMachine {

    public static final int BIG5_CLASS_FACTOR = 5;

    public StateMachineBig5() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, big5ClassTable),
                BIG5_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, big5StateTable),
                big5CharLenTable,
                Constants.CHARSET_BIG5
        );
    }

    private static int[] big5ClassTable = new int[]{
//        Pakage.pack4bits(0,1,1,1,1,1,1,1),  // 00 - 07
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 00 - 07    //allow 0x00 as legal value
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
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 80 - 87
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 88 - 8f
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 90 - 97
            Pakage.pack4bits(4, 4, 4, 4, 4, 4, 4, 4),  // 98 - 9f
            Pakage.pack4bits(4, 3, 3, 3, 3, 3, 3, 3),  // a0 - a7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // a8 - af
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // b0 - b7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // b8 - bf
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // c0 - c7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // c8 - cf
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // d0 - d7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // d8 - df
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // e0 - e7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // e8 - ef
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // f0 - f7
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 0)   // f8 - ff
    };

    private static int[] big5StateTable = new int[]{
            Pakage.pack4bits(ERROR, START, START, 3, ERROR, ERROR, ERROR, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ITSME, ITSME, ITSME, ITSME, ITSME, ERROR),//08-0f
            Pakage.pack4bits(ERROR, START, START, START, START, START, START, START) //10-17
    };

    private static int[] big5CharLenTable = new int[]{
            0, 1, 1, 2, 0
    };
}