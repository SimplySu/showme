package com.suwonsmartapp.hello.showme.detect.character;

import android.util.Log;

import com.suwonsmartapp.hello.showme.detect.prober.ProberCharset;
import com.suwonsmartapp.hello.showme.detect.prober.ProberEsc;
import com.suwonsmartapp.hello.showme.detect.prober.ProberLatin1;
import com.suwonsmartapp.hello.showme.detect.prober.ProberMBCSgroup;
import com.suwonsmartapp.hello.showme.detect.prober.ProberSBCSgroup;

public class CodeDetector {

    private static final String TAG = CodeDetector.class.getSimpleName();

    private void showLog(String msg) {
        Log.d(TAG, msg);
    }

    public static final float SHORTCUT_THRESHOLD = 0.95f;
    public static final float MINIMUM_THRESHOLD = 0.20f;

    public enum InputState {
        PURE_ASCII,
        ESC_ASCII,
        HIGHBYTE
    }

    private InputState inputState;
    private boolean done;
    private boolean start;
    private boolean gotData;
    private byte lastChar;
    private String detectedCharset;

    private ProberCharset[] probers;
    private ProberCharset escProberCharset;

    private CharsetListener listener;

    /**
     * @param listener a listener object that is notified of
     *                 the detected encocoding. Can be null.
     */
    public CodeDetector(CharsetListener listener) {
        this.listener = listener;
        this.escProberCharset = null;
        this.probers = new ProberCharset[3];
        for (int i = 0; i < this.probers.length; ++i) {
            this.probers[i] = null;
        }

        reset();
    }

    public boolean isDone() {
        return this.done;
    }

    /**
     * @return The detected encoding is returned. If the detector couldn't
     * determine what encoding was used, null is returned.
     */
    public String getDetectedCharset() {
        return this.detectedCharset;
    }

    public void setListener(CharsetListener listener) {
        this.listener = listener;
    }

    public CharsetListener getListener() {
        return this.listener;
    }

    public void handleData(final byte[] buf, int offset, int length) {
        if (this.done) {
            return;
        }

        if (length > 0) {
            this.gotData = true;
        }

        if (this.start) {
            this.start = false;
            if (length > 3) {
                int b1 = buf[offset] & 0xFF;
                int b2 = buf[offset + 1] & 0xFF;
                int b3 = buf[offset + 2] & 0xFF;
                int b4 = buf[offset + 3] & 0xFF;

                switch (b1) {
                    case 0xEF:
                        if (b2 == 0xBB && b3 == 0xBF) {
                            this.detectedCharset = Constants.CHARSET_UTF_8;
                        }
                        break;
                    case 0xFE:
                        if (b2 == 0xFF && b3 == 0x00 && b4 == 0x00) {
                            this.detectedCharset = Constants.CHARSET_X_ISO_10646_UCS_4_3412;
                        } else if (b2 == 0xFF) {
                            this.detectedCharset = Constants.CHARSET_UTF_16BE;
                        }
                        break;
                    case 0x00:
                        if (b2 == 0x00 && b3 == 0xFE && b4 == 0xFF) {
                            this.detectedCharset = Constants.CHARSET_UTF_32BE;
                        } else if (b2 == 0x00 && b3 == 0xFF && b4 == 0xFE) {
                            this.detectedCharset = Constants.CHARSET_X_ISO_10646_UCS_4_2143;
                        }
                        break;
                    case 0xFF:
                        if (b2 == 0xFE && b3 == 0x00 && b4 == 0x00) {
                            this.detectedCharset = Constants.CHARSET_UTF_32LE;
                        } else if (b2 == 0xFE) {
                            this.detectedCharset = Constants.CHARSET_UTF_16LE;
                        }
                        break;
                } // swich end

                if (this.detectedCharset != null) {
                    this.done = true;
                    return;
                }
            }
        } // if (start) end

        int maxPos = offset + length;
        for (int i = offset; i < maxPos; ++i) {
            int c = buf[i] & 0xFF;
            if ((c & 0x80) != 0 && c != 0xA0) {
                if (this.inputState != InputState.HIGHBYTE) {
                    this.inputState = InputState.HIGHBYTE;

                    if (this.escProberCharset != null) {
                        this.escProberCharset = null;
                    }

                    if (this.probers[0] == null) {
                        this.probers[0] = new ProberMBCSgroup();
                    }
                    if (this.probers[1] == null) {
                        this.probers[1] = new ProberSBCSgroup();
                    }
                    if (this.probers[2] == null) {
                        this.probers[2] = new ProberLatin1();
                    }
                }
            } else {
                if (this.inputState == InputState.PURE_ASCII &&
                        (c == 0x1B || (c == 0x7B && this.lastChar == 0x7E))) {
                    this.inputState = InputState.ESC_ASCII;
                }
                this.lastChar = buf[i];
            }
        } // for end

        ProberCharset.ProbingState st;
        if (this.inputState == InputState.ESC_ASCII) {
            if (this.escProberCharset == null) {
                this.escProberCharset = new ProberEsc();
            }
            st = this.escProberCharset.handleData(buf, offset, length);
            if (st == ProberCharset.ProbingState.FOUND_IT) {
                this.done = true;
                this.detectedCharset = this.escProberCharset.getCharSetName();
            }
        } else if (this.inputState == InputState.HIGHBYTE) {
            for (int i = 0; i < this.probers.length; ++i) {
                st = this.probers[i].handleData(buf, offset, length);
                if (st == ProberCharset.ProbingState.FOUND_IT) {
                    this.done = true;
                    this.detectedCharset = this.probers[i].getCharSetName();
                    return;
                }
            }
        } else { // pure ascii
            // do nothing
        }
    }

    public void dataEnd() {
        if (!this.gotData) {
            return;
        }

        if (this.detectedCharset != null) {
            this.done = true;
            if (this.listener != null) {
                this.listener.report(this.detectedCharset);
            }
            return;
        }

        if (this.inputState == InputState.HIGHBYTE) {
            float proberConfidence;
            float maxProberConfidence = 0.0f;
            int maxProber = 0;

            for (int i = 0; i < this.probers.length; ++i) {
                proberConfidence = this.probers[i].getConfidence();
                if (proberConfidence > maxProberConfidence) {
                    maxProberConfidence = proberConfidence;
                    maxProber = i;
                }
            }

            if (maxProberConfidence > MINIMUM_THRESHOLD) {
                this.detectedCharset = this.probers[maxProber].getCharSetName();
                if (this.listener != null) {
                    this.listener.report(this.detectedCharset);
                }
            }
        } else if (this.inputState == InputState.ESC_ASCII) {
            // do nothing
        } else {
            // do nothing
        }
    }

    public void reset() {
        this.done = false;
        this.start = true;
        this.detectedCharset = null;
        this.gotData = false;
        this.inputState = InputState.PURE_ASCII;
        this.lastChar = 0;

        if (this.escProberCharset != null) {
            this.escProberCharset.reset();
        }

        for (int i = 0; i < this.probers.length; ++i) {
            if (this.probers[i] != null) {
                this.probers[i].reset();
            }
        }
    }
}