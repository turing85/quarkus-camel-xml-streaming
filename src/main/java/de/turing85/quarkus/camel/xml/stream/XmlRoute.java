package de.turing85.quarkus.camel.xml.stream;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;

import de.turing85.quarkus.camel.xml.stream.processor.xml.XmlProcessor;
import io.vertx.core.http.HttpMethod;
import lombok.AllArgsConstructor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.platformHttp;

@Singleton
@AllArgsConstructor
public class XmlRoute extends RouteBuilder {
  public static final String ENCODING_GROUP_NAME = "encoding";
  private static final Pattern ENCODING_EXTRACTOR =
      Pattern.compile("^.*;charset=(?<%s>.*?)(?:;.*)?$".formatted(ENCODING_GROUP_NAME));

  private final XmlProcessor processor;

  @Override
  public void configure() {
    // @formatter:off
    from(
        platformHttp("/xml")
            .httpMethodRestrict(HttpMethod.POST.name())
            .consumes(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .produces(MediaType.APPLICATION_XML))
        .log("headers: ${headers}")
        .setProperty(XmlProcessor.PROPERTY_NAME_ADDITIONAL_VALUES_TO_EXTRACT,
            constant(Set.of("bang", "bongo")))
        .process(processor)
        .process(XmlRoute::constructResponse)
    ;
    // @formatter:on
  }

  private static void constructResponse(Exchange exchange) {
    constructBody(exchange);
    configureResponseContentType(exchange);
  }

  private static void configureResponseContentType(Exchange exchange) {
    String accept = exchange.getIn().getHeader("Accept", String.class);
    Matcher matcher = ENCODING_EXTRACTOR.matcher(accept);
    if (matcher.matches()) {
      setCharset(exchange, matcher.group(ENCODING_GROUP_NAME));
    }
  }

  private static void setCharset(Exchange exchange, String charset) {
    Message in = exchange.getIn();
    // @formatter:off
    in.setHeaders(Map.of(
        Exchange.HTTP_CHARACTER_ENCODING,
        charset,

        Exchange.CONTENT_TYPE,
        "%s;charset=%s".formatted(in.getHeader(Exchange.CONTENT_TYPE, String.class), charset)));
    // @formatter:on
    in.setBody(in.getBody(String.class).getBytes(Charset.forName(charset)));
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
