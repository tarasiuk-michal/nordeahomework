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

public class CsvWriter implements AutoCloseable {

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
    System.out.println("Writing sentence data to temporary file: " + tempFile.toAbsolutePath());
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
    if (tempWriter != null) {
      tempWriter.close();
    }

    System.out.println(
        "Temporary file writing complete. Max words found: "
            + maxWords
            + ". Sentences: "
            + sentenceCount);

    writeFinalFile();

    System.out.println("CsvWriter closed.");
  }

  private void writeFinalFile() throws IOException {
    System.out.println("Writing final CSV file: " + finalOutputFile.toAbsolutePath());
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

    } finally {
      deleteTempFile();
    }
  }

  private void deleteTempFile() {
    try {
      Files.delete(tempFile);
    } catch (IOException e) {
      System.err.println(
          "Error deleting temporary file: " + tempFile.toAbsolutePath() + " - " + e.getMessage());
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
