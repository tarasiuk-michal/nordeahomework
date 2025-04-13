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

public class XmlWriter implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(XmlWriter.class);
  public static final String NEWLINE = System.lineSeparator();
  public static final String SENTENCE_TAG_NAME = "sentence";
  public static final String WORD_TAG_NAME = "word";

  private final Writer writer;
  private final XMLStreamWriter xmlWriter;
  private boolean documentStarted = false;

  public XmlWriter(Path outputFile) throws IOException, XMLStreamException {
    logger.debug("Initializing XmlWriter for file: {}", outputFile);
    this.writer = new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(outputFile), StandardCharsets.UTF_8));
    this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(this.writer);
  }

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

  @Override
  public void close() throws IOException, XMLStreamException {
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
