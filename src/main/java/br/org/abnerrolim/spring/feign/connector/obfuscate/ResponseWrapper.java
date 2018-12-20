package br.org.abnerrolim.spring.feign.connector.obfuscate;

import feign.Response;

public class ResponseWrapper {

    public final Response obfuscated;
    public final Response original;

    ResponseWrapper(final Response obfuscated, final Response original) {
        this.obfuscated = obfuscated;
        this.original = original;
    }

    public Response getOriginalCopy(){
        return original;
    }

    @Override
    public String toString() {
        return this.obfuscated.toString();
    }
}
