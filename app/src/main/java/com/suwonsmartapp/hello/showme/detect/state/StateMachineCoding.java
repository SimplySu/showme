package com.suwonsmartapp.hello.showme.detect.state;

public class StateMachineCoding {

    protected StateMachine model;
    protected int currentState;
    protected int currentCharLen;
    protected int currentBytePos;

    public StateMachineCoding(StateMachine model) {
        this.model = model;
        this.currentState = StateMachine.START;
    }

    public int nextState(byte c) {
        int byteCls = this.model.getClass(c);
        if (this.currentState == StateMachine.START) {
            this.currentBytePos = 0;
            this.currentCharLen = this.model.getCharLen(byteCls);
        }

        this.currentState = this.model.getNextState(byteCls, this.currentState);
        ++this.currentBytePos;

        return this.currentState;
    }

    public int getCurrentCharLen() {
        return this.currentCharLen;
    }

    public void reset() {
        this.currentState = StateMachine.START;
    }

    public String getCodingStateMachine() {
        return this.model.getName();
    }
}