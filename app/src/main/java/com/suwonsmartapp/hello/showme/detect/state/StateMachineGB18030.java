package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineGB18030 extends StateMachine {

    public static final int GB18030_CLASS_FACTOR = 7;

    public StateMachineGB18030() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, gb18030ClassTable),
                GB18030_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, gb18030StateTable),
                gb18030CharLenTable,
                Constants.CHARSET_GB18030
        );
    }

    private static int[] gb18030ClassTable = new int[]{
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 00 - 07
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 0, 0),  // 08 - 0f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 10 - 17
            Pakage.pack4bits(1, 1, 1, 0, 1, 1, 1, 1),  // 18 - 1f
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 20 - 27
            Pakage.pack4bits(1, 1, 1, 1, 1, 1, 1, 1),  // 28 - 2f
            Pakage.pack4bits(3, 3, 3, 3, 3, 3, 3, 3),  // 30 - 37
            Pakage.pack4bits(3, 3, 1, 1, 1, 1, 1, 1),  // 38 - 3f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 40 - 47
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 48 - 4f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 50 - 57
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 58 - 5f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 60 - 67
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 68 - 6f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 70 - 77
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 4),  // 78 - 7f
            Pakage.pack4bits(5, 6, 6, 6, 6, 6, 6, 6),  // 80 - 87
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // 88 - 8f
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // 90 - 97
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // 98 - 9f
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // a0 - a7
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // a8 - af
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // b0 - b7
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // b8 - bf
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // c0 - c7
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // c8 - cf
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // d0 - d7
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // d8 - df
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // e0 - e7
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // e8 - ef
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 6),  // f0 - f7
            Pakage.pack4bits(6, 6, 6, 6, 6, 6, 6, 0)   // f8 - ff
    };

    private static int[] gb18030StateTable = new int[]{
            Pakage.pack4bits(ERROR, START, START, START, START, START, 3, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ITSME, ITSME),//08-0f
            Pakage.pack4bits(ITSME, ITSME, ITSME, ITSME, ITSME, ERROR, ERROR, START),//10-17
            Pakage.pack4bits(4, ERROR, START, START, ERROR, ERROR, ERROR, ERROR),//18-1f
            Pakage.pack4bits(ERROR, ERROR, 5, ERROR, ERROR, ERROR, ITSME, ERROR),//20-27
            Pakage.pack4bits(ERROR, ERROR, START, START, START, START, START, START) //28-2f
    };

    private static int[] gb18030CharLenTable = new int[]{
            0, 1, 1, 1, 1, 1, 2
    };
}