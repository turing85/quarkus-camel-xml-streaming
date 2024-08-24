package de.turing85.quarkus.camel.xml.stream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.common.truth.Truth;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
class XmlRouteTest {
  @Test
  void testXmlRoute() throws IOException {
    byte[] expected = getExpected();
    // @formatter:off
    byte[] actual = RestAssured
        .given()
            .contentType(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .accept(MediaType.APPLICATION_XML + ";charset=ISO-8859-15")
            .body(getClass().getClassLoader().getResourceAsStream("input.xml"))

        .when().post("/xml")

        .then()
            .statusCode(is(Response.Status.OK.getStatusCode()))
            .extract().body().asByteArray();
    // @formatter:on
    Truth.assertThat(actual).isEqualTo(expected);
  }

  private byte[] getExpected() throws IOException {
    try (
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("expected.xml")) {
      return Objects.requireNonNull(inputStream).readAllBytes();
    }
  }
}
