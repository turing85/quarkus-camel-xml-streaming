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

  private boolean lastWasStart;
  private boolean recordStart;
  private final Map<List<String>, List<String>> values;
  private int startIndex;
  private int elementCounter;

  TagBodyExtractor(String name, String input) {
    this.name = name;
    this.input = input;
    values = new TreeMap<>(Comparator.comparing(List::size));
    lastWasStart = false;
    recordStart = false;
    startIndex = -1;
    elementCounter = 0;
  }

  @Override
  public void handleStartElement(StartElement startElement, List<String> path) {
    String elementName = startElement.getName().getLocalPart();
    if (elementName.equals(name)) {
      elementCounter++;
      lastWasStart = true;
    } else if (recordStart && startIndex == -1) {
      startIndex = startElement.getLocation().getCharacterOffset();
      recordStart = false;
    }
  }

  @Override
  public void handleEventRecording(XMLEvent event, List<String> path) {
    if (lastWasStart && startIndex == -1 && !recordStart) {
      recordStart = true;
      lastWasStart = false;
    }
  }

  @Override
  public void handleEndElement(EndElement endElement, List<String> path) {
    String elementName = endElement.getName().getLocalPart();
    if (elementName.equals(name)) {
      --elementCounter;
      if (elementCounter == 0) {
        values.putIfAbsent(path, new ArrayList<>());
        values.get(path)
            .add(input.substring(startIndex, endElement.getLocation().getCharacterOffset()).trim());
        startIndex = -1;
      }
    }
  }

  @Override
  public Set<String> getValues() {
    return values.entrySet().stream().sorted().map(Map.Entry::getValue).flatMap(List::stream)
        .collect(Collectors.toUnmodifiableSet());
  }
}
