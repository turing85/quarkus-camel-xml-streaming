package de.turing85.quarkus.camel.xml.stream;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class XmlRouteTest {
  @Test
  void testXmlRoute() {
    // @formatter:off
    RestAssured
        .given()
            .contentType(MediaType.APPLICATION_XML)
            .accept(MediaType.APPLICATION_XML)
            .body("""
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
                    420
                </bongo>
                        </response>
                    </bing>
                </foo>
                """)

        .when().post("/xml")

        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body(is("""
                <extracted>
                    <request>
                <baz>
                    <bang>1337</bang>
                </baz>
                    </request>
                    <response>
                <bongo>
                    420
                </bongo>
                    </response>
                    <additionalProperties>
                        <bang>1337</bang>
                        <bongo>420</bongo>
                    </additionalProperties>
                </extracted>"""));
    // @formatter:on
  }
}
