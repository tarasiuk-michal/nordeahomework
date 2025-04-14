package com.tarasiuk.nordeahomework.output;

import com.tarasiuk.nordeahomework.domain.Sentence;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes processed sentences to an XML file using the StAX API ({@link XMLStreamWriter}). Each
 * sentence and word is represented by specific XML elements. Implements {@link AutoCloseable} for
 * resource management.
 */
public class XmlWriter implements AutoCloseable {
  public static final String NEWLINE = System.lineSeparator();
  public static final String SENTENCE_TAG_NAME = "sentence";
  public static final String WORD_TAG_NAME = "word";
  private static final Logger logger = LoggerFactory.getLogger(XmlWriter.class);
  private final Writer writer;
  private final XMLStreamWriter xmlWriter;
  private boolean documentStarted = false;

  /**
   * Constructs an XmlWriter that will write to the specified output file path. Creates the
   * necessary writers and prepares for XML output.
   *
   * @param outputFile The path to the target XML file.
   * @throws IOException If an I/O error occurs creating the file or writers.
   * @throws XMLStreamException If an error occurs initializing the XML stream writer.
   */
  public XmlWriter(Path outputFile) throws IOException, XMLStreamException {
    logger.debug("Initializing XmlWriter for file: {}", outputFile);
    this.writer =
        new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(outputFile), StandardCharsets.UTF_8));
    this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(this.writer);
  }

  /**
   * Writes the XML declaration and the root element start tag ({@code <text>}) to the output file.
   * This must be called once before writing any sentences.
   *
   * @throws XMLStreamException If an error occurs writing the XML structure.
   */
  public void openDocument() throws XMLStreamException {
    logger.debug("Opening XML document.");
    if (!documentStarted) {
      xmlWriter.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
      xmlWriter.writeCharacters(NEWLINE);
      xmlWriter.writeStartElement("text");
      xmlWriter.writeCharacters(NEWLINE);
      documentStarted = true;
    }
  }

  /**
   * Writes a list of sentences to the XML file. Each sentence is enclosed in {@code <sentence>}
   * tags, and each word within a sentence is enclosed in {@code <word>} tags. Assumes {@link
   * #openDocument()} has already been called.
   *
   * @param sentences The list of {@link Sentence} objects to write. Can be null or empty (will be
   *     skipped).
   * @throws XMLStreamException If an error occurs writing the XML elements or characters.
   * @throws IllegalStateException If {@link #openDocument()} has not been called first.
   */
  public void writeSentences(List<Sentence> sentences) throws XMLStreamException {
    if (!documentStarted) {
      logger.error("Attempted to write sentences before opening document.");
      throw new IllegalStateException("Document must be opened before writing sentences.");
    }
    if (sentences == null || sentences.isEmpty()) {
      logger.debug("Skipping write for null or empty sentence list.");
      return;
    }
    logger.trace("Writing {} sentences to XML.", sentences.size());

    for (Sentence sentence : sentences) {
      xmlWriter.writeStartElement(SENTENCE_TAG_NAME);
      for (String word : sentence.words()) {
        xmlWriter.writeStartElement(WORD_TAG_NAME);
        xmlWriter.writeCharacters(word);
        xmlWriter.writeEndElement();
      }
      xmlWriter.writeEndElement();
      xmlWriter.writeCharacters(NEWLINE);
    }
    xmlWriter.flush();
  }

  /**
   * Closes the root XML element ({@code </text>}), finishes the XML document, and closes the
   * underlying writers. This method should be called when all sentences have been written,
   * typically via a try-with-resources statement.
   *
   * @throws XMLStreamException If an error occurs writing the final XML elements or closing the XML
   *     stream writer.
   */
  @Override
  public void close() throws XMLStreamException {
    logger.debug("Closing XmlWriter.");
    try {
      if (documentStarted) {
        xmlWriter.writeEndElement();
        xmlWriter.writeCharacters(NEWLINE);
        xmlWriter.writeEndDocument();
        logger.debug("XML document end written.");
      }
      if (xmlWriter != null) {
        xmlWriter.flush();
        xmlWriter.close();
        logger.debug("XMLStreamWriter closed.");
      }
    } finally {
      if (writer != null) {
        try {
          writer.close();
          logger.debug("Underlying writer closed.");
        } catch (IOException e) {
          logger.warn("Error closing underlying writer: {}", e.getMessage(), e);
        }
      }
    }
    logger.info("XmlWriter closed.");
  }
}
