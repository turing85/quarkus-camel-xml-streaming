package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

class TagBodyExtractor implements XMLExtractor {
  private final String name;
  private final String input;

  private final Map<List<String>, List<String>> values;

  private boolean recordStart;
  private int startIndex;
  private int elementCounter;

  TagBodyExtractor(String name, String input) {
    this.name = name;
    this.input = input;
    values = new TreeMap<>(Comparator.comparing(List::size));
    recordStart = false;
    startIndex = -1;
    elementCounter = 0;
  }

  @Override
  public void handleStartElement(StartElement startElement, List<String> path) {
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
  public void recordEvent(XMLEvent event, List<String> path) {
    // NOOP
  }

  @Override
  public void handleEndElement(EndElement endElement, List<String> path) {
    if (endElement.getName().getLocalPart().equals(name)) {
      --elementCounter;
      if (elementCounter == 0) {
        values.putIfAbsent(path, new ArrayList<>());
        String value = extractSubstringFromInput(this.startIndex,
            endElement.getLocation().getCharacterOffset());
        values.get(path).add(value);
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
  public Set<String> getValues() {
    // @formatter:off
    return values.entrySet().stream()
        .sorted(Comparator.comparingInt(entry -> entry.getKey().size()))
        .map(Map.Entry::getValue)
        .flatMap(List::stream)
        .collect(Collectors.toUnmodifiableSet());
    // @formatter:on
  }
}
