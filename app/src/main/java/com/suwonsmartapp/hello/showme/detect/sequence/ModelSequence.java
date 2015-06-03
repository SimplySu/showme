package com.suwonsmartapp.hello.showme.detect.sequence;

public abstract class ModelSequence {

    protected short[] charToOrderMap;
    protected byte[] precedenceMatrix;
    protected float typicalPositiveRatio;
    protected boolean keepEnglishLetter;
    protected String charsetName;

    public ModelSequence(
            short[] charToOrderMap,
            byte[] precedenceMatrix,
            float typicalPositiveRatio,
            boolean keepEnglishLetter,
            String charsetName) {
        this.charToOrderMap = charToOrderMap;
        this.precedenceMatrix = precedenceMatrix;
        this.typicalPositiveRatio = typicalPositiveRatio;
        this.keepEnglishLetter = keepEnglishLetter;
        this.charsetName = charsetName;
    }

    public short getOrder(byte b) {
        int c = b & 0xFF;
        return this.charToOrderMap[c];
    }

    public byte getPrecedence(int pos) {
        return this.precedenceMatrix[pos];
    }

    public float getTypicalPositiveRatio() {
        return this.typicalPositiveRatio;
    }

    public boolean getKeepEnglishLetter() {
        return this.keepEnglishLetter;
    }

    public String getCharsetName() {
        return this.charsetName;
    }
}