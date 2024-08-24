package de.turing85.quarkus.camel.xml.stream.processor.xml;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.google.common.truth.Truth;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;

@lombok.extern.slf4j.Slf4j
@Slf4j
class XmlProcessorTest {
  @Test
  void perfTest() throws Exception {
    final XmlProcessor uut = new XmlProcessor();
    String input = """
        <?xml version="1.0" encoding="ISO-8859-15" ?>
        <foo>
            <bar>
                <request>
        <baz>
            <bang>1337</bang>
        </baz>
                </request>
            </bar>
            <bing>
                <response>
        <bongo>
            ÄÖ
        </bongo>
                </response>
            </bing>
        </foo>""";
    List<String> valuesToExtract = List.of("bang", "bongo");
    XmlProcessor.Result expected = new XmlProcessor.Result(List.of("""
        <baz>
            <bang>1337</bang>
        </baz>"""), List.of("""
        <bongo>
            ÄÖ
        </bongo>"""), Map.of("bang", List.of("1337"), "bongo", List.of("\n    ÄÖ\n")));

    log.info("Warming up");
    int runs = 50_000;
    for (int i = 1; i <= runs; i++) {
      run(uut, input, valuesToExtract, expected);
    }

    log.info("Warmup done!");
    long consumed = 0;
    runs = 1_000_000;
    for (int i = 1; i <= runs; i++) {
      consumed += run(uut, input, valuesToExtract, expected);
      if (i % 100_000 == 0) {
        logStats(i, consumed);
      }
    }
    log.info("Final stats:");
    logStats(runs, consumed);
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

  private static void logStats(int runs, long consumed) {
    log.info("{} xmls in {}", "%7d".formatted(runs), Duration.ofNanos(consumed));
  }
}
