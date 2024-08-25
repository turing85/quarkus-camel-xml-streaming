package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.truth.Truth;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;

@lombok.extern.slf4j.Slf4j
@Slf4j
class XmlProcessorTest {
  @Test
  void perfTest() throws Exception {
    String input = new String(
        Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("input.xml"))
            .readAllBytes(),
        Charset.forName("ISO-8859-15"));
    Set<String> valuesToExtract = Set.of("bang", "bongo", "empty", "anotherEmpty");
    // @formatter:off
    XmlProcessor.Result expected = new XmlProcessor.Result(
        List.of("""
            <baz>
                <bang>1337</bang>
                <bang>42</bang>
                <request>
            <boom>42</boom>
                </request>
            </baz>"""),
        List.of("""
            <bongo>
                ÄÖ
            </bongo>"""),
        Map.of(
            "bang", List.of("1337", "42"),
            "bongo", List.of("\n    ÄÖ\n"),
            "empty", List.of(""),
            "anotherEmpty", List.of("")));
    // @formatter:on
    log.info("Warming up");
    int runs = 20_000;
    for (int i = 1; i <= runs; i++) {
      run(input, valuesToExtract, expected);
    }

    log.info("Warmup done!");
    long consumed = 0;
    runs = 100_000;
    for (int i = 1; i <= runs; i++) {
      consumed += run(input, valuesToExtract, expected);
      if (i % 10_000 == 0) {
        log.info("{} xmls in {}", "%3dk".formatted(i / 1_000), Duration.ofNanos(consumed));
      }
    }
  }

  private static long run(String input, Set<String> valuesToExtract, XmlProcessor.Result expected)
      throws Exception {
    long start = System.nanoTime();
    XmlProcessor.Result actual = XmlProcessor.parse(input, valuesToExtract);
    long consumed = System.nanoTime() - start;

    // then
    Truth.assertThat(actual).isEqualTo(expected);
    return consumed;
  }

}
