package br.org.abnerrolim.spring.feign.connector.obfuscate;

import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import br.org.abnerrolim.spring.feign.connector.utils.StringObfuscationUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class ConfigurableHttpObfuscator implements HttpObfuscator {

    private static final Logger log = LoggerFactory.getLogger(ConfigurableHttpObfuscator.class);

    private final List<String> headers;
    private final List<String> jsonBodyFields;
    private final Map<Pattern, List<Integer>> encodePathSegments;

    private final FeignJsonMapper jsonMapper;

    private ConfigurableHttpObfuscator(List<String> headers, List<String> jsonBodyFields, Map<Pattern, List<Integer>> encodePathSegments, FeignJsonMapper jsonMapper) {
        this.headers = headers;
        this.jsonBodyFields = jsonBodyFields;
        this.encodePathSegments = encodePathSegments;
        this.jsonMapper = jsonMapper;
    }

    public static Builder builder(ObjectMapper objectMapper){
        return new Builder(objectMapper);
    }
    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {
        private FeignJsonMapper jsonMapper;
        private List<String> headers;
        private List<String> jsonBodyFields;
        private Map<Pattern, List<Integer>> encodePathSegments;

        private Builder(ObjectMapper objectMapper) {
            this.jsonMapper = new FeignJsonMapper(objectMapper);
            headers = new ArrayList<>();
            jsonBodyFields = new ArrayList<>();
            encodePathSegments = new HashMap<Pattern, List<Integer>>();
        }
        private Builder() {
            this.jsonMapper = new FeignJsonMapper();
            headers = new ArrayList<>();
            jsonBodyFields = new ArrayList<>();
            encodePathSegments = new HashMap<Pattern, List<Integer>>();
        }

        public Builder headers(String... headerKeys) {
            headers.addAll(Arrays.asList(headerKeys));
            return this;
        }

        public Builder jsonBodyFields(String... jsonBodyFieldsNames) {
            jsonBodyFields.addAll(Arrays.asList(jsonBodyFieldsNames));
            return this;
        }

        public Builder pathSegments(List<String> pathsObfuscateTemplate) {
            for (String template : pathsObfuscateTemplate) {
                String er = template.replaceAll("\\$\\{value\\}", ".*").replaceAll("\\$\\{obfuscate\\}", ".*");
                String[] segments = template.split("/");
                List<Integer> segmentIdx = new ArrayList<>();
                for (int i = 0; i < segments.length; i++) {
                    if ("${obfuscate}".equals(segments[i])) {
                        segmentIdx.add(i);
                    }
                }
                Pattern pattern = Pattern.compile(er);
                encodePathSegments.put(pattern, segmentIdx);
            }

            return this;
        }

        public ConfigurableHttpObfuscator build() {
            return new ConfigurableHttpObfuscator(
                    headers,
                    jsonBodyFields,
                    encodePathSegments,
                    jsonMapper
            );
        }
    }


    public RequestWrapper obfuscate(Request request){
        Map<String, Collection<String>> obfuscatedHeaders = obfuscateHeaders(request.headers());
        String obfuscatedUrl = obfuscateUrl(request.url());
        byte[] obfuscated = obfuscateBody(request.body()).getBytes();
        Request obfuscatedRequest = Request.create(
                request.method(),
                obfuscatedUrl,
                obfuscatedHeaders,
                obfuscated,
                request.charset()
        );
        return new RequestWrapper(obfuscatedRequest);
    }

    public ResponseWrapper obfuscate(Response response) throws IOException{
        Map<String, Collection<String>> obfuscatedHeaders = obfuscateHeaders(response.headers());
        byte[] obfuscated;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            IOUtils.copy(response.body().asInputStream(), baos);
            byte[] originalBody = baos.toByteArray();
            Response responseOriginalCopy = Response.builder()
                    .body(originalBody)
                    .headers(response.headers())
                    .reason(response.reason())
                    .request(response.request())
                    .status(response.status())
                    .build();
            obfuscated = obfuscateBody(baos.toByteArray()).getBytes();
            Response responseObfuscated = Response.builder()
                    .body(obfuscated)
                    .headers(obfuscatedHeaders)
                    .reason(response.reason())
                    .request(response.request())
                    .status(response.status())
                    .build();
            return new ResponseWrapper(responseObfuscated, responseOriginalCopy);
        }finally {
            response.body().close();
            baos.close();
        }
    }

    private Map<String, Collection<String>> obfuscateHeaders(Map<String, Collection<String>> rawHeaders) {
        Map<String, Collection<String>> newHeaders = new HashMap<>(rawHeaders);
        for (String header : headers) {
            if (newHeaders.containsKey(header))
                newHeaders.put(header, StringObfuscationUtils.obfuscate(newHeaders.get(header)));
        }
        return newHeaders;
    }

    private String obfuscateBody(byte[] body){
        String asString = "";
        if(body != null && body.length > 1) {
            try {
                Reader reader = new StringReader(new String(body));
                JsonNode node = StringObfuscationUtils.obfuscate(jsonMapper.read(reader, JsonNode.class), jsonBodyFields);
                asString = jsonMapper.write(node);
            }catch (Exception e){
                log.error("Unable to obfuscate current request/response body as json. Body value was [{}] and will parsed as empty string", body, e);
            }
        }
        return asString;
    }

    private String obfuscateUrl(String strUrl) {
        String newUrl = strUrl;
        if (!encodePathSegments.isEmpty()) {
            try {
                URL url = new URL(strUrl);
                if (isObfuscationCandidate(url)) {
                    List<Integer> segmentsToHide = segmentsToEncode(url);
                    newUrl = StringObfuscationUtils.obfuscate(url, segmentsToHide);

                }
            } catch (MalformedURLException u) {
                log.warn("Unable to obfuscate URL {}", strUrl, u);
                return strUrl;
            }
        }
        return newUrl;
    }

    private boolean isObfuscationCandidate(URL url) {
        return patternForUrl(url).isPresent();
    }

    private Optional<Pattern> patternForUrl(URL url) {
        String path = url.getPath();
        return encodePathSegments.keySet().stream().filter(p -> p.matcher(path).matches()).findAny();
    }

    private List<Integer> segmentsToEncode(URL url) {
        Optional<Pattern> patternForUrl = patternForUrl(url);
        if (patternForUrl.isPresent()) {
            return encodePathSegments.get(patternForUrl.get());
        } else {
            return Collections.emptyList();
        }
    }

}
