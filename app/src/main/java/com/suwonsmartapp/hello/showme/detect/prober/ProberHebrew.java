package com.suwonsmartapp.hello.showme.detect.prober;

import com.suwonsmartapp.hello.showme.detect.character.Constants;

public class ProberHebrew extends ProberCharset {

    public static final int FINAL_KAF = 0xEA;
    public static final int NORMAL_KAF = 0xEB;
    public static final int FINAL_MEM = 0xED;
    public static final int NORMAL_MEM = 0xEE;
    public static final int FINAL_NUN = 0xEF;
    public static final int NORMAL_NUN = 0xF0;
    public static final int FINAL_PE = 0xF3;
    public static final int NORMAL_PE = 0xF4;
    public static final int FINAL_TSADI = 0xF5;
    public static final int NORMAL_TSADI = 0xF6;

    public static final byte SPACE = 0x20;

    public static final int MIN_FINAL_CHAR_DISTANCE = 5;
    public static final float MIN_MODEL_DISTANCE = 0.01f;

    private int finalCharLogicalScore;
    private int finalCharVisualScore;
    private byte prev;
    private byte beforePrev;

    private ProberCharset logicalProber;
    private ProberCharset visualProber;

    public ProberHebrew() {
        super();
        this.logicalProber = null;
        this.visualProber = null;
        reset();
    }

    public void setModalProbers(ProberCharset logicalProber, ProberCharset visualProber) {
        this.logicalProber = logicalProber;
        this.visualProber = visualProber;
    }

    @Override
    public String getCharSetName() {
        // If the final letter score distance is dominant enough, rely on it.
        int finalsub = this.finalCharLogicalScore - this.finalCharVisualScore;
        if (finalsub >= MIN_FINAL_CHAR_DISTANCE) {
            return Constants.CHARSET_WINDOWS_1255;
        }
        if (finalsub <= -MIN_FINAL_CHAR_DISTANCE) {
            return Constants.CHARSET_ISO_8859_8;
        }

        // It's not dominant enough, try to rely on the model scores instead.
        float modelsub = this.logicalProber.getConfidence() - this.visualProber.getConfidence();
        if (modelsub > MIN_MODEL_DISTANCE) {
            return Constants.CHARSET_WINDOWS_1255;
        }
        if (modelsub < -MIN_MODEL_DISTANCE) {
            return Constants.CHARSET_ISO_8859_8;
        }

        // Still no good, back to final letter distance, maybe it'll save the day.
        if (finalsub < 0) {
            return Constants.CHARSET_ISO_8859_8;
        }

        // (finalsub > 0 - Logical) or (don't know what to do) default to Logical.
        return Constants.CHARSET_WINDOWS_1255;
    }

    @Override
    public float getConfidence() {
        return 0.0f;
    }

    @Override
    public ProbingState getState() {
        // Remain active as long as any of the model probers are active.
        if ((this.logicalProber.getState() == ProbingState.NOT_ME) &&
                (this.visualProber.getState() == ProbingState.NOT_ME)) {
            return ProbingState.NOT_ME;
        }

        return ProbingState.DETECTING;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length) {
        if (getState() == ProbingState.NOT_ME) {
            return ProbingState.NOT_ME;
        }

        byte c;
        int maxPos = offset + length;
        for (int i = offset; i < maxPos; ++i) {
            c = buf[i];
            if (c == SPACE) {
                if (this.beforePrev != SPACE) {
                    if (isFinal(this.prev)) {
                        ++this.finalCharLogicalScore;
                    } else if (isNonFinal(this.prev)) {
                        ++this.finalCharVisualScore;
                    }
                }
            } else {
                if ((this.beforePrev == SPACE) &&
                        isFinal(this.prev) &&
                        c != SPACE) {
                    ++this.finalCharVisualScore;
                }
            }
            this.beforePrev = this.prev;
            this.prev = c;
        }

        return ProbingState.DETECTING;
    }

    @Override
    public void reset() {
        this.finalCharLogicalScore = 0;
        this.finalCharVisualScore = 0;

        // mPrev and mBeforePrev are initialized to space in order to simulate a word
        // delimiter at the beginning of the data
        this.prev = SPACE;
        this.beforePrev = SPACE;
    }

    @Override
    public void setOption() {
    }

    protected static boolean isFinal(byte b) {
        int c = b & 0xFF;
        return (
                c == FINAL_KAF ||
                        c == FINAL_MEM ||
                        c == FINAL_NUN ||
                        c == FINAL_PE ||
                        c == FINAL_TSADI
        );
    }

    protected static boolean isNonFinal(byte b) {
        int c = b & 0xFF;
        return (
                c == NORMAL_KAF ||
                        c == NORMAL_MEM ||
                        c == NORMAL_NUN ||
                        c == NORMAL_PE
        );
        // The normal Tsadi is not a good Non-Final letter due to words like
        // 'lechotet' (to chat) containing an apostrophe after the tsadi. This
        // apostrophe is converted to a space in FilterWithoutEnglishLetters causing
        // the Non-Final tsadi to appear at an end of a word even though this is not
        // the case in the original text.
        // The letters Pe and Kaf rarely display a related behavior of not being a
        // good Non-Final letter. Words like 'Pop', 'Winamp' and 'Mubarak' for
        // example legally end with a Non-Final Pe or Kaf. However, the benefit of
        // these letters as Non-Final letters outweighs the damage since these words
        // are quite rare.
    }
}