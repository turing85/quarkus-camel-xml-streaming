package de.turing85.quarkus.camel.xml.stream;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import jakarta.inject.Singleton;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@Singleton
@AllArgsConstructor
public class XmlProcessor implements Processor {
  public static final String PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT =
      "additionalValuesToExtract";
  public static final String PROPERTY_NAME_ADDITIONAL_VALUES = "additionalValues";
  public static final String PROPERTY_NAME_REQUEST = "property-request";
  public static final String PROPERTY_NAME_RESPONSE = "property-response";

  private final XMLInputFactory inputFactory;
  private final XMLOutputFactory outputFactory;

  @Override
  @SuppressWarnings("unchecked")
  public void process(Exchange exchange) throws XMLStreamException {

    Result result = parse(exchange.getIn().getBody(String.class),
        exchange.getProperty(PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT, List.of(), List.class));

    exchange.setProperty(PROPERTY_NAME_ADDITIONAL_VALUES,
        Collections.unmodifiableMap(result.additionalValues()));
    exchange.setProperty(PROPERTY_NAME_REQUEST, result.request());
    exchange.setProperty(PROPERTY_NAME_RESPONSE, result.response());
  }

  private Result parse(String input, List<String> additionalValuesToExtract)
      throws XMLStreamException {
    StringWriter requestWriter = new StringWriter();
    StringWriter responseWriter = new StringWriter();
    Map<String, StringWriter> additionalWriters =
        constructAdditionalWriters(additionalValuesToExtract);

    parse(input, requestWriter, responseWriter, additionalWriters);

    // @formatter:off
    return new Result(
        requestWriter.toString(),
        responseWriter.toString(),
        additionalWriters.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().toString())));
    // @formatter:on
  }

  private static Map<String, StringWriter> constructAdditionalWriters(
      List<String> additionalValuesToExtract) {
    Map<String, StringWriter> additionalWriters =
        HashMap.newHashMap(additionalValuesToExtract.size());
    for (String additionalValue : additionalValuesToExtract) {
      additionalWriters.put(additionalValue, new StringWriter());
    }
    return additionalWriters;
  }

  private void parse(String input, StringWriter requestWriter, StringWriter responseWriter,
      Map<String, StringWriter> additionalWriters) throws XMLStreamException {
    List<ThrowingBiConsumer<XMLEvent, State>> handlers =
        constructHandlers(requestWriter, responseWriter, additionalWriters);
    State state = new State().additionalNames(additionalWriters.keySet());

    XMLEventReader reader = inputFactory.createXMLEventReader(new StringReader(input));
    while (reader.hasNext()) {
      final XMLEvent event = reader.nextEvent();

      handleStartEvent(event, state);
      handleEventRecording(event, state, handlers);
      handleEndEvent(event, state);
    }
  }

  private List<ThrowingBiConsumer<XMLEvent, State>> constructHandlers(StringWriter requestWriter,
      StringWriter responseWriter, Map<String, StringWriter> additionalWriters)
      throws XMLStreamException {
    List<ThrowingBiConsumer<XMLEvent, State>> handlers =
        new ArrayList<>(additionalWriters.size() + 2);
    handlers.add(constructRequestRecorder(outputFactory.createXMLEventWriter(requestWriter)));
    handlers.add(constructResponseRecorder(outputFactory.createXMLEventWriter(responseWriter)));
    for (Map.Entry<String, XMLEventWriter> entry : toXmlWriters(additionalWriters).entrySet()) {
      handlers.add(constructValueRecorder(entry.getKey(), entry.getValue()));
    }
    return Collections.unmodifiableList(handlers);
  }

  private Map<String, XMLEventWriter> toXmlWriters(
      Map<String, StringWriter> additionalStringWriters) throws XMLStreamException {
    Map<String, XMLEventWriter> additionalXmlWriters =
        HashMap.newHashMap(additionalStringWriters.size());
    for (Map.Entry<String, StringWriter> entry : additionalStringWriters.entrySet()) {
      additionalXmlWriters.put(entry.getKey(),
          outputFactory.createXMLEventWriter(entry.getValue()));
    }
    return Collections.unmodifiableMap(additionalXmlWriters);
  }

  private static ThrowingBiConsumer<XMLEvent, State> constructValueRecorder(String name,
      XMLEventWriter writer) {
    return (event, state) -> {
      if (state.shouldRecord(name) && event.isCharacters()) {
        writer.add(event);
      }
    };
  }

  private static ThrowingBiConsumer<XMLEvent, State> constructRequestRecorder(
      XMLEventWriter requestWriter) {
    return (event, state) -> {
      if (state.recordRequest()) {
        requestWriter.add(event);
      }
    };
  }

  private static ThrowingBiConsumer<XMLEvent, State> constructResponseRecorder(
      XMLEventWriter responseWriter) {
    return (event, state) -> {
      if (state.recordResponse()) {
        responseWriter.add(event);
      }
    };
  }

  private static void handleStartEvent(XMLEvent event, State state) {
    if (event.isStartElement()) {
      String name = event.asStartElement().getName().getLocalPart();

      if (name.equals("request")) {
        state.requestActive(true);
      } else if (state.requestActive() && state.activeRequestElement().isEmpty()) {
        state.activeRequestElement(name);
      }

      if (name.equals("response")) {
        state.responseActive(true);
      } else if (state.activeResponseElement().isEmpty() && state.responseActive()) {
        state.activeResponseElement(name);
      }

      for (String additionalName : state.additionalNames()) {
        if (name.equals(additionalName)) {
          state.activate(additionalName);
        }
      }
    }
  }

  private void handleEventRecording(XMLEvent event, State state,
      List<ThrowingBiConsumer<XMLEvent, State>> consumers) {
    for (ThrowingBiConsumer<XMLEvent, State> consumer : consumers) {
      try {
        consumer.consume(event, state);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void handleEndEvent(XMLEvent event, State state) {
    if (event.isEndElement()) {
      String name = event.asEndElement().getName().getLocalPart();

      if (name.equals(state.activeRequestElement().orElse(""))) {
        state.unsetActiveRequestElement();
      } else if (name.equals("request")) {
        state.requestActive(false);
      }

      if (name.equals(state.activeResponseElement().orElse(""))) {
        state.unsetActiveResponseElement();
      } else if (name.equals("response")) {
        state.responseActive(false);
      }

      for (String additionalName : state.additionalNames()) {
        if (name.equals(additionalName)) {
          state.deactivate(additionalName);
        }
      }
    }
  }

  public interface ThrowingBiConsumer<T, U> {
    void consume(T t, U u) throws Exception;
  }

  @Getter
  @Setter
  static class State {
    private boolean requestActive = false;
    private boolean responseActive = false;
    private Set<String> additionalNames = Collections.emptySet();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Map<String, Boolean> additionalNameRecord = new HashMap<>();

    @Getter(AccessLevel.NONE)
    private String activeRequestElement = null;

    @Getter(AccessLevel.NONE)
    private String activeResponseElement = null;

    public boolean recordRequest() {
      return activeRequestElement().isPresent();
    }

    public void unsetActiveRequestElement() {
      activeRequestElement(null);
    }

    public Optional<String> activeRequestElement() {
      return Optional.ofNullable(activeRequestElement);
    }

    public boolean recordResponse() {
      return activeResponseElement().isPresent();
    }

    public void unsetActiveResponseElement() {
      activeResponseElement(null);
    }

    public Optional<String> activeResponseElement() {
      return Optional.ofNullable(activeResponseElement);
    }

    public void activate(String name) {
      verifyNameIsKnown(name);
      additionalNameRecord.put(name, Boolean.TRUE);
    }

    public boolean shouldRecord(String name) {
      verifyNameIsKnown(name);
      return additionalNameRecord.getOrDefault(name, false);
    }

    public void deactivate(String name) {
      verifyNameIsKnown(name);
      additionalNameRecord.put(name, Boolean.FALSE);
    }

    private void verifyNameIsKnown(String name) {
      if (!additionalNames().contains(name)) {
        throw new IllegalStateException(
            "name '%s' is not registered as additional name".formatted(name));
      }
    }
  }

  record Result(String request, String response, Map<String, String> additionalValues) {}
}
