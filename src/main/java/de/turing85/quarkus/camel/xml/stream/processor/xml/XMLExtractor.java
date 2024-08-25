package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.util.List;
import java.util.Set;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public interface XMLExtractor {
  default boolean handlesStartEvents() {
    return true;
  }

  void handleStartElement(StartElement startElement, List<String> path) throws Exception;

  default boolean recordsEvents() {
    return true;
  }

  void recordEvent(XMLEvent event, List<String> path) throws Exception;

  default boolean handlesEndEvents() {
    return true;
  }

  void handleEndElement(EndElement endElement, List<String> path) throws Exception;

  Set<String> getValues();
}
