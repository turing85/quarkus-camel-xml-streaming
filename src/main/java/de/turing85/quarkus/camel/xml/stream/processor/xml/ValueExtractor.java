package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
class ValueExtractor implements XMLExtractor {
  @Getter
  private final String name;
  private boolean recordValue;
  private final Set<String> values;

  private StringWriter writer;

  ValueExtractor(String name) throws IOException {
    this.name = name;
    recordValue = false;
    values = new HashSet<>();
    initializeWriter();
  }

  private void initializeWriter() throws IOException {
    if (writer != null) {
      writer.close();
    }
    writer = new StringWriter();
  }

  @Override
  public void handleStartElement(StartElement startElement, List<String> path) {
    if (startElement.getName().getLocalPart().equals(name())) {
      recordValue = true;
    }
  }

  @Override
  public void recordEvent(XMLEvent event, List<String> path) {
    if (recordValue && event.isCharacters()) {
      writer.append(event.asCharacters().getData());
    }
  }

  @Override
  public void handleEndElement(EndElement endElement, List<String> path) throws IOException {
    if (endElement.getName().getLocalPart().equals(name())) {
      recordValue = false;
      values.add(writer.toString());
      initializeWriter();
    }
  }

  @Override
  public Set<String> getValues() {
    return Collections.unmodifiableSet(values);
  }
}
