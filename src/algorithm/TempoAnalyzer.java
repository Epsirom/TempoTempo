package algorithm;

import com.musicg.graphic.GraphicRender;
import com.musicg.wave.Wave;
import com.sun.media.sound.AudioFloatInputStream;
import entry.Base;
import org.slf4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Epsirom on 14/12/17.
 */
public class TempoAnalyzer {
    private static Logger logger = Base.logger(TempoAnalyzer.class);

    public class TAConfig {
        public int DETECTION_RANGES = 128;
        public float DETECTION_RATE = 12.0F;
        public float DETECTION_FACTOR = 0.915F;
        public float QUALITY_DECAY = 0.6F;
        public float QUALITY_TOLERANCE = 0.96F;
        public float QUALITY_REWARD = 10.0F;
        public float QUALITY_STEP = 0.1F;
        public int MINIMUM_CONTRIBUTIONS = 6;
        public float FINISH_LINE = 60.0F;
        public float[] REWARD_TOLERANCES = new float[] {0.001F, 0.005F, 0.01F, 0.02F, 0.04F, 0.08F, 0.10F, 0.15F, 0.30F};
        public float[] REWARD_MULTIPLIERS = new float[] {20.0F, 10.0F, 8.0F, 1.0F, 0.5F, 0.25F, 0.125F, 1.0F/16.0F, 1.0F/32.0F};
    }

    public TAConfig config = new TAConfig();
    public float BPM_MIN = 85.0F;
    public float BPM_MAX = 169.0F;
    public int beat_counter = 0;
    public int half_counter = 0;
    public int quarter_counter = 0;

    public float[] a_freq_range = new float[config.DETECTION_RANGES];
    public float[] ma_freq_range = new float[config.DETECTION_RANGES];
    public float[] maa_freq_range = new float[config.DETECTION_RANGES];
    public float[] last_detection = new float[config.DETECTION_RANGES];

    public float[] ma_bpm_range = new float[config.DETECTION_RANGES];
    public float[] maa_bpm_range = new float[config.DETECTION_RANGES];

    public float[] detection_quality = new float[config.DETECTION_RANGES];
    public boolean[] detection = new boolean[config.DETECTION_RANGES];

    public float ma_quality_avg = 0;
    public float ma_quality_total = 0;
    public HashMap<Integer, Float> bpm_contest = new HashMap<Integer, Float>();
    public HashMap<Integer, Float> bpm_contest_lo = new HashMap<Integer, Float>();
    public float quality_total = 0;
    public float quality_avg = 0;
    public float current_bpm = 0;
    public float current_bpm_lo = 0;
    public float winning_bpm = 0;
    public float win_val = 0;
    public float winning_bpm_lo = 0;
    public float win_val_lo = 0;
    public int win_bpm_int = 0;
    public int win_bpm_int_lo = 0;
    public float bpm_predict = 0;
    public boolean is_erratic = false;
    public float bpm_offset = 0;
    public float last_timer = 0;
    public float last_update = 0;
    public float bpm_timer = 0;

