package com.tarasiuk.nordeahomework;

import com.tarasiuk.nordeahomework.domain.Sentence;
import com.tarasiuk.nordeahomework.output.CsvWriter;
import com.tarasiuk.nordeahomework.output.XmlWriter;
import com.tarasiuk.nordeahomework.processing.Processor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.xml.stream.XMLStreamException;

public class Main {
  private static final String DEFAULT_INPUT_DIR = "src/test/resources/in";
  private static final String DEFAULT_OUTPUT_DIR = "src/test/resources/out";

  public static void main(String[] args) {
    try { // Add try-catch block
      List<Path> filePaths = createFilePaths(args);

      Path inputFile = filePaths.get(0);
      Path xmlOutputFile = filePaths.get(1);
      Path csvOutputFile = filePaths.get(2);

      process(inputFile, xmlOutputFile, csvOutputFile);
    } catch (IOException e) {
      System.err.println("Initialization failed: " + e.getMessage());
      System.exit(1);
    } catch (Exception e) {
      System.err.println("An unexpected error occurred during processing: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static void process(Path inputFile, Path xmlOutputFile, Path csvOutputFile) {
    try (Processor processor = new Processor(inputFile);
         XmlWriter xmlWriter = new XmlWriter(xmlOutputFile);
         CsvWriter csvWriter = new CsvWriter(csvOutputFile)) {

      xmlWriter.openDocument();

      List<Sentence> batch;
      int totalSentences = 0;

      while (!(batch = processor.readNextSentences()).isEmpty()) {
        totalSentences += batch.size();
        xmlWriter.writeSentences(batch);
        csvWriter.writeSentences(batch);
      }

      System.out.println("Finished reading input. Total sentences processed: " + totalSentences);

    } catch (IOException e) {
      System.err.println("Error during file reading/writing: " + e.getMessage());
      e.printStackTrace();
    } catch (XMLStreamException e) {
      System.err.println("Error writing XML file: " + e.getMessage());
      e.printStackTrace();
    } catch (Exception e) {
      System.err.println("An unexpected error occurred: " + e.getMessage());
      e.printStackTrace();
    } finally {
      System.out.println("Processing finished.");
    }
  }

  private static List<Path> createFilePaths(String[] args) throws IOException {
    Path inputFile;
    Path xmlOutputFile;
    Path csvOutputFile;

    String inputFileName = args.length > 0 ? args[0] : "small.in";
    String timeSuffix = LocalTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss"));
    String outputName =
        args.length > 1 ? args[1] : inputFileName.replaceFirst("[.][^.]+$", "") + "_" + timeSuffix;

    try {
      inputFile = Paths.get(DEFAULT_INPUT_DIR, inputFileName);

      if (!Files.exists(inputFile)) {
        System.err.println("Error: Input file not found at " + inputFile.toAbsolutePath());
        return null;
      }

      Path outputDir = Paths.get(DEFAULT_OUTPUT_DIR);
      Files.createDirectories(outputDir);

      xmlOutputFile = outputDir.resolve(outputName + ".xml");
      csvOutputFile = outputDir.resolve(outputName + ".csv");

    } catch (InvalidPathException e) {
      throw new IOException("Error setting up file paths: " + e.getMessage(), e);
    }

    System.out.println("Input file: " + inputFile.toAbsolutePath());
    System.out.println("XML output file: " + xmlOutputFile.toAbsolutePath());
    System.out.println("CSV output file: " + csvOutputFile.toAbsolutePath());

    return List.of(inputFile, xmlOutputFile, csvOutputFile);
  }
}
