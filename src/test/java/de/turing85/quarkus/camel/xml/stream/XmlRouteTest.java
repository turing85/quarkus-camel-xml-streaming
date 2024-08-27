package de.turing85.quarkus.camel.xml.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;

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
    String expected = getExpected();
    // @formatter:off
    RestAssured
        .given()
            .contentType(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .accept(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .body(getClass().getClassLoader().getResourceAsStream("input.xml"))

        .when().post("/xml")

        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .contentType(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .body(is(expected));
    // @formatter:on
  }

  private String getExpected() throws IOException {
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("expected.xml.txt")) {
      return new String(Objects.requireNonNull(inputStream).readAllBytes(),
          Charset.forName("ISO-8859-15"));
    }
  }
}
