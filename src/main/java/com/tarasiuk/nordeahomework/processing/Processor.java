package com.tarasiuk.nordeahomework.processing;

import com.tarasiuk.nordeahomework.domain.Sentence;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class Processor implements AutoCloseable {

  private static final int BUFFER_SIZE = 10240;
  private static final String OPENNLP_EN_TOKEN_MODEL_PATH =
      "/opennlp-en-ud-ewt-tokens-1.2-2.5.0.bin";
  private static final String OPENNLP_EN_SENTENCE_MODEL_PATH =
      "/opennlp-en-ud-ewt-sentence-1.2-2.5.0.bin";
  private static final Comparator<String> COMPARATOR = caseInsensitiveWithLowercaseFirst();
  private static final Set<String> ABBREVIATIONS_TO_PRESERVE = Set.of("Mr.", "Mrs.", "Ms.");
  private static final Pattern PUNCTUATION_PATTERN =
      Pattern.compile("^[.,!?:;()\"']+|[.,!?:;()\"']+$|^-$");

  private final BufferedReader reader;
  private final char[] charBuffer;
  private final StringBuilder buffer = new StringBuilder();
  private final SentenceDetectorME sdetector;
  private final TokenizerME tokenizer;
  private boolean eofReached = false;

  public Processor(Path inputFile) throws IOException {
    try (InputStream sentModelIn =
            Objects.requireNonNull(
                getClass().getResourceAsStream(OPENNLP_EN_SENTENCE_MODEL_PATH),
                "Sentence model not found on classpath at: " + OPENNLP_EN_SENTENCE_MODEL_PATH);
        InputStream tokenModelIn =
            Objects.requireNonNull(
                getClass().getResourceAsStream(OPENNLP_EN_TOKEN_MODEL_PATH),
                "Tokenizer model not found on classpath at: " + OPENNLP_EN_TOKEN_MODEL_PATH)) {

      this.sdetector = new SentenceDetectorME(new SentenceModel(sentModelIn));
      this.tokenizer = new TokenizerME(new TokenizerModel(tokenModelIn));
    } catch (IOException | NullPointerException e) {
      System.err.println("Error loading OpenNLP models from classpath: " + e.getMessage());
      throw new IOException("Failed to initialize OpenNLP Processor from classpath models", e);
    }

    this.reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
    this.charBuffer = new char[BUFFER_SIZE];
  }

  private static Comparator<String> caseInsensitiveWithLowercaseFirst() {
    return (a, b) -> {
      int cmp = a.compareToIgnoreCase(b);
      if (cmp != 0) return cmp;

      boolean aIsUpper = Character.isUpperCase(a.charAt(0));
      boolean bIsUpper = Character.isUpperCase(b.charAt(0));

      if (aIsUpper && !bIsUpper) return 1;
      if (!aIsUpper && bIsUpper) return -1;

      return a.compareTo(b);
    };
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
    Span[] sentenceSpans = sdetector.sentPosDetect(currentText);
    int lastProcessedEnd = 0;

    for (Span span : sentenceSpans) {
      String sentence = span.getCoveredText(currentText).toString().trim();
      if (!sentence.isEmpty()) {
        List<String> words = extractWords(sentence);
        if (!words.isEmpty()) {
          sentencesFound.add(new Sentence(words));
        }
      }
      lastProcessedEnd = span.getEnd();
    }

    if (lastProcessedEnd > 0) {
      buffer.delete(0, lastProcessedEnd);
    } else if (eofReached && !buffer.isEmpty()) {
      String remainingText = buffer.toString().trim();
      if (!remainingText.isEmpty()) {
        List<String> words = extractWords(remainingText);
        if (!words.isEmpty()) {
          sentencesFound.add(new Sentence(words));
        }
      }
      buffer.setLength(0);
    }

    return sentencesFound;
  }

  private List<String> extractWords(String sentence) {
    String[] tokens = tokenizer.tokenize(sentence);
    return Arrays.stream(tokens)
        .map(
            token -> {
              if (ABBREVIATIONS_TO_PRESERVE.contains(token)) {
                return token;
              } else {
                return PUNCTUATION_PATTERN.matcher(token).replaceAll("");
              }
            })
        .filter(token -> !token.isEmpty())
        .sorted(COMPARATOR)
        .collect(Collectors.toList());
  }
}
