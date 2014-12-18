package algorithm;

import entry.Base;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by Epsirom on 14/12/17.
 */
public class MP3WAVReader {

    private static Logger logger = Base.logger(MP3WAVReader.class);

    public File file = null;
    public AudioFormat baseFormat = null;
    public AudioFormat decodedFormat = null;
    public AudioFileFormat baseFileFormat = null;
    public byte[] data = null;

    public MP3WAVReader(String filename) throws Exception {
        file = new File(filename);
        initialize();
    }

    private void initialize() throws Exception {
        AudioInputStream in = AudioSystem.getAudioInputStream(file);
        AudioInputStream din = null;
        baseFormat = in.getFormat();
        baseFileFormat = AudioSystem.getAudioFileFormat(file);
        if (baseFileFormat.getType().toString().equals("WAVE")) {
            data = new byte[in.available()];
            in.read(data);
            return;
        } else if (baseFileFormat.getType().toString().equals("MP3")) {
            int duration = Integer.parseInt(baseFileFormat.properties().get("duration").toString());
            //data = new byte[(int)((double)duration / 1000000 * baseFormat.getSampleRate() * baseFormat.getChannels() * 2)];
            decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            getRaw(decodedFormat, din);
        } else {
            throw new UnsupportedAudioFileException();
        }
    }

    private void getRaw(AudioFormat targetFormat, AudioInputStream din) throws Exception {
        int nBytesRead = 0;
        byte[] buf = new byte[4096];
        ArrayList<Byte> tmp = new ArrayList<Byte>();
        while (nBytesRead >= 0) {
            nBytesRead = din.read(buf, 0, buf.length);
            for (int i = 0; i < nBytesRead; ++i) {
                tmp.add(buf[i]);
            }
        }
        data = new byte[tmp.size()];
        for (int i = 0; i < data.length; ++i) {
            data[i] = tmp.get(i);
        }
        din.close();
    }

}
