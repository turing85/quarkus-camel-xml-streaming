package de.turing85.quarkus.camel.xml.stream.processor;

import java.util.List;
import java.util.Set;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public interface XMLExtractor {
  void handleStartElement(StartElement startElement, List<String> path) throws Exception;

  void handleEventRecording(XMLEvent event, List<String> path) throws Exception;

  void handleEndElement(EndElement endElement, List<String> path) throws Exception;

  Set<String> getValues();
}
