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

  @ParameterizedTest(name = "[{index}] Writing {0}")
  @MethodSource("xmlWritingProvider")
  void writeSentences_producesCorrectXml(
      String outputFileName, List<Sentence> sentences, String expectedXmlContent)
      throws IOException, XMLStreamException {
    Path outputFile = tempDir.resolve(outputFileName);

    try (XmlWriter writer = new XmlWriter(outputFile)) {
      writer.openDocument();
      writer.writeSentences(sentences);
    }

    assertTrue(Files.exists(outputFile), "Output file should exist");
    String actualXmlContent = Files.readString(outputFile, StandardCharsets.UTF_8);

    String expectedWithDecl = XML_DECL_HEADER + expectedXmlContent;

    assertThat(actualXmlContent).and(expectedWithDecl).areIdentical();
  }

  @Test
  void writeSentences_handlesNullListGracefully() throws IOException, XMLStreamException {
    Path outputFile = tempDir.resolve("null_list.xml");
    String expectedXmlContent = XML_DECL_HEADER + "<text>\n</text>\n";

    try (XmlWriter writer = new XmlWriter(outputFile)) {
      writer.openDocument();
      writer.writeSentences(null); // Pass null list
    }

    assertTrue(Files.exists(outputFile), "Output file should exist");
    String actualXmlContent = Files.readString(outputFile, StandardCharsets.UTF_8);
    assertThat(actualXmlContent).and(expectedXmlContent).ignoreWhitespace().areIdentical();
  }

  @Test
  void writeSentences_withoutOpenDocument_throwsException() throws IOException, XMLStreamException {
    Path outputFile = tempDir.resolve("no_open.xml");
    try (XmlWriter writer = new XmlWriter(outputFile)) {
      assertThrows(
          IllegalStateException.class,
          () -> writer.writeSentences(List.of(new Sentence(List.of("test")))),
          "Should throw IllegalStateException if document is not opened");
    }
  }

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
            List.of(
                new Sentence(Arrays.asList("First", "one")), new Sentence(Arrays.asList("Second"))),
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
}
