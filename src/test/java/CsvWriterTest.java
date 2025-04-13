import static com.tarasiuk.nordeahomework.output.CsvWriter.DELIMITER;
import static com.tarasiuk.nordeahomework.output.CsvWriter.NEWLINE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.tarasiuk.nordeahomework.domain.Sentence;
import com.tarasiuk.nordeahomework.output.CsvWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CsvWriterTest {
  @TempDir Path tempDir;

  @ParameterizedTest(name = "[{index}] Writing {0}")
  @MethodSource("csvWritingProvider")
  void writeSentences_producesCorrectCsv(
      String outputFileName, List<Sentence> sentences, String expected) throws IOException {
    // Given
    Path outputFile = tempDir.resolve(outputFileName);

    // When
    try (CsvWriter writer = new CsvWriter(outputFile)) {
      writer.writeSentences(sentences);
    }

    // Then
    assertTrue(Files.exists(outputFile), "Output file should exist");
    String actual = Files.readString(outputFile, StandardCharsets.UTF_8);
    assertEquals(expected, actual, "CSV content mismatch for " + outputFileName);
  }

  @Test
  void writeSentences_handlesNullListGracefully() throws IOException {
    // Given
    Path outputFile = tempDir.resolve("null_list.csv");

    // When
    try (CsvWriter writer = new CsvWriter(outputFile)) {
      writer.writeSentences(null);
    }

    // Then
    assertTrue(Files.exists(outputFile), "Output file should exist");
    String actual = Files.readString(outputFile, StandardCharsets.UTF_8);
    assertEquals("", actual, "CSV content for null list mismatch");
  }

  static Stream<Arguments> csvWritingProvider() {
    return Stream.of(
            arguments( // Single sentence
                    "single_sentence.csv",
                    List.of(new Sentence(Arrays.asList("Hello", "world"))),
                    new StringJoiner(NEWLINE)
                            .add(new StringJoiner(DELIMITER).add("").add("Word 1").add("Word 2").toString())
                            .add(
                                    new StringJoiner(DELIMITER)
                                            .add("Sentence 1")
                                            .add("Hello")
                                            .add("world")
                                            .toString())
                            .add("")
                            .toString()),
            arguments( // Multiple sentences, different lengths
                    "multi_sentence.csv",
                    List.of(
                            new Sentence(List.of("Short")),
                            new Sentence(Arrays.asList("This", "is", "longer")),
                            new Sentence(Arrays.asList("Medium", "one"))),
                    new StringJoiner(NEWLINE)
                            .add(
                                    new StringJoiner(DELIMITER)
                                            .add("")
                                            .add("Word 1")
                                            .add("Word 2")
                                            .add("Word 3")
                                            .toString())
                            .add(new StringJoiner(DELIMITER).add("Sentence 1").add("Short").toString())
                            .add(
                                    new StringJoiner(DELIMITER)
                                            .add("Sentence 2")
                                            .add("This")
                                            .add("is")
                                            .add("longer")
                                            .toString())
                            .add(
                                    new StringJoiner(DELIMITER)
                                            .add("Sentence 3")
                                            .add("Medium")
                                            .add("one")
                                            .toString())
                            .add("")
                            .toString()),
            arguments( // Sentence with characters needing escaping
                    "escaped_sentence.csv",
                    List.of(
                            new Sentence(Arrays.asList("Comma,here", "Quote\"there", "Both,\"&", "Normal"))),
                    new StringJoiner(NEWLINE)
                            .add(
                                    new StringJoiner(DELIMITER)
                                            .add("")
                                            .add("Word 1")
                                            .add("Word 2")
                                            .add("Word 3")
                                            .add("Word 4")
                                            .toString())
                            .add(
                                    new StringJoiner(DELIMITER)
                                            .add("Sentence 1")
                                            .add("\"Comma,here\"")
                                            .add("\"Quote\"\"there\"")
                                            .add("\"Both,\"\"&\"")
                                            .add("Normal")
                                            .toString())
                            .add("")
                            .toString()),
            arguments("empty_list.csv", Collections.emptyList(), ""));
  }
}
