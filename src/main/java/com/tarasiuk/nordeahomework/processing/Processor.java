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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes an input text file, detecting sentences and tokenizing words using Apache OpenNLP.
 * Reads the input file in chunks, extracts sentences, cleans and sorts the words within each
 * sentence, and provides them in batches. Implements {@link AutoCloseable} for resource management.
 */
public class Processor implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(Processor.class);

  private static final String OPENNLP_EN_TOKEN_MODEL_PATH =
      "/opennlp-en-ud-ewt-tokens-1.2-2.5.0.bin";
  private static final String OPENNLP_EN_SENTENCE_MODEL_PATH =
      "/opennlp-en-ud-ewt-sentence-1.2-2.5.0.bin";

  /** Size of the buffer for reading file chunks */
  private static final int BUFFER_SIZE = 10240;

  /**
   * Comparator for sorting words within a sentence. Primary sort: Case-insensitive alphabetical
   * order. Secondary sort (tie-breaker): Lowercase words before uppercase words if they are
   * otherwise identical ignoring case. Tertiary sort: Case-sensitive order if the first character's
   * case is the same.
   */
  private static final Comparator<String> COMPARATOR = caseInsensitiveWithLowercaseFirst();

  /** Set of abbreviations for which the trailing period should be preserved during tokenization. */
  private static final Set<String> ABBREVIATIONS_TO_PRESERVE = Set.of("Mr.", "Mrs.", "Ms.");

  /**
   * Pattern to remove leading/trailing punctuation from tokens, or tokens consisting solely of a
   * hyphen.
   */
  private static final Pattern PUNCTUATION_PATTERN =
      Pattern.compile("^[.,!?:;()\"']+|[.,!?:;()\"']+$|^-$");

  private final BufferedReader reader;
  private final char[] charBuffer;
  private final StringBuilder buffer = new StringBuilder();
  private final SentenceDetectorME sdetector;
  private final TokenizerME tokenizer;
  private boolean eofReached = false;

  /**
   * Constructs a Processor to read and process the given input file. Loads OpenNLP sentence
   * detection and tokenizer models from the classpath.
   *
   * @param inputFile The path to the input text file.
   * @throws IOException If an error occurs reading the input file or loading the OpenNLP models.
   */
  public Processor(Path inputFile) throws IOException {
    logger.debug("Initializing Processor for file: {}", inputFile);
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
      logger.debug("OpenNLP models loaded successfully.");

    } catch (IOException | NullPointerException e) {
      logger.error("Error loading OpenNLP models from classpath: {}", e.getMessage(), e);
      throw new IOException("Failed to initialize OpenNLP Processor from classpath models", e);
    }

    this.reader = Files.newBufferedReader(inputFile, StandardCharsets.UTF_8);
    this.charBuffer = new char[BUFFER_SIZE];
  }

  /**
   * Creates a comparator for sorting words. Primary sort: Case-insensitive alphabetical order.
   * Secondary sort (tie-breaker): Lowercase words before uppercase words if they are otherwise
   * identical ignoring case. Tertiary sort: Case-sensitive order if the first character's case is
   * the same. (Standard library natural order puts uppercase first in case ties).
   *
   * @return The configured Comparator for sorting words.
   */
  private static Comparator<String> caseInsensitiveWithLowercaseFirst() {
    return (a, b) -> {
      int cmp = a.compareToIgnoreCase(b);
      if (cmp != 0) {
        return cmp;
      }

      boolean aIsUpper = Character.isUpperCase(a.charAt(0));
      boolean bIsUpper = Character.isUpperCase(b.charAt(0));

      if (aIsUpper && !bIsUpper) {
        return 1;
      }
      if (!aIsUpper && bIsUpper) {
        return -1;
      }

      return a.compareTo(b);
    };
  }

  /**
   * Closes the underlying file reader and clears the internal buffer. This method should be called
   * when processing is complete, typically via a try-with-resources statement.
   *
   * @throws IOException If an error occurs closing the reader.
   */
  @Override
  public void close() throws IOException {
    logger.debug("Closing Processor.");
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException e) {
        logger.warn("Error closing reader: {}", e.getMessage(), e);
      }
    }
    buffer.setLength(0);
    logger.info("Processor closed.");
  }

  /**
   * Reads the next chunk of the input file (if necessary), detects complete sentences within the
   * available text, processes them (tokenizes, cleans, sorts words), and returns them as a list.
   * Returns an empty list when the end of the file is reached and all buffered text has been
   * processed.
   *
   * @return A list of {@link Sentence} objects found in the current processing batch, or an empty
   *     list if no complete sentences are found or EOF is reached.
   * @throws IOException If an error occurs reading from the input file.
   */
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

  /**
   * Extracts, cleans, and sorts words from a given sentence string. Uses the OpenNLP tokenizer,
   * applies punctuation removal (preserving specific abbreviations), filters empty tokens, and
   * sorts the results using the defined {@code COMPARATOR}.
   *
   * @param sentence The sentence string to process.
   * @return A sorted list of cleaned words extracted from the sentence.
   */
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