    public TempoAnalyzer() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < config.DETECTION_RANGES; ++i) {
            a_freq_range[i] = 0;
            ma_freq_range[i] = 0;
            maa_freq_range[i] = 0;
            last_detection[i] = 0;
            ma_bpm_range[i] = maa_freq_range[i] =
                    60.0F / BPM_MIN + ((60.0F / BPM_MAX - 60.0F / BPM_MIN) * ((float)i / config.DETECTION_RANGES));
            detection_quality[i] = 0;
            detection[i] = false;
        }
        ma_quality_avg = 0;
        ma_quality_total = 0;
        bpm_contest = new HashMap<Integer, Float>();
        bpm_contest_lo = new HashMap<Integer, Float>();
        quality_total = 0;
        quality_avg = 0;
        current_bpm = 0;
        current_bpm_lo = 0;
        winning_bpm = 0;
        win_val = 0;
        winning_bpm_lo = 0;
        win_val_lo = 0;
        win_bpm_int = 0;
        win_bpm_int_lo = 0;
        bpm_predict = 0;
        is_erratic = false;
        bpm_offset = 0;
        last_timer = 0;
        last_update = 0;
        bpm_timer = 0;
        beat_counter = 0;
        half_counter = 0;
        quarter_counter = 0;
    }

    public void process(float timer_seconds, float[] fft_data) {
        if (last_timer == 0) {
            last_timer = timer_seconds;
            return;
        }
        if (last_timer > timer_seconds) {
            reset();
            return;
        }
        last_update = timer_seconds - last_timer;
        last_timer = timer_seconds;
        if (last_update > 1.0) {
            reset();
            return;
        }

        float bpm_floor = 60.0F / BPM_MAX;
        float bpm_ceil = 60.0F / BPM_MIN;

        int range_step = fft_data.length / config.DETECTION_RANGES;
        int range = 0;

        for (int x = 0; x < fft_data.length; x += range_step) {
            a_freq_range[range] = 0;

            for (int i = x; i < x + range_step; ++i) {
                float v = Math.abs(fft_data[i]);
                a_freq_range[range] += v;
            }

            a_freq_range[range] /= range_step;
            ma_freq_range[range] -= (ma_freq_range[range] - a_freq_range[range]) * last_update * config.DETECTION_RATE;
            maa_freq_range[range] -= (maa_freq_range[range] - ma_freq_range[range]) * last_update * config.DETECTION_RATE;

            boolean det = ma_freq_range[range] * config.DETECTION_FACTOR >= maa_freq_range[range];
            if (ma_bpm_range[range] > bpm_ceil) {
                ma_bpm_range[range] = bpm_ceil;
            }
            if (ma_bpm_range[range] < bpm_floor) {
                ma_bpm_range[range] = bpm_floor;
            }
            if (maa_bpm_range[range] > bpm_ceil) {
                maa_bpm_range[range] = bpm_ceil;
            }
            if (maa_bpm_range[range] < bpm_floor) {
                maa_bpm_range[range] = bpm_floor;
            }

            boolean rewarded = false;

            if (!this.detection[range] && det) {
                float trigger_gap = timer_seconds - last_detection[range];
                if (trigger_gap < bpm_ceil && trigger_gap > bpm_floor) {
                    for (int i = 0; i < config.REWARD_TOLERANCES.length; ++i) {
                        if (Math.abs(ma_bpm_range[range] - trigger_gap) < ma_bpm_range[range] * config.REWARD_TOLERANCES[i]) {
                            detection_quality[range] += config.QUALITY_REWARD * config.REWARD_MULTIPLIERS[i];
                            rewarded = true;
                        }
                    }
                    if (rewarded) {
                        last_detection[range] = timer_seconds;
                    }
                } else if (trigger_gap >= bpm_ceil) {
                    trigger_gap /= 2.0F;

                    if (trigger_gap < bpm_ceil && trigger_gap > bpm_floor) {
                        for (int i = 0; i < config.REWARD_TOLERANCES.length; ++i) {
                            if (Math.abs(ma_bpm_range[range] - trigger_gap) < ma_bpm_range[range] * config.REWARD_TOLERANCES[i]) {
                                detection_quality[range] += config.QUALITY_REWARD * config.REWARD_MULTIPLIERS[i];
                                rewarded = true;
                            }
                        }
                    }
                    if (!rewarded) {
                        trigger_gap *= 2.0F;
                    }
                    last_detection[range] = timer_seconds;
                }

                if (rewarded) {
                    float qmp = detection_quality[range] / quality_avg * config.QUALITY_STEP;
                    if (qmp > 1.0F) {
                        qmp = 1.0F;
                    }
                    ma_bpm_range[range] -= (ma_bpm_range[range] - trigger_gap) * qmp;
                    maa_bpm_range[range] -= (maa_bpm_range[range] - ma_bpm_range[range]) * qmp;
                } else if (trigger_gap >= bpm_floor && trigger_gap <= bpm_ceil) {
                    if (detection_quality[range] < quality_avg * config.QUALITY_TOLERANCE && current_bpm != 0) {
                        ma_bpm_range[range] -= (ma_bpm_range[range] - trigger_gap) * config.QUALITY_STEP;
                        maa_bpm_range[range] -= (maa_bpm_range[range] - ma_bpm_range[range]) * config.QUALITY_STEP;
                    }
                    detection_quality[range] -= config.QUALITY_STEP;
                } else if (trigger_gap >= bpm_ceil) {
                    if (detection_quality[range] < quality_avg * config.QUALITY_TOLERANCE && current_bpm != 0) {
                        ma_bpm_range[range] -= (ma_bpm_range[range] - current_bpm) * 0.5F;
                        maa_bpm_range[range] -= (maa_bpm_range[range] - ma_bpm_range[range]) * 0.5F;
                    }
                    detection_quality[range] -= config.QUALITY_REWARD * config.QUALITY_STEP;
                }
            }
            if ((!rewarded && timer_seconds - last_detection[range] > bpm_ceil) || (det && Math.abs(ma_bpm_range[range] - current_bpm) > bpm_offset)) {
                detection_quality[range] -= detection_quality[range] * config.QUALITY_STEP * config.QUALITY_DECAY * last_update;
            }

            if (detection_quality[range] < 0.001F) {
                detection_quality[range] = 0.001F;
            }

            detection[range] = det;
            ++range;
        }

        quality_total = 0;
        float bpm_total = 0;
        float bpm_contributions = 0;

        for (int i = 0; i < config.DETECTION_RANGES; ++i) {
            quality_total += detection_quality[i];
        }
        quality_avg = quality_total / config.DETECTION_RANGES;
        if (quality_total != 0) {
            ma_quality_avg += (quality_avg - ma_quality_avg) * last_update * config.DETECTION_RATE / 2.0F;
            ma_quality_total += (quality_total - ma_quality_total) * last_update * config.DETECTION_RATE / 2.0F;
            ma_quality_avg -= 0.98F * ma_quality_avg * last_update * 3.0F;
        } else {
            quality_avg = 0.001F;
        }
        if (ma_quality_total <= 0) {
            ma_quality_total = 1.0F;
        }
        if (ma_quality_avg <= 0) {
            ma_quality_avg = 1.0F;
        }

        float avg_bpm_offset = 0;
        float offset_test_bpm = current_bpm;
        HashMap<Integer, Float> draft = new HashMap<Integer, Float>();

        if (quality_avg != 0) {
            for (int i = 0; i < config.DETECTION_RANGES; ++i) {
                if (detection_quality[i] * config.QUALITY_TOLERANCE >= ma_quality_avg) {
                    if (maa_bpm_range[i] < bpm_ceil && maa_bpm_range[i] > bpm_floor) {
                        bpm_total += maa_bpm_range[i];
                        float draft_float = Math.round((60.0F / maa_bpm_range[i]) * 1000.0F);
                        if (Math.abs(Math.ceil(draft_float) - (60.0F / current_bpm) * 1000.0F) < (Math.abs(Math.floor(draft_float) - (60.0F / current_bpm * 1000.0F)))) {
                            draft_float = (float)Math.ceil(draft_float / 10.0);
                        } else {
                            draft_float = (float)Math.floor(draft_float / 10.0);
                        }
                        int draft_int = (int)(draft_float / 10);
                        float old_draft = 0.0F;
                        if (draft.containsKey(draft_int)) {
                            old_draft = draft.get(draft_int);
                        }
                        old_draft += detection_quality[i] / quality_avg;
                        draft.put(draft_int, old_draft);
                        ++bpm_contributions;
                        if (offset_test_bpm == 0) {
                            offset_test_bpm = maa_bpm_range[i];
                        } else {
                            avg_bpm_offset += Math.abs(offset_test_bpm - maa_bpm_range[i]);
                        }
                    }
                }
            }
        }

        boolean has_prediction = (bpm_contributions >= config.MINIMUM_CONTRIBUTIONS);

        int draft_winner = 0;
        float win_val = 0;

        if (has_prediction) {
            for (int k : draft.keySet()) {
                float d = draft.get(k);
                if (d > win_val) {
                    win_val = d;
                    draft_winner = k;
                }
            }
            bpm_predict = 60.0F / (draft_winner / 10.0F);
            avg_bpm_offset /= bpm_contributions;
            bpm_offset = avg_bpm_offset;

            if (current_bpm == 0) {
                current_bpm = bpm_predict;
            }
        }

        if (current_bpm != 0 && bpm_predict != 0) {
            current_bpm -= (current_bpm - bpm_predict) * last_update;
        }

        float contest_max = 0;

        for (int contest_i : bpm_contest.keySet()) {
            if (contest_max < bpm_contest.get(contest_i)) {
                contest_max = bpm_contest.get(contest_i);
            }
            if (bpm_contest.get(contest_i) > config.FINISH_LINE / 2.0F) {
                int draft_int_lo = (int)Math.round((contest_i) / 10.0);
                float old_bpm_contest_lo = 0.0F;
                if (bpm_contest_lo.containsKey(draft_int_lo)) {
                    old_bpm_contest_lo = bpm_contest_lo.get(draft_int_lo);
                }
                old_bpm_contest_lo += (bpm_contest.get(contest_i) / 6.0F) * last_update;
                bpm_contest_lo.put(contest_i, old_bpm_contest_lo);
            }
        }

        if (contest_max > config.FINISH_LINE) {
            for (int contest_i : bpm_contest.keySet()) {
                bpm_contest.put(contest_i, bpm_contest.get(contest_i) / contest_max * config.FINISH_LINE);
            }
        }

        contest_max = 0;
        for (int contest_i : bpm_contest_lo.keySet()) {
            if (contest_max < bpm_contest_lo.get(contest_i)) {
                contest_max = bpm_contest_lo.get(contest_i);
            }
        }

        if (contest_max > config.FINISH_LINE) {
            for (int contest_i : bpm_contest_lo.keySet()) {
                bpm_contest_lo.put(contest_i, bpm_contest_lo.get(contest_i) / contest_max * config.FINISH_LINE);
            }
        }

        for (int contest_i : bpm_contest.keySet()) {
            float tmp = bpm_contest.get(contest_i);
            bpm_contest.put(contest_i, tmp - tmp * (last_update / config.DETECTION_RATE));
        }

        for (int contest_i : bpm_contest_lo.keySet()) {
            float tmp = bpm_contest_lo.get(contest_i);
            bpm_contest_lo.put(contest_i, tmp - tmp * (last_update / config.DETECTION_RATE));
        }

        bpm_timer += last_update;

        int winner = 0;
        int winner_lo = 0;

        if (bpm_timer > winning_bpm / 4.0F && current_bpm != 0) {
            win_val = 0;
            win_val_lo = 0;
            if (winning_bpm != 0) {
                while (bpm_timer > winning_bpm / 4.0F) {
                    bpm_timer -= winning_bpm / 4.0F;
                }
            }

            ++quarter_counter;
            half_counter = quarter_counter / 2;
            beat_counter = quarter_counter / 4;

            int idx = Math.round(60.0F / current_bpm * 10.0F);
            float old_contest = 0.0F;
            if (bpm_contest.containsKey(idx)) {
                old_contest = bpm_contest.get(idx);
            }
            bpm_contest.put(idx, old_contest + config.QUALITY_REWARD);

            for (int contest_i : bpm_contest.keySet()) {
                if (win_val < bpm_contest.get(contest_i)) {
                    winner = contest_i;
                    win_val = bpm_contest.get(contest_i);
                }
            }

            if (winner != 0) {
                win_bpm_int = winner;
                winning_bpm = (60.0F / (winner / 10.0F));
            }

            for (int contest_i : bpm_contest_lo.keySet()) {
                if (win_val_lo < bpm_contest_lo.get(contest_i)) {
                    winner_lo = contest_i;
                    win_val_lo = bpm_contest_lo.get(contest_i);
                }
            }

            if (winner_lo != 0) {
                win_bpm_int_lo = winner_lo;
                winning_bpm_lo = 60.0F / winner_lo;
            }

            //System.out.println(winner);
        }
    }

    public static int analyzeTempo(String path) throws Exception {
        MP3WAVReader reader = new MP3WAVReader(path);
        byte[] data = reader.data;
        int windowSize = 1024;
        TempoAnalyzer analyzer = new TempoAnalyzer();
        int sampleRate = (int)reader.baseFormat.getSampleRate();
        SimpleFFT fft = new SimpleFFT(windowSize, sampleRate);
        int currentStart = 0;
        int channels = reader.baseFormat.getChannels(), bitsPerSample = 16, bytesPerSample = bitsPerSample / 8;
        float[] buf = new float[windowSize];
        int bufIdx = 0;
        long bufInt = 0;
        float currentTime = 0;
        while (currentStart < data.length) {
            // get a buffer.
            for (bufIdx = 0; bufIdx < windowSize; ++bufIdx) {
                buf[bufIdx] = 0;
                if (currentStart < data.length) {
                    for (int c = 0; c < channels; ++c) {
                        bufInt = 0;
                        for (int i = 0; i < bytesPerSample; ++i) {
                            if (currentStart + i < data.length) {
                                bufInt += ((data[currentStart + i] & 0xFF) << (i * 8));
                            }
                        }
                        if (bufInt > (1 << (bitsPerSample - 1))) {
                            bufInt = bufInt - (1 << bitsPerSample);
                        }
                        buf[bufIdx] += (float) bufInt / (1 << (bitsPerSample - 1));
                        currentStart += bytesPerSample;
                    }
                    buf[bufIdx] /= channels;
                }
            }
            currentTime += (float)windowSize / sampleRate;
            fft.forward(buf);
            analyzer.process(currentTime, fft.spectrum);
        }
        return Math.round(analyzer.win_bpm_int / 10.0F);
    }

    public static List<Integer> analyzeTempo(List<String> paths) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (String path : paths) {
            int tempo = 104;
            try {
                tempo = analyzeTempo(path);
            } catch (Exception e) {
                logger.error("Analyze {} failed: {}", path, e.toString());
                e.printStackTrace();
            } finally {
                result.add(tempo);
            }
        }
        return result;
    }
}
