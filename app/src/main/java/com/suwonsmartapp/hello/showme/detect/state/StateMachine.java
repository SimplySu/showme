package com.suwonsmartapp.hello.showme.detect.state;

public abstract class StateMachine {

    public static final int START = 0;
    public static final int ERROR = 1;
    public static final int ITSME = 2;

    protected Pakage classTable;
    protected int classFactor;
    protected Pakage stateTable;
    protected int[] charLenTable;
    protected String name;

    public StateMachine(Pakage classTable, int classFactor, Pakage stateTable, int[] charLenTable, String name) {
        this.classTable = classTable;
        this.classFactor = classFactor;
        this.stateTable = stateTable;
        this.charLenTable = charLenTable;
        this.name = name;
    }

    public int getClass(byte b) {
        int c = b & 0xFF;
        return this.classTable.unpack(c);
    }

    public int getNextState(int cls, int currentState) {
        return this.stateTable.unpack(currentState * this.classFactor + cls);
    }

    public int getCharLen(int cls) {
        return this.charLenTable[cls];
    }

    public String getName() {
        return this.name;
    }
}