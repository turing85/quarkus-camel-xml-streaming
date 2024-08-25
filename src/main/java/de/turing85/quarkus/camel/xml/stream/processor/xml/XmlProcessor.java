package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import jakarta.inject.Singleton;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import lombok.AllArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import static java.util.function.Predicate.not;

@Singleton
@AllArgsConstructor
public class XmlProcessor implements Processor {
  public static final String PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT =
      "additionalValuesToExtract";
  public static final String PROPERTY_NAME_ADDITIONAL_VALUES = "additionalValues";
  public static final String PROPERTY_NAME_REQUEST = "property-request";
  public static final String PROPERTY_NAME_RESPONSE = "property-response";

  private static final InputFactoryImpl INPUT_FACTORY = new InputFactoryImpl();

  @Override
  public void process(Exchange exchange) throws XMLStreamException {
    String input = exchange.getIn().getBody(String.class);
    @SuppressWarnings("unchecked")
    Result result = parse(input, exchange.getProperty(PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT,
        Collections.emptyList(), Collection.class));
    exchange.setProperty(PROPERTY_NAME_REQUEST, result.requests().getFirst());
    exchange.setProperty(PROPERTY_NAME_RESPONSE, result.responses().getFirst());
    exchange.setProperty(PROPERTY_NAME_ADDITIONAL_VALUES, result.additionalValues().entrySet()
        .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFirst())));
  }

  static Result parse(String input, Collection<String> additionalValuesToExtract)
      throws XMLStreamException {
    Map<String, XMLExtractor> extractors = constructExtractors(input, additionalValuesToExtract);
    ExtractorGroups extractorGroups = groupExtractors(extractors);
    XMLEventReader reader = INPUT_FACTORY.createXMLEventReader(new StringReader(input));
    int depth = 0;
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        StartElement startElement = event.asStartElement();
        ++depth;
        for (XMLExtractor extractor : extractorGroups.startElementHandlers()) {
          extractor.handleStartElement(startElement, depth);
        }
      }
      for (XMLExtractor extractor : extractorGroups.eventRecorders()) {
        extractor.recordEvent(event, depth);
      }
      if (event.isEndElement()) {
        EndElement endElement = event.asEndElement();
        for (XMLExtractor extractor : extractorGroups.endElementHandlers()) {
          extractor.handleEndElement(endElement, depth);
        }
        --depth;
      }
    }
    // @formatter:off
    return new Result(
        extractors.get("INTERNAL-request").getValues(),
        extractors.get("INTERNAL-response").getValues(),
        extractors.entrySet().stream()
            .filter(not(entry -> entry.getKey().startsWith("INTERNAL")))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> List.copyOf(entry.getValue().getValues()))));
    // @formatter:on
  }

  private static Map<String, XMLExtractor> constructExtractors(String input,
      Collection<String> additionalValuesToExtract) {
    Map<String, XMLExtractor> extractors = HashMap.newHashMap(additionalValuesToExtract.size() + 2);

    TagBodyExtractor requestExtractor = new TagBodyExtractor("request", input);
    extractors.put("INTERNAL-request", requestExtractor);
    TagBodyExtractor responseExtractor = new TagBodyExtractor("response", input);
    extractors.put("INTERNAL-response", responseExtractor);
    for (String additionalValue : additionalValuesToExtract) {
      extractors.put(additionalValue, new ValueExtractor(additionalValue));
    }

    return extractors;
  }

  private static ExtractorGroups groupExtractors(Map<String, XMLExtractor> extractors) {
    List<XMLExtractor> startElementHandlers = new ArrayList<>();
    List<XMLExtractor> eventRecorders = new ArrayList<>();
    List<XMLExtractor> endElementHandlers = new ArrayList<>();
    for (XMLExtractor extractor : extractors.values()) {
      if (extractor.handlesStartEvents()) {
        startElementHandlers.add(extractor);
      }
      if (extractor.recordsEvents()) {
        eventRecorders.add(extractor);
      }
      if (extractor.handlesEndEvents()) {
        endElementHandlers.add(extractor);
      }
    }
    return new ExtractorGroups(startElementHandlers, eventRecorders, endElementHandlers);
  }

  private record ExtractorGroups(List<XMLExtractor> startElementHandlers,
      List<XMLExtractor> eventRecorders, List<XMLExtractor> endElementHandlers) {}

  record Result(List<String> requests, List<String> responses,
      Map<String, List<String>> additionalValues) {}
}
