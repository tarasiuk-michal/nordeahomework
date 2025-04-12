package com.tarasiuk.nordeahomework.processing;

import com.tarasiuk.nordeahomework.domain.Sentence;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Processor implements AutoCloseable {

  public static final int BUFFER_SIZE = 1024;
  private final BufferedReader reader;
  private final char[] charBuffer;
  private final StringBuilder buffer = new StringBuilder();
  private boolean eofReached = false;

  public static final String PUNCTUATION_REGEX = "^[.,!?:;()\"']+|[.,!?:;()\"']+$";
  private static final Pattern SENTENCE_END_PATTERN = Pattern.compile("(?<=[.?!])(\\s+|$)");
  private static final Pattern WORD_PATTERN =
      Pattern.compile(
          "\\b(?:Mr\\.|Mrs\\.|Ms\\.)\\b|"
              + "[\\p{L}\\p{N}]+(?:'[\\p{L}\\p{N}]+)*|"
              + "[\\p{L}\\p{N}]+|"
              + "[^\\s\\p{L}\\p{N}]+");

  public Processor(Path inputFile) throws IOException {
    this.reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
    this.charBuffer = new char[BUFFER_SIZE];
  }

  @Override
  public void close() throws IOException {
    if (reader != null) {
      reader.close();
    }
    buffer.setLength(0);
    System.out.println("Processor closed.");
  }

  public List<Sentence> readNextSentences() throws IOException {
    if (eofReached && buffer.isEmpty()) {
      return Collections.emptyList();
    }

    if (!eofReached) {
      int bytesRead = reader.read(charBuffer);
      if (bytesRead == -1) {
        eofReached = true;
      } else {
        buffer.append(charBuffer, 0, bytesRead);
      }
    }

    List<Sentence> sentencesFound = new ArrayList<>();
    String currentText = buffer.toString();
    int lastSentenceEndIndex = -1;

    Matcher matcher = SENTENCE_END_PATTERN.matcher(currentText);
    while (matcher.find()) {
      int sentenceEnd = matcher.start();
      int splitPoint = matcher.end();

      String sentenceText =
          currentText
              .substring((lastSentenceEndIndex == -1) ? 0 : lastSentenceEndIndex, sentenceEnd)
              .trim();

      if (!sentenceText.isEmpty()) {
        List<String> words = extractWords(sentenceText);
        words.sort(String::compareTo);
        if (!words.isEmpty()) {
          sentencesFound.add(new Sentence(words));
        }
      }
      lastSentenceEndIndex = splitPoint;
    }

    if (lastSentenceEndIndex != -1) {
      buffer.delete(0, lastSentenceEndIndex);
    } else if (eofReached && !buffer.isEmpty()) {
      String remainingText = buffer.toString().trim();
      if (!remainingText.isEmpty()) {
        List<String> words = extractWords(remainingText);
        words.sort(String::compareToIgnoreCase);
        if (!words.isEmpty()) {
          sentencesFound.add(new Sentence(words));
        }
      }
      buffer.setLength(0);
    }

    if (eofReached && buffer.isEmpty()) {
      close();
    }

    return sentencesFound;
  }

  private List<String> extractWords(String sentence) {
    List<String> words = new ArrayList<>();
    Matcher matcher = WORD_PATTERN.matcher(sentence);

    while (matcher.find()) {
      words.add(matcher.group());
    }

    return words.stream()
        .map(word -> word.replaceAll(PUNCTUATION_REGEX, ""))
        .filter(word -> !word.isEmpty())
        .collect(Collectors.toList());
  }
}
