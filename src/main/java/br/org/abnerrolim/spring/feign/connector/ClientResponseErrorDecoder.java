package br.org.abnerrolim.spring.feign.connector;

import br.org.abnerrolim.spring.feign.connector.obfuscate.HttpObfuscator;
import br.org.abnerrolim.spring.feign.connector.obfuscate.NoneHttpObfuscator;
import br.org.abnerrolim.spring.feign.connector.obfuscate.ResponseWrapper;
import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

public class ClientResponseErrorDecoder implements ErrorDecoder {

    private final FeignJsonMapper jsonMapper;
    private final HttpObfuscator httpObfuscator;
    private final String errorPrefix;
    private final String moduleName;
    private final static Pattern JSON_MEDIA =  Pattern.compile(".*" + MediaType.APPLICATION_JSON_VALUE + ".*");
    private static final Logger log = LoggerFactory.getLogger(ClientResponseErrorDecoder.class);

    public ClientResponseErrorDecoder(FeignJsonMapper jsonMapper, HttpObfuscator httpObfuscator, String errorPrefix, String moduleName) {
        this.jsonMapper = jsonMapper;
        this.httpObfuscator = httpObfuscator;
        this.errorPrefix = errorPrefix;
        this.moduleName = moduleName;
    }

    public ClientResponseErrorDecoder(FeignJsonMapper jsonMapper, String errorPrefix, String moduleName) {
        this.jsonMapper = jsonMapper;
        this.httpObfuscator = new NoneHttpObfuscator();
        this.errorPrefix = errorPrefix;
        this.moduleName = moduleName;
    }

    @Override
    public ClientResponseException decode(String methodKey, Response response) {
        try {
            ResponseWrapper wrapper = httpObfuscator.obfuscate(response);
            log.error("{} Module - Error calling method {}. Request was [{}].\nResponse was [{}]", moduleName, methodKey, httpObfuscator.obfuscate(response.request()), wrapper);
            ErrorResponse errorResponse;
            if (isJson(response)) {
                JsonNode node = jsonMapper.getMAPPER().readTree(wrapper.getOriginalCopy().body().asInputStream());
                if(node.isArray()){
                    node = node.get(0);
                }
                errorResponse = jsonMapper.getMAPPER().convertValue(node, ErrorResponse.class);
            }else
                errorResponse = ErrorResponse.builder().build();
            if (isNotCapturedClientMessage(errorResponse))
                errorResponse = buildUnknownErrorMessage(response, errorResponse.metadata());
            return ClientResponseException.of(errorResponse, response.status());
        } catch (Exception e) {
            log.error("{} Module - Exception calling method {}. Exception was:", moduleName, methodKey, e);
            return ClientResponseException.of(buildUnknownErrorMessage(response, Collections.emptyMap()), response.status());
        }
    }

    private boolean isJson(Response response) {
        return response.headers().containsKey(HttpHeaders.CONTENT_TYPE)
                && response.headers().get(HttpHeaders.CONTENT_TYPE).stream()
        .anyMatch(s -> JSON_MEDIA.matcher(s).matches());
    }

    private ErrorResponse buildUnknownErrorMessage(Response response, Map<String, Object> metadata) {
        String code = String.join("-", errorPrefix, String.valueOf(response.status()));
        String message = StringUtils.isEmpty(response.reason()) ?
                String.format("Unmapped error message of connector on module %s", moduleName)
                : response.reason();
        ErrorResponse errorResponse = ErrorResponse.of(code, message);
        if (metadata != null && !metadata.isEmpty())
            errorResponse.metadata(metadata);
        return errorResponse;
    }

    private boolean isNotCapturedClientMessage(ErrorResponse errorResponse) {
        return errorResponse.getCode() == null
                && (errorResponse.getFields() == null
                || errorResponse.getFields().isEmpty());
    }

    public static class ClientResponseException extends FeignException {

        public final ErrorResponse response;
        public final int httpStatusResponse;


        ClientResponseException(String message,
                                ErrorResponse response, int httpStatusResponse) {
            super(message);
            this.response = response;
            this.httpStatusResponse = httpStatusResponse;
        }

        static ClientResponseException of(ErrorResponse response, int httpStatusResponse) {
            return new ClientResponseException("Connector request fail", response, httpStatusResponse);
        }
    }
}
