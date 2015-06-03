package com.suwonsmartapp.hello.showme.detect.prober;

import com.suwonsmartapp.hello.showme.detect.character.Constants;
import com.suwonsmartapp.hello.showme.detect.distribute.DistributeEUCKR;
import com.suwonsmartapp.hello.showme.detect.state.StateMachineCoding;
import com.suwonsmartapp.hello.showme.detect.state.StateMachineEUCKR;
import com.suwonsmartapp.hello.showme.detect.state.StateMachine;

public class ProberEUCKR extends ProberCharset {

    private StateMachineCoding codingSM;
    private ProbingState state;

    private DistributeEUCKR distributionAnalyzer;

    private byte[] lastChar;

    private static final StateMachine STATE_MACHINE = new StateMachineEUCKR();

    public ProberEUCKR() {
        super();
        this.codingSM = new StateMachineCoding(STATE_MACHINE);
        this.distributionAnalyzer = new DistributeEUCKR();
        this.lastChar = new byte[2];
        reset();
    }

    @Override
    public String getCharSetName() {
        return Constants.CHARSET_EUC_KR;
    }

    @Override
    public float getConfidence() {
        float distribCf = this.distributionAnalyzer.getConfidence();

        return distribCf;
    }

    @Override
    public ProbingState getState() {
        return this.state;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length) {
        int codingState;

        int maxPos = offset + length;
        for (int i = offset; i < maxPos; ++i) {
            codingState = codingSM.nextState(buf[i]);
            if (codingState == StateMachine.ERROR) {
                this.state = ProbingState.NOT_ME;
                break;
            }
            if (codingState == StateMachine.ITSME) {
                this.state = ProbingState.FOUND_IT;
                break;
            }
            if (codingState == StateMachine.START) {
                int charLen = this.codingSM.getCurrentCharLen();
                if (i == offset) {
                    this.lastChar[1] = buf[offset];
                    this.distributionAnalyzer.handleOneChar(this.lastChar, 0, charLen);
                } else {
                    this.distributionAnalyzer.handleOneChar(buf, i - 1, charLen);
                }
            }
        }

        this.lastChar[0] = buf[maxPos - 1];

        if (this.state == ProbingState.DETECTING) {
            if (this.distributionAnalyzer.gotEnoughData() && getConfidence() > SHORTCUT_THRESHOLD) {
                this.state = ProbingState.FOUND_IT;
            }
        }

        return this.state;
    }

    @Override
    public void reset() {
        this.codingSM.reset();
        this.state = ProbingState.DETECTING;
        this.distributionAnalyzer.reset();
        java.util.Arrays.fill(this.lastChar, (byte) 0);
    }

    @Override
    public void setOption() {
    }
}