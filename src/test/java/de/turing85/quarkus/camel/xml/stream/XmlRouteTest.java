package de.turing85.quarkus.camel.xml.stream;

import java.io.IOException;
import java.nio.charset.Charset;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class XmlRouteTest {
  @Test
  void testXmlRoute() throws IOException {
    // @formatter:off
    RestAssured
        .given()
            .contentType(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .accept(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .body(getClass().getClassLoader().getResourceAsStream("input.xml"))

        .when().post("/xml")

        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .body(is(new String(
                getClass().getClassLoader()
                    .getResourceAsStream("expected.xml.txt")
                    .readAllBytes(),
                Charset.forName("ISO-8859-15"))));
    // @formatter:on
  }
}
