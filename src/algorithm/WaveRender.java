package algorithm;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;

/**
 * Created by Epsirom on 14/12/18.
 */
public class WaveRender {

    public static float timeStep = 0.01F;

    public static void renderWave(String wav_path, String img_path) {
        Wave wave = new Wave(wav_path);

        GraphicRender render = new GraphicRender();
        render.renderWaveform(wave, timeStep, img_path);
    }

    public static void renderSpectrogram(String wave_path, String img_path) {
        Wave wave = new Wave(wave_path);

        GraphicRender render = new GraphicRender();
        render.renderSpectrogram(wave.getSpectrogram(), img_path);
    }
}
