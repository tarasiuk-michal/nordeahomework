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

  static Stream<Arguments> readNextSentencesProvider() {
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
            "It's a test, don't fail.",
            List.of(new Sentence(Arrays.asList("a", "don't", "fail", "It's", "test")))),
        arguments("Just a phrase", List.of(new Sentence(Arrays.asList("a", "Just", "phrase")))),
        arguments("  .   ? !  ", List.of()));
  }

  @AfterEach
  void tearDown() {
    if (processor != null) {
      try {
        processor.close();
      } catch (IOException e) {
        System.err.println("Error closing processor in tearDown: " + e.getMessage());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("readNextSentencesProvider")
  void readNextSentences_extractionTest(String inputText, List<Sentence> expectedSentences)
      throws IOException {
    // Given
    testFile = createTestFile(inputText);
    processor = new Processor(testFile);

    List<Sentence> actualSentences = new ArrayList<>();
    List<Sentence> batch;

    // When
    while (!(batch = processor.readNextSentences()).isEmpty()) {
      actualSentences.addAll(batch);
    }

    // Then
    assertEquals(expectedSentences, actualSentences, "The list of parsed sentences did not match");
  }

  @Test
  void readNextSentences_emptyFile_returnsEmptyList() throws IOException {
    // Given
    testFile = createTestFile("");
    processor = new Processor(testFile);

    // When
    List<Sentence> sentences = processor.readNextSentences();
    List<Sentence> sentencesAfterEof = processor.readNextSentences();

    // Then
    assertTrue(sentences.isEmpty(), "Should return empty list for empty file");
    assertTrue(sentencesAfterEof.isEmpty(), "Should return empty list after EOF");
  }

  @Test
  void close_canBeCalledMultipleTimes() throws IOException {
    // Given
    testFile = createTestFile("Test.");
    processor = new Processor(testFile);

    // When
    processor.close();

    // Then
    assertDoesNotThrow(() -> processor.close());
  }

  private Path createTestFile(String content) throws IOException {
    Path file = tempDir.resolve("testInput.txt");

    Files.writeString(file, content, StandardCharsets.UTF_8);

    return file;
  }
}
