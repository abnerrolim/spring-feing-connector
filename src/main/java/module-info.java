open module br.org.abnerrolim.spring.feign.connector{


    requires feign.core;
    requires feign.httpclient;
    requires feign.jackson;

    requires jackson.annotations;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.datatype.jdk8;

    requires spring.web;
    requires spring.context;
    requires spring.core;
    requires spring.beans;
    requires spring.cloud.openfeign.core;

    requires slf4j.api;

    requires org.apache.commons.io;

    requires io.vavr;

    exports br.org.abnerrolim.spring.feign.connector;
    exports br.org.abnerrolim.spring.feign.connector.obfuscate;
}
