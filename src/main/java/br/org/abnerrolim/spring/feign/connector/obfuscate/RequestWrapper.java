package br.org.abnerrolim.spring.feign.connector.obfuscate;

import feign.Request;

public class RequestWrapper {

    public final Request request;

    RequestWrapper(final Request request){
        this.request = request;
    }

    @Override
    public String toString(){
        return this.request.toString();
    }
}
