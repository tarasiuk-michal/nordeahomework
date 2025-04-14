import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.xmlunit.assertj3.XmlAssert.assertThat;

import com.tarasiuk.nordeahomework.domain.Sentence;
import com.tarasiuk.nordeahomework.output.XmlWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class XmlWriterTest {

  public static final String XML_DECL_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
  @TempDir Path tempDir;

  static Stream<Arguments> xmlWritingProvider() {
    return Stream.of(
        arguments(
            "single_sentence.xml",
            List.of(new Sentence(Arrays.asList("Hello", "world"))),
            """
                                        <text>
                                        <sentence><word>Hello</word><word>world</word></sentence>
                                        </text>
                                        """),
        arguments(
            "multi_sentence.xml",
            List.of(new Sentence(Arrays.asList("First", "one")), new Sentence(List.of("Second"))),
            """
                                        <text>
                                        <sentence><word>First</word><word>one</word></sentence>
                                        <sentence><word>Second</word></sentence>
                                        </text>
                                        """),
        arguments(
            "escaped_sentence.xml",
            List.of(
                new Sentence(
                    Arrays.asList("LessThan<", "GreaterThan>", "Ampersand&", "Apos'", "Quote\""))),
            """
                                        <text>
                                        <sentence><word>LessThan&lt;</word><word>GreaterThan&gt;</word><word>Ampersand&amp;</word><word>Apos'</word><word>Quote&quot;</word></sentence>
                                        </text>
                                        """),
        arguments(
            "empty_list.xml",
            Collections.emptyList(),
            """
                                        <text>
                                        </text>
                                        """));
  }

  @ParameterizedTest(name = "[{index}] Writing {0}")
  @MethodSource("xmlWritingProvider")
  void writeSentences_producesCorrectXml(
      String outputFileName, List<Sentence> sentences, String expectedXmlContent)
      throws IOException, XMLStreamException {
    // Given
    Path outputFile = tempDir.resolve(outputFileName);

    // When
    try (XmlWriter writer = new XmlWriter(outputFile)) {
      writer.openDocument();
      writer.writeSentences(sentences);
    }
    assertTrue(Files.exists(outputFile), "Output file should exist");
    String actual = Files.readString(outputFile, StandardCharsets.UTF_8);

    String expected = XML_DECL_HEADER + expectedXmlContent;

    // Then
    assertThat(actual).and(expected).ignoreWhitespace().areIdentical();
  }

  @Test
  void writeSentences_handlesNullListGracefully() throws IOException, XMLStreamException {
    // Given
    Path outputFile = tempDir.resolve("null_list.xml");

    // When
    try (XmlWriter writer = new XmlWriter(outputFile)) {
      writer.openDocument();
      writer.writeSentences(null); // Pass null list
    }
    assertTrue(Files.exists(outputFile), "Output file should exist");
    String actual = Files.readString(outputFile, StandardCharsets.UTF_8);

    String expected = XML_DECL_HEADER + "<text>\n</text>\n";

    // Then
    assertThat(actual).and(expected).ignoreWhitespace().areIdentical();
  }

  @Test
  void writeSentences_withoutOpenDocument_throwsException() throws IOException, XMLStreamException {
    // Given
    Path outputFile = tempDir.resolve("no_open.xml");
    // When
    try (XmlWriter writer = new XmlWriter(outputFile)) {
      // Then
      assertThrows(
          IllegalStateException.class,
          () -> writer.writeSentences(List.of(new Sentence(List.of("test")))),
          "Should throw IllegalStateException if document is not opened");
    }
  }
}
