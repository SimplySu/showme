package com.suwonsmartapp.hello.showme.encoding.prober.distributionanalysis;

public abstract class CharDistributionAnalysis {
    public static final float SURE_NO = 0.01F;
    public static final float SURE_YES = 0.99F;
    public static final int ENOUGH_DATA_THRESHOLD = 1024;
    public static final int MINIMUM_DATA_THRESHOLD = 4;
    private int freqChars;
    private int totalChars;
    protected int[] charToFreqOrder;
    protected float typicalDistributionRatio;
    protected boolean done;

    public CharDistributionAnalysis() {
        this.reset();
    }

    public void handleData(byte[] var1, int var2, int var3) {
    }

    public void handleOneChar(byte[] var1, int var2, int var3) {
        int var4 = -1;
        if (var3 == 2) {
            var4 = this.getOrder(var1, var2);
        }

        if (var4 >= 0) {
            ++this.totalChars;
            if (var4 < this.charToFreqOrder.length && 512 > this.charToFreqOrder[var4]) {
                ++this.freqChars;
            }
        }

    }

    public float getConfidence() {
        if (this.totalChars > 0 && this.freqChars > 4) {
            if (this.totalChars != this.freqChars) {
                float var1 = (float) (this.freqChars / (this.totalChars - this.freqChars)) * this.typicalDistributionRatio;
                if (var1 < 0.99F) {
                    return var1;
                }
            }

            return 0.99F;
        } else {
            return 0.01F;
        }
    }

    public void reset() {
        this.done = false;
        this.totalChars = 0;
        this.freqChars = 0;
    }

    public void setOption() {
    }

    public boolean gotEnoughData() {
        return this.totalChars > 1024;
    }

    protected abstract int getOrder(byte[] var1, int var2);
}
