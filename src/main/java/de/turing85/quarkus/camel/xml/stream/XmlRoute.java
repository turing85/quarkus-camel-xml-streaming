package de.turing85.quarkus.camel.xml.stream;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;

import de.turing85.quarkus.camel.xml.stream.processor.xml.XmlProcessor;
import io.vertx.core.http.HttpMethod;
import lombok.AllArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.platformHttp;

@Singleton
@AllArgsConstructor
public class XmlRoute extends RouteBuilder {
  private final XmlProcessor processor;

  @Override
  public void configure() {
    // @formatter:off
    from(platformHttp("/xml")
        .httpMethodRestrict(HttpMethod.POST.name())
        .consumes(MediaType.APPLICATION_XML)
        .produces(MediaType.APPLICATION_XML))
        .setProperty(XmlProcessor.PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT,
            constant(Set.of("bang", "bongo")))
        .process(processor)
        .process(XmlRoute::constructBody)
    ;
    // @formatter:on
  }

  private static void constructBody(Exchange exchange) {
    @SuppressWarnings("unchecked")
    Map<String, String> additionalValues =
        exchange.getProperty(XmlProcessor.PROPERTY_NAME_ADDITIONAL_VALUES, Map.class);
    // @formatter:off
    exchange.getIn().setBody("""
        <extracted>
            <request>
        %s
            </request>
            <response>
        %s
            </response>
            <additionalProperties>
        %s
            </additionalProperties>
        </extracted>"""
        .formatted(
            exchange.getProperty(XmlProcessor.PROPERTY_NAME_REQUEST),
            exchange.getProperty(XmlProcessor.PROPERTY_NAME_RESPONSE),
            additionalValues.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "        <%1$s>%2$s</%1$s>"
                    .formatted(entry.getKey(), entry.getValue().trim()))
                .collect(Collectors.joining("\n"))));
    // @formatter:on
  }
}
