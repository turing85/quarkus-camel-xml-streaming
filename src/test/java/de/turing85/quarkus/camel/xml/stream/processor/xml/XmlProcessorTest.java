package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.truth.Truth;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;

@lombok.extern.slf4j.Slf4j
@Slf4j
class XmlProcessorTest {
  @Test
  void perfTest() throws Exception {
    final XmlProcessor uut = new XmlProcessor();
    String input = new String(
        Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("input.xml"))
            .readAllBytes(),
        Charset.forName("ISO-8859-15"));
    List<String> valuesToExtract = List.of("bang", "bongo", "empty", "anotherEmpty");
    // @formatter:off
    XmlProcessor.Result expected = new XmlProcessor.Result(
        List.of("""
            <baz>
                <bang>1337</bang>
                <request>
            <boom>42</boom>
                </request>
            </baz>"""),
        List.of("""
            <bongo>
                ÄÖ
            </bongo>"""),
        Map.of(
            "bang", List.of("1337"),
            "bongo", List.of("\n    ÄÖ\n"),
            "empty", List.of(""),
            "anotherEmpty", List.of("")));
    // @formatter:on
    log.info("Warming up");
    int runs = 20_000;
    for (int i = 1; i <= runs; i++) {
      run(uut, input, valuesToExtract, expected);
    }

    log.info("Warmup done!");
    long consumed = 0;
    runs = 1_000_000;
    for (int i = 1; i <= runs; i++) {
      consumed += run(uut, input, valuesToExtract, expected);
      if (i % 100_000 == 0) {
        log.info("{} xmls in {}", "%4dk".formatted(i / 1_000), Duration.ofNanos(consumed));
      }
    }
  }

  private static long run(XmlProcessor uut, String input, List<String> valuesToExtract,
      XmlProcessor.Result expected) throws Exception {
    long start = System.nanoTime();
    XmlProcessor.Result actual = uut.parse(input, valuesToExtract);
    long consumed = System.nanoTime() - start;

    // then
    Truth.assertThat(actual).isEqualTo(expected);
    return consumed;
  }

}
