package algorithm;

import entry.Base;
import org.slf4j.Logger;

/**
 * Created by Epsirom on 14/12/17.
 */
public class SimpleFFT {
    private static Logger logger = Base.logger(SimpleFFT.class);

    public int bufferSize = 0;
    public int sampleRate = 0;
    public float[] spectrum = null;
    public float[] real = null;
    public float[] imag = null;
    public int[] reverseTable = null;
    public float[] sinTable = null;
    public float[] cosTable = null;

    public SimpleFFT(int bufferSize, int sampleRate) {
        this.bufferSize = bufferSize;
        this.sampleRate = sampleRate;
        spectrum = new float[bufferSize / 2];
        real = new float[bufferSize];
        imag = new float[bufferSize];
        reverseTable = new int[bufferSize];
        sinTable = new float[bufferSize];
        cosTable = new float[bufferSize];

        int limit = 1, bit = bufferSize >> 1;
        while (limit < bufferSize) {
            for (int i = 0; i < limit; ++i) {
                reverseTable[i + limit] = reverseTable[i] + bit;
            }
            limit = limit << 1;
            bit = bit >> 1;
        }

        for (int i = 1; i < bufferSize; ++i) {
            sinTable[i] = (float)Math.sin(-Math.PI / i);
            cosTable[i] = (float)Math.cos(-Math.PI / i);
        }
    }

    public void forward(float[] buffer) {
        if (bufferSize != buffer.length) {
            logger.warn("Buffer size not match, ignored! FFT size: {}, buffer size: {}.", bufferSize, buffer.length);
            return;
        }
        for (int i = 0; i < bufferSize; ++i) {
            real[i] = buffer[reverseTable[i]];
            imag[i] = 0;
        }

        int halfSize = 1;
        while (halfSize < bufferSize) {
            float phaseShiftStepReal = cosTable[halfSize];
            float phaseShiftStepImag = sinTable[halfSize];
            float currentPhaseShiftReal = 1.0F;
            float currentPhaseShiftImag = 0.0F;
            for (int fftStep = 0; fftStep < halfSize; ++fftStep) {    //FFT Step
                int i = fftStep;
                while (i < bufferSize) {
                    int off = i + halfSize;
                    float tr = (currentPhaseShiftReal * real[off]) - (currentPhaseShiftImag * imag[off]);
                    float ti = (currentPhaseShiftReal * imag[off]) + (currentPhaseShiftImag * real[off]);

                    real[off] = real[i] - tr;
                    imag[off] = imag[i] - ti;
                    real[i] += tr;
                    imag[i] += ti;

                    i += (halfSize << 1);
                }

                float tmpReal = currentPhaseShiftReal;
                currentPhaseShiftReal = (tmpReal * phaseShiftStepReal) - (currentPhaseShiftImag * phaseShiftStepImag);
                currentPhaseShiftImag = (tmpReal * phaseShiftStepImag) + (currentPhaseShiftImag * phaseShiftStepReal);
            }
            halfSize <<= 1;
        }
        for (int i = bufferSize / 2 - 1; i >= 0; --i) {
            spectrum[i] = 2 * (float)Math.sqrt((double)(real[i] * real[i] + imag[i] * imag[i])) / bufferSize;
        }
    }
}
