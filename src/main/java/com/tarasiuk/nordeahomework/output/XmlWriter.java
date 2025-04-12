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

public class XmlWriter implements AutoCloseable {

  private final Writer writer;
  private final XMLStreamWriter xmlWriter;
  private boolean documentStarted = false;

  public XmlWriter(Path outputFile) throws IOException, XMLStreamException {
    this.writer =
        new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(outputFile), StandardCharsets.UTF_8));
    this.xmlWriter =
        XMLOutputFactory.newInstance()
            .createXMLStreamWriter(Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8));
  }

  public void openDocument() throws XMLStreamException {
    if (!documentStarted) {
      xmlWriter.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
      xmlWriter.writeCharacters("\n");
      xmlWriter.writeStartElement("text");
      xmlWriter.writeCharacters("\n");
      documentStarted = true;
    }
  }

  public void writeSentences(List<Sentence> sentences) throws XMLStreamException {
    if (!documentStarted) {
      throw new IllegalStateException("Document must be opened before writing sentences.");
    }
    if (sentences == null || sentences.isEmpty()) {
      return;
    }

    for (Sentence sentence : sentences) {
      xmlWriter.writeStartElement("sentence");
      for (String word : sentence.words()) {
        xmlWriter.writeStartElement("word");
        xmlWriter.writeCharacters(word);
        xmlWriter.writeEndElement();
      }
      xmlWriter.writeEndElement();
      xmlWriter.writeCharacters("\n");
    }
    xmlWriter.flush();
  }

  @Override
  public void close() throws IOException, XMLStreamException {
    try {
      if (documentStarted) {
        xmlWriter.writeEndElement(); // </text>
        xmlWriter.writeCharacters("\n");
        xmlWriter.writeEndDocument();
      }
      if (xmlWriter != null) {
        xmlWriter.flush();
        xmlWriter.close();
      }
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
    System.out.println("XmlWriter closed.");
  }
}
