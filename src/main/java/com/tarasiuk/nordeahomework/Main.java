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
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final String DEFAULT_INPUT_DIR = "src/test/resources/in";
  private static final String DEFAULT_OUTPUT_DIR = "src/test/resources/out";

  public static void main(String[] args) {
    try {
      List<Path> filePaths = createFilePaths(args);

      Path inputFile = filePaths.get(0);
      Path xmlOutputFile = filePaths.get(1);
      Path csvOutputFile = filePaths.get(2);

      process(inputFile, xmlOutputFile, csvOutputFile);
    } catch (IOException e) {
      logger.error("Initialization or processing failed: {}", e.getMessage(), e);
      System.exit(1);
    } catch (XMLStreamException e) {
      logger.error("XML processing failed: {}", e.getMessage(), e);
      System.exit(1);
    } catch (Exception e) {
      logger.error("An unexpected error occurred during processing: {}", e.getMessage(), e);
      System.exit(1);
    }
  }

  public static void process(Path inputFile, Path xmlOutputFile, Path csvOutputFile)
      throws IOException, XMLStreamException {
    logger.info("Starting processing for file: {}", inputFile.getFileName());
    long startTime = System.currentTimeMillis();

    try (Processor processor = new Processor(inputFile);
        XmlWriter xmlWriter = new XmlWriter(xmlOutputFile);
        CsvWriter csvWriter = new CsvWriter(csvOutputFile)) {

      xmlWriter.openDocument();

      List<Sentence> batch;
      int sentenceCount = 0;
      while (!(batch = processor.readNextSentences()).isEmpty()) {
        xmlWriter.writeSentences(batch);
        csvWriter.writeSentences(batch);
        sentenceCount += batch.size();
      }
      logger.info("Successfully processed {} sentences.", sentenceCount);
    } // try-with-resources ensures close() is called

    long endTime = System.currentTimeMillis();
    logger.info("Processing finished in {} ms.", (endTime - startTime));
  }

  private static List<Path> createFilePaths(String[] args) throws IOException {
    String inputFileName;
    if (args.length > 0) {
      inputFileName = args[0];
    } else {
      inputFileName = "small.in";
      logger.warn("No input file specified, using default: {}", inputFileName);
    }

    String outputDirName;
    if (args.length > 1) {
      outputDirName = args[1];
    } else {
      outputDirName = DEFAULT_OUTPUT_DIR;
      logger.warn("No output directory specified, using default: {}", outputDirName);
    }

    String outputName = inputFileName.substring(0, inputFileName.lastIndexOf('.'));
    Path inputFile;
    Path xmlOutputFile;
    Path csvOutputFile;

    try {
      inputFile = Paths.get(DEFAULT_INPUT_DIR, inputFileName);

      if (!Files.exists(inputFile)) {
        throw new IOException("Input file not found at " + inputFile.toAbsolutePath());
      }

      Path outputDir = Paths.get(outputDirName);
      Files.createDirectories(outputDir);

      xmlOutputFile = outputDir.resolve(outputName + ".xml");
      csvOutputFile = outputDir.resolve(outputName + ".csv");

    } catch (InvalidPathException e) {
      throw new IOException("Error setting up file paths: " + e.getMessage(), e);
    }

    logger.info("Input file: {}", inputFile.toAbsolutePath());
    logger.info("XML output file: {}", xmlOutputFile.toAbsolutePath());
    logger.info("CSV output file: {}", csvOutputFile.toAbsolutePath());

    return List.of(inputFile, xmlOutputFile, csvOutputFile);
  }
}
