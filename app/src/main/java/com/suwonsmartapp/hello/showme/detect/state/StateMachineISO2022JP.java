package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineISO2022JP extends StateMachine {

    public static final int ISO2022JP_CLASS_FACTOR = 10;

    public StateMachineISO2022JP() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, iso2022jpClassTable),
                ISO2022JP_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, iso2022jpStateTable),
                iso2022jpCharLenTable,
                Constants.CHARSET_ISO_2022_JP
        );
    }

    private static int[] iso2022jpClassTable = new int[]{
            Pakage.pack4bits(2, 0, 0, 0, 0, 0, 0, 0),  // 00 - 07
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 2, 2),  // 08 - 0f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 10 - 17
            Pakage.pack4bits(0, 0, 0, 1, 0, 0, 0, 0),  // 18 - 1f
            Pakage.pack4bits(0, 0, 0, 0, 7, 0, 0, 0),  // 20 - 27
            Pakage.pack4bits(3, 0, 0, 0, 0, 0, 0, 0),  // 28 - 2f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 30 - 37
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 38 - 3f
            Pakage.pack4bits(6, 0, 4, 0, 8, 0, 0, 0),  // 40 - 47
            Pakage.pack4bits(0, 9, 5, 0, 0, 0, 0, 0),  // 48 - 4f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 50 - 57
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 58 - 5f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 60 - 67
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 68 - 6f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 70 - 77
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 78 - 7f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 80 - 87
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 88 - 8f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 90 - 97
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // 98 - 9f
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // a0 - a7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // a8 - af
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // b0 - b7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // b8 - bf
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // c0 - c7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // c8 - cf
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // d0 - d7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // d8 - df
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // e0 - e7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // e8 - ef
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2),  // f0 - f7
            Pakage.pack4bits(2, 2, 2, 2, 2, 2, 2, 2)   // f8 - ff
    };

    private static int[] iso2022jpStateTable = new int[]{
            Pakage.pack4bits(START, 3, ERROR, START, START, START, START, START),//00-07
            Pakage.pack4bits(START, START, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR),//08-0f
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ITSME, ITSME, ITSME, ITSME),//10-17
            Pakage.pack4bits(ITSME, ITSME, ITSME, ITSME, ITSME, ITSME, ERROR, ERROR),//18-1f
            Pakage.pack4bits(ERROR, 5, ERROR, ERROR, ERROR, 4, ERROR, ERROR),//20-27
            Pakage.pack4bits(ERROR, ERROR, ERROR, 6, ITSME, ERROR, ITSME, ERROR),//28-2f
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ITSME, ITSME),//30-37
            Pakage.pack4bits(ERROR, ERROR, ERROR, ITSME, ERROR, ERROR, ERROR, ERROR),//38-3f
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ITSME, ERROR, START, START) //40-47
    };

    private static int[] iso2022jpCharLenTable = new int[]{
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };
}