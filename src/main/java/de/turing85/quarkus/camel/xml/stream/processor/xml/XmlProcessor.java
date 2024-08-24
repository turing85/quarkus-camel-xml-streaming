package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
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
  @SuppressWarnings("unchecked")
  public void process(Exchange exchange) throws Exception {
    String converted = exchange.getIn().getBody(String.class);
    Result result =
        parse(converted, exchange.getProperty(PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT,
            Collections.emptyList(), List.class));
    exchange.setProperty(PROPERTY_NAME_REQUEST, result.requests().getFirst());
    exchange.setProperty(PROPERTY_NAME_RESPONSE, result.responses().getFirst());
    exchange.setProperty(PROPERTY_NAME_ADDITIONAL_VALUES, result.additionalValues().entrySet()
        .stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getFirst())));
  }

  Result parse(String input, List<String> additionalValuesToExtract) throws Exception {
    Map<String, XMLExtractor> extractors = new HashMap<>();
    TagBodyExtractor requestExtractor = new TagBodyExtractor("request", input);
    extractors.put("INTERNAL-request", requestExtractor);
    TagBodyExtractor responseExtractor = new TagBodyExtractor("response", input);
    extractors.put("INTERNAL-response", responseExtractor);
    for (String additionalValue : additionalValuesToExtract) {
      extractors.put(additionalValue, new ValueExtractor(additionalValue));
    }

    XMLEventReader reader = INPUT_FACTORY.createXMLEventReader(new StringReader(input));
    List<String> path = new ArrayList<>();
    List<String> unmodifiable = List.copyOf(path);
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        StartElement startElement = event.asStartElement();
        path.add(startElement.getName().getLocalPart());
        unmodifiable = List.copyOf(path);
        for (XMLExtractor extractor : extractors.values()) {
          extractor.handleStartElement(startElement, unmodifiable);
        }
      }
      for (XMLExtractor extractor : extractors.values()) {
        extractor.handleEventRecording(event, unmodifiable);
      }
      if (event.isEndElement()) {
        EndElement endElement = event.asEndElement();
        for (XMLExtractor extractor : extractors.values()) {
          extractor.handleEndElement(endElement, unmodifiable);
        }
        path.removeLast();
        unmodifiable = List.copyOf(path);
      }
    }
    // @formatter:off
    return new Result(
        List.copyOf(requestExtractor.getValues()),
        List.copyOf(responseExtractor.getValues()),
        extractors.entrySet().stream()
            .filter(not(entry -> entry.getKey().startsWith("INTERNAL")))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> List.copyOf(entry.getValue().getValues()))));
    // @formatter:on
  }

  record Result(List<String> requests, List<String> responses,
      Map<String, List<String>> additionalValues) {}
}
