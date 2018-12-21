package br.org.abnerrolim.spring.feign.connector;

import br.org.abnerrolim.spring.feign.connector.obfuscate.HttpObfuscator;
import br.org.abnerrolim.spring.feign.connector.obfuscate.NoneHttpObfuscator;
import br.org.abnerrolim.spring.feign.connector.obfuscate.ResponseWrapper;
import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedList;

public class ClientResponseDecoder extends ResponseEntityDecoder {
    private HttpObfuscator httpObfuscator;
    private String moduleName;

    private static final Logger log = LoggerFactory.getLogger(ClientResponseDecoder.class);

    public ClientResponseDecoder(Decoder decoder, HttpObfuscator httpObfuscator, String moduleName) {
        super(decoder);
        this.httpObfuscator = httpObfuscator;
        this.moduleName = moduleName;
    }

    public ClientResponseDecoder(Decoder decoder, String moduleName) {
        super(decoder);
        this.httpObfuscator = new NoneHttpObfuscator();
        this.moduleName = moduleName;
    }

    public Object decode(Response response, Type type) throws IOException, FeignException {
        ResponseWrapper wrapper = httpObfuscator.obfuscate(response);
        log.info("{} Module - Request was [{}]. Response was [{}]", moduleName, httpObfuscator.obfuscate(response.request()), wrapper);
        return super.decode(wrapper.getOriginalCopy(), type);
    }
}
