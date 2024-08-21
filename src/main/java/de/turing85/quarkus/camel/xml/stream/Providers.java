package de.turing85.quarkus.camel.xml.stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@Dependent
public class Providers {
  @Produces
  public XMLInputFactory xmlInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }

  @Produces
  public XMLOutputFactory xmlOutputFactory() {
    return XMLOutputFactory.newInstance();
  }
}
