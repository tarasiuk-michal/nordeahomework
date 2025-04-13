import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tarasiuk.nordeahomework.domain.Sentence;
import com.tarasiuk.nordeahomework.processing.Processor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProcessorTest {

  @TempDir Path tempDir;
  private Path testFile;
  private Processor processor;

  private Path createTestFile(String content) throws IOException {
    Path file = tempDir.resolve("testInput.txt");
    Files.writeString(file, content, StandardCharsets.UTF_8);
    return file;
  }

  @AfterEach
  void tearDown(){
    if (processor != null) {
      try {
        processor.close();
      } catch (IOException e) {
        System.err.println("Error closing processor in tearDown: " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("readNextSentencesExtractionProvider")
  void readNextSentences_extractionTest(String inputText, List<Sentence> expectedSentences)
      throws IOException {
    testFile = createTestFile(inputText);
    processor = new Processor(testFile);

    List<Sentence> actualSentences = new ArrayList<>();
    List<Sentence> batch;

    while (!(batch = processor.readNextSentences()).isEmpty()) {
      actualSentences.addAll(batch);
    }

    assertEquals(expectedSentences, actualSentences, "The list of parsed sentences did not match");
  }

  @Test
  void readNextSentences_emptyFile_shouldReturnEmptyList() throws IOException {
    testFile = createTestFile("");
    processor = new Processor(testFile);
    List<Sentence> sentences = processor.readNextSentences();
    assertTrue(sentences.isEmpty(), "Should return empty list for empty file");
    // Call again after EOF
    List<Sentence> sentencesAfterEof = processor.readNextSentences();
    assertTrue(sentencesAfterEof.isEmpty(), "Should return empty list after EOF");
  }

  @Test
  void readNextSentences_textEndingWithoutPunctuation_multiRead() throws IOException {
    // This test specifically checks the multi-read behavior when EOF logic kicks in
    testFile =
        createTestFile(
            "This is the first sentence. This is the second one");
    processor = new Processor(testFile);

    List<Sentence> firstBatch = processor.readNextSentences();
    assertEquals(1, firstBatch.size());
    Sentence expected1 = new Sentence(Arrays.asList("first", "is", "sentence", "the", "This"));
    assertEquals(expected1, firstBatch.get(0));

    List<Sentence> secondBatch = processor.readNextSentences();
    assertEquals(1, secondBatch.size());

    Sentence expected2 = new Sentence(Arrays.asList("is", "one", "second", "the", "This"));
    assertEquals(
        expected2,
        secondBatch.get(0),
        "Remaining text at EOF should be processed (case-insensitive sort)");

    List<Sentence> thirdBatch = processor.readNextSentences();
    assertTrue(thirdBatch.isEmpty());
  }

  @Test
  void readNextSentences_afterExplicitClose_returnsEmpty() throws IOException {
    testFile = createTestFile("Test.");
    processor = new Processor(testFile);
    processor.close();

    List<Sentence> sentences = processor.readNextSentences();
    assertTrue(sentences.isEmpty(), "Reading after close should return empty list");
  }

  @Test
  void close_canBeCalledMultipleTimes() throws IOException {
    testFile = createTestFile("Test.");
    processor = new Processor(testFile);
    processor.close();

    assertDoesNotThrow(() -> processor.close());
  }

  static Stream<Arguments> readNextSentencesExtractionProvider() {
    return Stream.of(
            arguments(
                    "This is a test.", List.of(new Sentence(Arrays.asList("a", "is", "test", "This")))),
            arguments(
                    "First sentence? Second sentence! Third one ends here.",
                    List.of(
                            new Sentence(Arrays.asList("First", "sentence")),
                            new Sentence(Arrays.asList("Second", "sentence")),
                            new Sentence(Arrays.asList("ends", "here", "one", "Third")))),
            arguments(
                    "Words, like 'hyphen-ated' (or not), are tricky.",
                    List.of(
                            new Sentence(
                                    Arrays.asList("are", "hyphen-ated", "like", "not", "or", "tricky", "Words")))),
            arguments(
                    "Mr. Smith went to Washington. Mrs. Jones stayed home.",
                    List.of(
                            new Sentence(Arrays.asList("Mr", "Smith", "to", "Washington", "went")),
                            new Sentence(Arrays.asList("home", "Jones", "Mrs", "stayed")))),
            arguments(
                    "It's a test, don't fail.",
                    List.of(new Sentence(Arrays.asList("a", "don't", "fail", "It's", "test")))),
            arguments(
                    "Just a phrase",
                    List.of(
                            new Sentence(
                                    Arrays.asList(
                                            "a", "Just", "phrase"))
                    )),
            arguments(
                    "  .   ? !  ", List.of()));
  }
}
