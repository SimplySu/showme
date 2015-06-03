package com.suwonsmartapp.hello.showme.detect.prober;

import com.suwonsmartapp.hello.showme.detect.state.StateMachineCoding;
import com.suwonsmartapp.hello.showme.detect.state.StateMachineHZ;
import com.suwonsmartapp.hello.showme.detect.state.StateMachineISO2022CN;
import com.suwonsmartapp.hello.showme.detect.state.StateMachineISO2022JP;
import com.suwonsmartapp.hello.showme.detect.state.StateMachineISO2022KR;
import com.suwonsmartapp.hello.showme.detect.state.StateMachine;

public class ProberEsc extends ProberCharset {

    private StateMachineCoding[] codingSM;
    private int activeSM;
    private ProbingState state;
    private String detectedCharset;

    private static final StateMachineHZ hzsModel = new StateMachineHZ();
    private static final StateMachineISO2022CN iso2022cnModel = new StateMachineISO2022CN();
    private static final StateMachineISO2022JP iso2022jpModel = new StateMachineISO2022JP();
    private static final StateMachineISO2022KR iso2022krModel = new StateMachineISO2022KR();

    public ProberEsc() {
        super();

        this.codingSM = new StateMachineCoding[4];
        this.codingSM[0] = new StateMachineCoding(hzsModel);
        this.codingSM[1] = new StateMachineCoding(iso2022cnModel);
        this.codingSM[2] = new StateMachineCoding(iso2022jpModel);
        this.codingSM[3] = new StateMachineCoding(iso2022krModel);

        reset();
    }

    @Override
    public String getCharSetName() {
        return this.detectedCharset;
    }

    @Override
    public float getConfidence() {
        return 0.99f;
    }

    @Override
    public ProbingState getState() {
        return this.state;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length) {
        int codingState;

        int maxPos = offset + length;
        for (int i = offset; i < maxPos && this.state == ProbingState.DETECTING; ++i) {
            for (int j = this.activeSM - 1; j >= 0; --j) {
                codingState = this.codingSM[j].nextState(buf[i]);
                if (codingState == StateMachine.ERROR) {
                    --this.activeSM;
                    if (this.activeSM <= 0) {
                        this.state = ProbingState.NOT_ME;
                        return this.state;
                    } else if (j != this.activeSM) {
                        StateMachineCoding t;
                        t = this.codingSM[this.activeSM];
                        this.codingSM[this.activeSM] = this.codingSM[j];
                        this.codingSM[j] = t;
                    }
                } else if (codingState == StateMachine.ITSME) {
                    this.state = ProbingState.FOUND_IT;
                    this.detectedCharset = this.codingSM[j].getCodingStateMachine();
                    return this.state;
                }
            }
        }

        return this.state;
    }

    @Override
    public void reset() {
        this.state = ProbingState.DETECTING;
        for (int i = 0; i < this.codingSM.length; ++i) {
            this.codingSM[i].reset();
        }
        this.activeSM = this.codingSM.length;
        this.detectedCharset = null;
    }

    @Override
    public void setOption() {
    }
}