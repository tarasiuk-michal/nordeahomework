package com.tarasiuk.nordeahomework.output;

import com.tarasiuk.nordeahomework.domain.Sentence;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvWriter implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(CsvWriter.class);

  public static final String DELIMITER = ", ";
  public static final String NEWLINE = System.lineSeparator();
  private final Path finalOutputFile;
  private final Path tempFile;
  private final BufferedWriter tempWriter;
  private int maxWords = 0;
  private int sentenceCount = 0;

  public CsvWriter(Path outputFile) throws IOException {
    this.finalOutputFile = outputFile;
    this.tempFile = Files.createTempFile("csv_writer_temp_", ".tmp");
    this.tempWriter = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8);
    logger.info("Writing sentence data to temporary file: {}", tempFile.toAbsolutePath());
  }

  public void writeSentences(List<Sentence> sentences) throws IOException {
    if (sentences == null || sentences.isEmpty()) {
      return;
    }

    for (Sentence sentence : sentences) {
      List<String> words = sentence.words();
      if (words.size() > this.maxWords) {
        this.maxWords = words.size();
      }

      tempWriter.write(
          words.stream().map(this::escapeCsvField).collect(Collectors.joining(DELIMITER)));
      tempWriter.write(NEWLINE);
      sentenceCount++;
    }
    tempWriter.flush();
  }

  @Override
  public void close() throws IOException {
    logger.debug("Closing CsvWriter.");
    if (tempWriter != null) {
      try {
        tempWriter.close();
      } catch (IOException e) {
        logger.warn("Error closing temporary writer: {}", e.getMessage(), e);
      }
    }

    logger.info(
        "Temporary file writing complete. Max words found: {}. Sentences: {}",
        maxWords,
        sentenceCount);

    try {
      writeFinalFile();
    } catch (IOException e) {
      logger.error("Failed to write final CSV file: {}", e.getMessage(), e);
      deleteTempFile();
      throw e;
    }

    logger.info("CsvWriter closed."); // Confirmation message
  }

  private void writeFinalFile() throws IOException {
    logger.info("Writing final CSV file: {}", finalOutputFile.toAbsolutePath());
    try (BufferedReader tempReader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8);
        BufferedWriter finalWriter =
            Files.newBufferedWriter(finalOutputFile, StandardCharsets.UTF_8)) {

      writeFinalHeader(finalWriter);

      String line;
      int currentSentenceNum = 1;
      while ((line = tempReader.readLine()) != null) {
        finalWriter.write("Sentence ");
        finalWriter.write(String.valueOf(currentSentenceNum++));
        finalWriter.write(DELIMITER);
        finalWriter.write(line);
        finalWriter.write(NEWLINE);
      }
      logger.debug("Finished writing content to final CSV file.");

    } finally {
      deleteTempFile();
    }
  }

  private void deleteTempFile() {
    try {
      if (Files.exists(tempFile)) {
        Files.delete(tempFile);
        logger.debug("Temporary file deleted: {}", tempFile.toAbsolutePath());
      }
    } catch (IOException e) {
      logger.error(
          "Error deleting temporary file: {} - {}", tempFile.toAbsolutePath(), e.getMessage(), e);
    }
  }

  private void writeFinalHeader(BufferedWriter finalWriter) throws IOException {
    if (maxWords > 0) {
      StringJoiner header = new StringJoiner(DELIMITER);

      for (int i = 1; i <= maxWords; i++) {
        String s = "Word " + i;
        header.add(s);
      }

      finalWriter.write(DELIMITER + header);
      finalWriter.write(NEWLINE);
    }
  }

  private String escapeCsvField(String field) {
    if (field == null) {
      return "";
    }

    if (field.contains(",") || field.contains("\n") || field.contains("\"")) {
      String escapedField = field.replace("\"", "\"\"");
      return "\"" + escapedField + "\"";
    } else {
      return field;
    }
  }
}
