import static org.junit.jupiter.api.Assertions.*;
import static org.xmlunit.assertj3.XmlAssert.assertThat; // XMLUnit AssertJ

import com.tarasiuk.nordeahomework.Main;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

class ExampleFilesTest {
  public static final String INPUT_FILE = "small.in";
  public static final String CSV_OUTPUT_FILE = "small.csv";
  public static final String XML_OUTPUT_FILE = "small.xml";
  private static final Path testInputPath =
      Paths.get("src", "test", "resources", "in").resolve(INPUT_FILE);
  private static final Path expectedCsvPath =
      Paths.get("src", "test", "resources", "out").resolve(CSV_OUTPUT_FILE);
  private static final Path expectedXmlPath =
      Paths.get("src", "test", "resources", "out").resolve(XML_OUTPUT_FILE);
  @TempDir Path actualOutputDir;

  @BeforeAll
  static void checkBaselineFiles() {
    assertTrue(Files.exists(testInputPath), "Test input file missing: " + testInputPath);
    assertTrue(Files.exists(expectedCsvPath), "Expected CSV file missing: " + expectedCsvPath);
    assertTrue(Files.exists(expectedXmlPath), "Expected XML file missing: " + expectedXmlPath);
  }

  @Test
  void processSmallFile_generatesCorrectOutput() throws IOException, XMLStreamException {
    // Given
    Path actualCsvPath = actualOutputDir.resolve(CSV_OUTPUT_FILE);
    Path actualXmlPath = actualOutputDir.resolve(XML_OUTPUT_FILE);

    // When
    Main.process(testInputPath, actualXmlPath, actualCsvPath);

    // Then
    assertTrue(Files.exists(actualCsvPath), "Actual CSV output file was not created.");
    String expectedCsvContent =
        Files.readString(expectedCsvPath, StandardCharsets.UTF_8).replace("\r\n", "\n");
    String actualCsvContent =
        Files.readString(actualCsvPath, StandardCharsets.UTF_8).replace("\r\n", "\n");
    assertEquals(expectedCsvContent, actualCsvContent, "CSV file content does not match expected.");

    assertTrue(Files.exists(actualXmlPath), "Actual XML output file was not created.");
    assertThat(actualXmlPath)
        .and(expectedXmlPath)
        .ignoreWhitespace()
        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
        .areIdentical();

    System.out.println("Integration test passed for: " + INPUT_FILE);
    System.out.println("Actual CSV output: " + actualCsvPath);
    System.out.println("Actual XML output: " + actualXmlPath);
  }
}
