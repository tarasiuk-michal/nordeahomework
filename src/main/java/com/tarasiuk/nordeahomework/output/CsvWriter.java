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

/**
 * Writes processed sentences to a CSV file. Uses a two-pass approach: first writes words to a
 * temporary file to determine the maximum number of words per sentence, then writes the final CSV
 * with a header row and sentence numbers to the target file. Implements {@link AutoCloseable} for
 * resource management.
 */
public class CsvWriter implements AutoCloseable {
  public static final String DELIMITER = ", ";
  public static final String NEWLINE = System.lineSeparator();
  private static final Logger logger = LoggerFactory.getLogger(CsvWriter.class);
  private final Path finalOutputFile;
  private final Path tempFile;
  private final BufferedWriter tempWriter;
  private int maxWords = 0;
  private int sentenceCount = 0;

  /**
   * Constructs a CsvWriter that will write to the specified output file path. Creates a temporary
   * file for intermediate processing.
   *
   * @param outputFile The path to the target CSV file.
   * @throws IOException If an I/O error occurs creating the temporary file or writers.
   */
  public CsvWriter(Path outputFile) throws IOException {
    this.finalOutputFile = outputFile;
    this.tempFile = Files.createTempFile("csv_writer_temp_", ".tmp");
    this.tempWriter = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8);
    logger.info("Writing sentence data to temporary file: {}", tempFile.toAbsolutePath());
  }

  /**
   * Writes a list of sentences to the temporary file. Each line in the temporary file contains the
   * comma-separated, escaped words of one sentence. Updates the maximum word count encountered.
   *
   * @param sentences The list of {@link Sentence} objects to write. Can be null or empty (will be
   *     skipped).
   * @throws IOException If an I/O error occurs writing to the temporary file.
   */
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

  /**
   * Closes the temporary file writer, generates the final CSV file (including header and sentence
   * numbers) based on the temporary file content, and deletes the temporary file. This method
   * should be called when all sentences have been written, typically via a try-with-resources
   * statement.
   *
   * @throws IOException If an I/O error occurs closing the temporary writer, reading the temporary
   *     file, writing the final file, or deleting the temporary file.
   */
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

  /**
   * Reads the temporary file, writes the header row, and then writes each sentence line prefixed
   * with its sentence number to the final output file. Deletes the temporary file upon completion
   * or failure.
   *
   * @throws IOException If an I/O error occurs during file operations.
   */
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

  /** Deletes the temporary file used for intermediate storage. Logs errors if deletion fails. */
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

  /**
   * Writes the header row to the final CSV file. The header includes "Sentence No." followed by
   * "Word 1", "Word 2", ..., up to the maximum number of words found.
   *
   * @param finalWriter The writer for the final output file.
   * @throws IOException If an I/O error occurs writing the header.
   */
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

  /**
   * Escapes a string field for safe inclusion in a CSV file according to basic CSV rules (quoting
   * fields containing delimiters, newlines, or quotes, and doubling internal quotes).
   *
   * @param field The string field to escape. Can be null.
   * @return The escaped string field, or an empty string if the input field was null.
   */
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
