package br.org.abnerrolim.spring.feign.connector.obfuscate;

import feign.Request;
import feign.Response;

import java.io.IOException;

public interface HttpObfuscator {


    RequestWrapper obfuscate(Request request) throws IOException;

    ResponseWrapper obfuscate(Response response) throws IOException;

}
