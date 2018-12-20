package br.org.abnerrolim.spring.feign.connector.obfuscate;

import feign.Request;
import feign.Response;

public class NoneHttpObfuscator implements HttpObfuscator {

    public RequestWrapper obfuscate(Request request) {
        return new RequestWrapper(request);
    }

    public ResponseWrapper obfuscate(Response response) {
        return new ResponseWrapper(response, response);
    }
}
