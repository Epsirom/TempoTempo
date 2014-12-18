package algorithm;

import entry.Base;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Epsirom on 14/12/17.
 */
public class Data {

    private static Logger logger = Base.logger(Data.class);

    public static List<Integer> readIntFile(String path) throws Exception {
        ArrayList<Integer> result = new ArrayList<Integer>();
        BufferedReader in = new BufferedReader(new FileReader(path));
        String buf = in.readLine();
        while (buf != null) {
            int tmp = Integer.parseInt(buf);
            if (!result.add(tmp)) {
                logger.warn("Add int {} from buffer {} failed.", tmp, buf);
            }
            buf = in.readLine();
        }
        return result;
    }

    public static List<String> readStringFile(String path) throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new FileReader(path));
        String buf = in.readLine();
        while (buf != null) {
            if (!result.add(buf)) {
                logger.warn("Add buffer {} failed.", buf);
            }
            buf = in.readLine();
        }
        return result;
    }

    public static void writeIntFile(String path, List<Integer> result) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(path));
        for (int r : result) {
            out.write(r);
            out.newLine();
        }
        out.close();
    }

    public static double getMSE(List<Integer> result, List<Integer> answer, boolean debug) {
        if (result.size() != answer.size()) {
            logger.warn("Size of result and answer not matched.");
        }
        int answerSize = answer.size();
        double tmp = 0;
        for (int i = 0; i < answerSize; ++i) {
            if (debug) {
                logger.info("{}\tResult:{}\tAnswer:{}", i, result.get(i), answer.get(i));
            }
            int offMean = result.get(i) - answer.get(i);
            tmp += (offMean * offMean);
        }
        return (tmp / answerSize);
    }
}
