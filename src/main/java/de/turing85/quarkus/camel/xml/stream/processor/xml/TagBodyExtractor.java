package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

class TagBodyExtractor implements XMLExtractor {
  private final String name;
  private final String input;

  private final SortedMap<Integer, List<String>> values;

  private boolean recordStart;
  private int startIndex;
  private int elementCounter;

  TagBodyExtractor(String name, String input) {
    this.name = name;
    this.input = input;
    values = new TreeMap<>();
    recordStart = false;
    startIndex = -1;
    elementCounter = 0;
  }

  @Override
  public void handleStartElement(StartElement startElement, int depth) {
    if (startElement.getName().getLocalPart().equals(name)) {
      elementCounter++;
      recordStart = true;
    } else {
      if (recordStart && startIndex == -1) {
        startIndex = startElement.getLocation().getCharacterOffset();
      }
      recordStart = false;
    }
  }

  @Override
  public boolean recordsEvents() {
    return false;
  }

  @Override
  public void recordEvent(XMLEvent event, int depth) {
    // NOOP
  }

  @Override
  public void handleEndElement(EndElement endElement, int depth) {
    if (endElement.getName().getLocalPart().equals(name)) {
      --elementCounter;
      if (elementCounter == 0) {
        values.putIfAbsent(depth, new ArrayList<>());
        String value = extractSubstringFromInput(this.startIndex,
            endElement.getLocation().getCharacterOffset());
        values.get(depth).add(value);
        startIndex = -1;
      }
    }
  }

  private String extractSubstringFromInput(int startIndex, int endIndex) {
    if (startIndex > -1) {
      return input.substring(startIndex, endIndex).trim();
    } else {
      return "";
    }
  }

  @Override
  public List<String> getValues() {
    // @formatter:off
    return List.copyOf(values.values().stream()
        .flatMap(List::stream)
        .toList());
    // @formatter:on
  }
}
