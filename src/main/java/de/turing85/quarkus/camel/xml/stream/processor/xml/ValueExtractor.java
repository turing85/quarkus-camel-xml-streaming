package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
class ValueExtractor implements XMLExtractor {
  @Getter
  private final String name;

  private final Set<String> values;
  private StringBuilder builder;
  private boolean recordValue;


  ValueExtractor(String name) {
    this.name = name;
    recordValue = false;
    values = new TreeSet<>();
    builder = new StringBuilder();
  }

  @Override
  public void handleStartElement(StartElement startElement, int depth) {
    if (startElement.getName().getLocalPart().equals(name())) {
      recordValue = true;
    }
  }

  @Override
  public void recordEvent(XMLEvent event, int depth) {
    if (recordValue && event.isCharacters()) {
      builder.append(event.asCharacters().getData());
    }
  }

  @Override
  public void handleEndElement(EndElement endElement, int depth) {
    if (endElement.getName().getLocalPart().equals(name())) {
      recordValue = false;
      values.add(builder.toString());
      builder = new StringBuilder();
    }
  }

  @Override
  public List<String> getValues() {
    return List.copyOf(values);
  }
}
