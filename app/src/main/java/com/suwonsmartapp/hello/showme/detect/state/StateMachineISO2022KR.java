package com.suwonsmartapp.hello.showme.detect.state;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

import static com.suwonsmartapp.hello.showme.detect.state.Pakage.INDEX_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.SHIFT_MASK_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.BIT_SHIFT_4BITS;
import static com.suwonsmartapp.hello.showme.detect.state.Pakage.UNIT_MASK_4BITS;

public class StateMachineISO2022KR extends StateMachine {

    public static final int ISO2022KR_CLASS_FACTOR = 6;

    public StateMachineISO2022KR() {
        super(
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, iso2022krClassTable),
                ISO2022KR_CLASS_FACTOR,
                new Pakage(INDEX_SHIFT_4BITS, SHIFT_MASK_4BITS, BIT_SHIFT_4BITS, UNIT_MASK_4BITS, iso2022krStateTable),
                iso2022krCharLenTable,
                Constants.CHARSET_ISO_2022_KR
        );
    }

    private static int[] iso2022krClassTable = new int[]{
            Pakage.pack4bits(2, 0, 0, 0, 0, 0, 0, 0),  // 00 - 07
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 08 - 0f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 10 - 17
            Pakage.pack4bits(0, 0, 0, 1, 0, 0, 0, 0),  // 18 - 1f
            Pakage.pack4bits(0, 0, 0, 0, 3, 0, 0, 0),  // 20 - 27
            Pakage.pack4bits(0, 4, 0, 0, 0, 0, 0, 0),  // 28 - 2f
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 30 - 37
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 38 - 3f
            Pakage.pack4bits(0, 0, 0, 5, 0, 0, 0, 0),  // 40 - 47
            Pakage.pack4bits(0, 0, 0, 0, 0, 0, 0, 0),  // 48 - 4f
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

    private static int[] iso2022krStateTable = new int[]{
            Pakage.pack4bits(START, 3, ERROR, START, START, START, ERROR, ERROR),//00-07
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, ITSME, ITSME, ITSME, ITSME),//08-0f
            Pakage.pack4bits(ITSME, ITSME, ERROR, ERROR, ERROR, 4, ERROR, ERROR),//10-17
            Pakage.pack4bits(ERROR, ERROR, ERROR, ERROR, 5, ERROR, ERROR, ERROR),//18-1f
            Pakage.pack4bits(ERROR, ERROR, ERROR, ITSME, START, START, START, START) //20-27
    };

    private static int[] iso2022krCharLenTable = new int[]{
            0, 0, 0, 0, 0, 0
    };
}