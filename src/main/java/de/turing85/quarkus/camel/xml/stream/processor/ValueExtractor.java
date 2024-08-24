package de.turing85.quarkus.camel.xml.stream.processor;

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
public class ValueExtractor implements XMLExtractor {
  @Getter
  private final String name;
  private boolean isActive;
  private final Set<String> values;

  private StringWriter writer;

  public ValueExtractor(String name) throws IOException {
    this.name = name;
    this.isActive = false;
    this.values = new HashSet<>();
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
      isActive = true;
    }
  }

  @Override
  public void handleEventRecording(XMLEvent event, List<String> path) {
    if (isActive && event.isCharacters()) {
      writer.append(event.asCharacters().getData());
    }
  }

  @Override
  public void handleEndElement(EndElement endElement, List<String> path) throws IOException {
    if (endElement.getName().getLocalPart().equals(name())) {
      isActive = false;
      values.add(writer.toString());
      initializeWriter();
    }
  }

  public Set<String> getValues() {
    return Collections.unmodifiableSet(values);
  }
}