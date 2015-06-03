package com.suwonsmartapp.hello.showme.detect.prober;

import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceHebrew;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceCyrillicIBM855;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceCyrillicIBM866;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceCyrillicKoi8R;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceBulgarianLatin5;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceCyrillicLatin5;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceGreekLatin7;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceCyrillicMac;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequence;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceBulgarianWin1251;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceCyrillicWin1251;
import com.suwonsmartapp.hello.showme.detect.sequence.ModelSequenceGreekWin1253;

import java.nio.ByteBuffer;

public class ProberSBCSgroup extends ProberCharset {

    private ProbingState state;
    private ProberCharset[] probers;
    private boolean[] isActive;
    private int bestGuess;
    private int activeNum;

    // models
    private static final ModelSequence win1251Model = new ModelSequenceCyrillicWin1251();
    private static final ModelSequence koi8rModel = new ModelSequenceCyrillicKoi8R();
    private static final ModelSequence latin5Model = new ModelSequenceCyrillicLatin5();
    private static final ModelSequence macCyrillicModel = new ModelSequenceCyrillicMac();
    private static final ModelSequence ibm866Model = new ModelSequenceCyrillicIBM866();
    private static final ModelSequence ibm855Model = new ModelSequenceCyrillicIBM855();
    private static final ModelSequence latin7Model = new ModelSequenceGreekLatin7();
    private static final ModelSequence win1253Model = new ModelSequenceGreekWin1253();
    private static final ModelSequence latin5BulgarianModel = new ModelSequenceBulgarianLatin5();
    private static final ModelSequence win1251BulgarianModel = new ModelSequenceBulgarianWin1251();
    private static final ModelSequence hebrewModel = new ModelSequenceHebrew();

    public ProberSBCSgroup() {
        super();

        this.probers = new ProberCharset[13];
        this.isActive = new boolean[13];

        this.probers[0] = new ProberSingleByte(win1251Model);
        this.probers[1] = new ProberSingleByte(koi8rModel);
        this.probers[2] = new ProberSingleByte(latin5Model);
        this.probers[3] = new ProberSingleByte(macCyrillicModel);
        this.probers[4] = new ProberSingleByte(ibm866Model);
        this.probers[5] = new ProberSingleByte(ibm855Model);
        this.probers[6] = new ProberSingleByte(latin7Model);
        this.probers[7] = new ProberSingleByte(win1253Model);
        this.probers[8] = new ProberSingleByte(latin5BulgarianModel);
        this.probers[9] = new ProberSingleByte(win1251BulgarianModel);

        ProberHebrew hebprober = new ProberHebrew();
        this.probers[10] = hebprober;
        this.probers[11] = new ProberSingleByte(hebrewModel, false, hebprober);
        this.probers[12] = new ProberSingleByte(hebrewModel, true, hebprober);
        hebprober.setModalProbers(this.probers[11], this.probers[12]);

        reset();
    }

    @Override
    public String getCharSetName() {
        if (this.bestGuess == -1) {
            getConfidence();
            if (this.bestGuess == -1) {
                this.bestGuess = 0;
            }
        }

        return this.probers[this.bestGuess].getCharSetName();
    }

    @Override
    public float getConfidence() {
        float bestConf = 0.0f;
        float cf;

        if (this.state == ProbingState.FOUND_IT) {
            return 0.99f;
        } else if (this.state == ProbingState.NOT_ME) {
            return 0.01f;
        } else {
            for (int i = 0; i < probers.length; ++i) {
                if (!this.isActive[i]) {
                    continue;
                }

                cf = this.probers[i].getConfidence();
                if (bestConf < cf) {
                    bestConf = cf;
                    this.bestGuess = i;
                }
            }
        }

        return bestConf;
    }

    @Override
    public ProbingState getState() {
        return this.state;
    }

    @Override
    public ProbingState handleData(byte[] buf, int offset, int length) {
        ProbingState st;

        do {
            ByteBuffer newbuf = filterWithoutEnglishLetters(buf, offset, length);
            if (newbuf.position() == 0) {
                break;
            }

            for (int i = 0; i < this.probers.length; ++i) {
                if (!this.isActive[i]) {
                    continue;
                }
                st = this.probers[i].handleData(newbuf.array(), 0, newbuf.position());
                if (st == ProbingState.FOUND_IT) {
                    this.bestGuess = i;
                    this.state = ProbingState.FOUND_IT;
                    break;
                } else if (st == ProbingState.NOT_ME) {
                    this.isActive[i] = false;
                    --this.activeNum;
                    if (this.activeNum <= 0) {
                        this.state = ProbingState.NOT_ME;
                        break;
                    }
                }
            }
        } while (false);

        return this.state;
    }

    @Override
    public void reset() {
        this.activeNum = 0;
        for (int i = 0; i < this.probers.length; ++i) {
            this.probers[i].reset();
            this.isActive[i] = true;
            ++this.activeNum;
        }

        this.bestGuess = -1;
        this.state = ProbingState.DETECTING;
    }

    @Override
    public void setOption() {
    }
}