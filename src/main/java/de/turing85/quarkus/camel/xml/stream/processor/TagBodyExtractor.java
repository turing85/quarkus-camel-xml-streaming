package de.turing85.quarkus.camel.xml.stream.processor;

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

public class TagBodyExtractor implements XMLExtractor {
  private final String name;
  private final String input;

  private boolean isActive;
  private String currentlyActiveTag;
  private final Map<List<String>, List<String>> values;
  private int startIndex;
  private int endIndex;

  public TagBodyExtractor(String name, String input) {
    this.name = name;
    this.input = input;
    values = new TreeMap<>(Comparator.comparing(List::size));
    isActive = false;
    currentlyActiveTag = null;
    startIndex = -1;
  }

  @Override
  public void handleStartElement(StartElement startElement, List<String> path) {
    String elementName = startElement.getName().getLocalPart();
    if (elementName.equals(name)) {
      startIndex = startElement.getLocation().getCharacterOffset();
      isActive = true;
    } else if (isActive && currentlyActiveTag == null) {
      currentlyActiveTag = elementName;
    }
  }

  @Override
  public void handleEventRecording(XMLEvent event, List<String> path) {
    // NOOP
  }

  @Override
  public void handleEndElement(EndElement endElement, List<String> path) {
    String elementName = endElement.getName().getLocalPart();
    if (elementName.equals(currentlyActiveTag)) {
      currentlyActiveTag = null;
      endIndex = endElement.getLocation().getCharacterOffset();
    } else if (isActive && elementName.equals(name)) {
      isActive = false;
      values.putIfAbsent(path, new ArrayList<>());
      values.get(path).add(input.substring(startIndex, endIndex).trim());
      startIndex = -1;
    }
  }

  public Set<String> getValues() {
    return values.entrySet().stream().sorted().map(Map.Entry::getValue).flatMap(List::stream)
        .collect(Collectors.toUnmodifiableSet());
  }
}
