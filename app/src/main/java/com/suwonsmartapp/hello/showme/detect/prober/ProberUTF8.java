package com.suwonsmartapp.hello.showme.detect.prober;

import com.suwonsmartapp.hello.showme.detect.character.Constants;
import com.suwonsmartapp.hello.showme.detect.state.StateMachineCoding;
import com.suwonsmartapp.hello.showme.detect.state.StateMachine;
import com.suwonsmartapp.hello.showme.detect.state.UTF8StateMachine;

public class ProberUTF8 extends ProberCharset {

    public static final float ONE_CHAR_PROB = 0.50f;

    private StateMachineCoding codingSM;
    private ProbingState state;
    private int numOfMBChar;

    private static final StateMachine STATE_MACHINE = new UTF8StateMachine();

    public ProberUTF8() {
        super();
        this.numOfMBChar = 0;
        this.codingSM = new StateMachineCoding(STATE_MACHINE);

        reset();
    }

    public String getCharSetName() {
        return Constants.CHARSET_UTF_8;
    }

    public ProbingState handleData(final byte[] buf, int offset, int length) {
        int codingState;

        int maxPos = offset + length;
        for (int i = offset; i < maxPos; ++i) {
            codingState = this.codingSM.nextState(buf[i]);
            if (codingState == StateMachine.ERROR) {
                this.state = ProbingState.NOT_ME;
                break;
            }
            if (codingState == StateMachine.ITSME) {
                this.state = ProbingState.FOUND_IT;
                break;
            }
            if (codingState == StateMachine.START) {
                if (this.codingSM.getCurrentCharLen() >= 2) {
                    ++this.numOfMBChar;
                }
            }
        }

        if (this.state == ProbingState.DETECTING) {
            if (getConfidence() > SHORTCUT_THRESHOLD) {
                this.state = ProbingState.FOUND_IT;
            }
        }

        return this.state;
    }

    public ProbingState getState() {
        return this.state;
    }

    public void reset() {
        this.codingSM.reset();
        this.numOfMBChar = 0;
        this.state = ProbingState.DETECTING;
    }

    public float getConfidence() {
        float unlike = 0.99f;

        if (this.numOfMBChar < 6) {
            for (int i = 0; i < this.numOfMBChar; ++i) {
                unlike *= ONE_CHAR_PROB;
            }
            return (1.0f - unlike);
        } else {
            return 0.99f;
        }
    }

    public void setOption() {
    }
}