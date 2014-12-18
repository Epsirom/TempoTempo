package entry;

import algorithm.Data;
import algorithm.TempoAnalyzer;
import algorithm.WaveRender;
import org.apache.commons.cli.*;
import org.slf4j.Logger;

import java.util.List;

public class Main {

    private static Logger logger = Base.logger(Main.class);

    public static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TempoTempo", options);
    }

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("h", "help", false, "Show this maessage.");
        options.addOption(OptionBuilder.withLongOpt("sound-file")
                .withDescription("The path of the sound file. If given, will only analyze the sound file.")
                .hasArg()
                .withArgName("SOUND-FILE")
                .create("s")
        );
        options.addOption(OptionBuilder.withLongOpt("input")
                .withDescription("The path of the input data file.")
                .hasArg()
                .withArgName("INPUT-PATH")
                .create("i")
        );
        options.addOption(OptionBuilder.withLongOpt("output")
                .withDescription("The path of the output data file.")
                .hasArg()
                .withArgName("OUTPUT-PATH")
                .create("o")
        );
        options.addOption(OptionBuilder.withLongOpt("answer")
                .withDescription("The path of the correct answer data file.")
                .hasArg()
                .withArgName("ANSWER-PATH")
                .create("a")
        );
        options.addOption(OptionBuilder.withLongOpt("render-wave")
                .withDescription("Render the wave into image.")
                .hasArg()
                .withArgName("IMAGE-PATH")
                .create("rw")
        );
        options.addOption(OptionBuilder.withLongOpt("render-spectrogram")
                .withDescription("Render the spectrogram into image.")
                .hasArg()
                .withArgName("IMAGE-PATH")
                .create("rs")
        );
        CommandLineParser parser = new BasicParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                printHelp(options);
                return;
            }
            if (cmd.hasOption("sound-file")) {
                Config.sound_file = cmd.getOptionValue("sound-file");
                if (cmd.hasOption("render-wave")) {
                    WaveRender.renderWave(Config.sound_file, cmd.getOptionValue("render-wave"));
                }
                if (cmd.hasOption("render-spectrogram")) {
                    WaveRender.renderWave(Config.sound_file, cmd.getOptionValue("render-spectrogram"));
                }
                try {
                    int tempo = TempoAnalyzer.analyzeTempo(Config.sound_file);
                    System.out.println(tempo);
                } catch (Exception e) {
                    logger.error(e.toString());
                    e.printStackTrace();
                }
            } else {
                Config.input_path = cmd.getOptionValue("input", "input.data");
                Config.output_path = cmd.getOptionValue("output", "output.data");

                List<String> paths = Data.readStringFile(Config.input_path);
                List<Integer> results = TempoAnalyzer.analyzeTempo(paths);
                Data.writeIntFile(Config.output_path, results);

                if (cmd.hasOption("answer")) {
                    Config.answer_path = cmd.getOptionValue("answer");
                    List<Integer> answers = Data.readIntFile(Config.answer_path);
                    double mse = Data.getMSE(results, answers, true);
                    System.out.println(mse);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            printHelp(options);
        }
    }
}
