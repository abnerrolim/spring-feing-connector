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
    private Decoder decoder;
    private HttpObfuscator httpObfuscator;
    private String moduleName;

    private static final Logger log = LoggerFactory.getLogger(ClientResponseDecoder.class);

    public ClientResponseDecoder(Decoder decoder, HttpObfuscator httpObfuscator, String moduleName) {
        super(decoder);
        this.decoder = decoder;
        this.httpObfuscator = httpObfuscator;
        this.moduleName = moduleName;
    }

    public ClientResponseDecoder(Decoder decoder, String moduleName) {
        super(decoder);
        this.decoder = decoder;
        this.httpObfuscator = new NoneHttpObfuscator();
        this.moduleName = moduleName;
    }

    public Object decode(Response response, Type type) throws IOException, FeignException {

        ResponseWrapper wrapper = httpObfuscator.obfuscate(response);
        log.info("{} Module - Request was [{}]. Response was [{}]", moduleName, httpObfuscator.obfuscate(response.request()), wrapper);
        if (this.isParameterizeHttpEntity(type)) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (isParameterizedClientResponse(type)) {
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
                Object decodedObject = decoder.decode(wrapper.getOriginalCopy(), type);
                return this.createHttpResponse(decodedObject, wrapper.getOriginalCopy(), type);
            }
        } else if (this.isParameterizedClientResponse(type)) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
            Object decodedObject = decoder.decode(wrapper.getOriginalCopy(), type);
            return this.createClientResponse(decodedObject, wrapper.getOriginalCopy());
        }
        return super.decode(wrapper.getOriginalCopy(), type);
    }

    private boolean isParameterizedClientResponse(Type type) {
        return type instanceof ParameterizedType ? this.isClientResponse(((ParameterizedType) type).getRawType()) : false;
    }

    private boolean isParameterizeHttpEntity(Type type) {
        return type instanceof ParameterizedType ? this.isHttpEntity(((ParameterizedType) type).getRawType()) : false;
    }

    private boolean isHttpEntity(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return HttpEntity.class.isAssignableFrom(c);
        }
        return false;
    }

    private boolean isClientResponse(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return ClientResponse.class.isAssignableFrom(c);
        }
        return false;
    }

    private <T> ClientResponse<T> createClientResponse(T instance, Response response) {
        return ClientResponse.successOf(instance, response.status());
    }

    private <T> ResponseEntity<ClientResponse<T>> createHttpResponse(T instance, Response response, Type type) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Iterator<String> headersKey = response.headers().keySet().iterator();
        while (headersKey.hasNext()) {
            String key = headersKey.next();
            headers.put(key, new LinkedList(response.headers().get(key)));
        }
        ClientResponse<T> clientResponse = createClientResponse(instance, response);

        return new ResponseEntity<>(clientResponse, headers, HttpStatus.valueOf(response.status()));
    }

}
