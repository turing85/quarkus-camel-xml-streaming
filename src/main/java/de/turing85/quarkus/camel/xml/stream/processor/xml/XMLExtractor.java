package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public interface XMLExtractor {
  default boolean handlesStartEvents() {
    return true;
  }

  void handleStartElement(StartElement startElement, int depth) throws XMLStreamException;

  default boolean recordsEvents() {
    return true;
  }

  void recordEvent(XMLEvent event, int depth) throws XMLStreamException;

  default boolean handlesEndEvents() {
    return true;
  }

  void handleEndElement(EndElement endElement, int depth) throws XMLStreamException;

  List<String> getValues();
}
