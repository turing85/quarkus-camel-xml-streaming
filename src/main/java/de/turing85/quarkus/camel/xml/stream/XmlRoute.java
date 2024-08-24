package de.turing85.quarkus.camel.xml.stream;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;

import de.turing85.quarkus.camel.xml.stream.processor.XmlProcessor;
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
        .log("charset = ${header.%s}".formatted(Exchange.CHARSET_NAME))
        .setProperty(XmlProcessor.PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT,
            constant(List.of("bang", "bongo")))
        .process(processor)
        .process(XmlRoute::constructBody)
        .convertBodyTo(byte[].class, "ISO-8859-15")
    ;
    // @formatter:on
  }

  @SuppressWarnings("unchecked")
  private static void constructBody(Exchange exchange) {
    String request = """
            <request>
        %s
            </request>""".formatted(exchange.getProperty(XmlProcessor.PROPERTY_NAME_REQUEST));
    String response = """
            <response>
        %s
            </response>""".formatted(exchange.getProperty(XmlProcessor.PROPERTY_NAME_RESPONSE));
    StringBuilder additionalValuesString =
        new StringBuilder("    <additionalProperties>").append(System.lineSeparator());
    TreeMap<String, String> additionalValues = new TreeMap<>(String::compareTo);
    additionalValues
        .putAll(exchange.getProperty(XmlProcessor.PROPERTY_NAME_ADDITIONAL_VALUES, Map.class));
    additionalValues.forEach((key, value) -> additionalValuesString
        .append("        <%1$s>%2$s</%1$s>".formatted(key, value.trim()))
        .append(System.lineSeparator()));
    additionalValuesString.append("    </additionalProperties>");
    exchange.getIn().setBody("""
        <extracted>
        %s
        %s
        %s
        </extracted>""".formatted(request, response, additionalValuesString.toString()),
        String.class);
  }
}