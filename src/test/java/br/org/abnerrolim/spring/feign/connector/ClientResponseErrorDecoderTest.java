package br.org.abnerrolim.spring.feign.connector;

import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import feign.Request;
import feign.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.*;
import static org.hamcrest.CoreMatchers.*;

public class ClientResponseErrorDecoderTest {

    private FeignJsonMapper jsonMapper;
    private ClientResponseErrorDecoder errorDecoder;

    @Before
    public void init(){
        jsonMapper = new FeignJsonMapper();
        errorDecoder = new ClientResponseErrorDecoder(jsonMapper, "Prefix","Module Name");
    }

    @Test
    public void decodeStandardErrorResponse() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));
        headers.put("content-type", Collections.singleton("application/json"));

        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                null,
                Charset.defaultCharset()
        );

        ObjectErrorResponse  objectErrorResponse = new ObjectErrorResponse("CCC-0001", "Message", null);
        String body = jsonMapper.write(objectErrorResponse);
        Response response = Response.builder()
                .headers(headers)
                .body(body.getBytes())
                .status(422)
                .reason("ERROR")
                .request(request)
                .build();
        ClientResponseErrorDecoder.ClientResponseException e = errorDecoder.decode("GET", response);
        Assert.assertThat(e, notNullValue());
        Assert.assertThat(e.response, notNullValue());
        Assert.assertThat(e.response.isError(), is(true));
        Assert.assertThat(e.response.getError().getCode(), notNullValue());
        Assert.assertThat(e.response.getError().getFields(), nullValue());
        Assert.assertThat(e.response.getHttpStatus().get(), equalTo(422));
    }

    @Test
    public void decodeStandardValidationFieldsResponse() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));
        headers.put("content-type", Collections.singleton("application/json"));

        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                null,
                Charset.defaultCharset()
        );

        ObjectErrorResponse  objectErrorResponse = new ObjectErrorResponse(null, null, Collections.singletonMap("field", Arrays.asList("cpf.invalid","name.empty")));
        String body = jsonMapper.write(objectErrorResponse);
        Response response = Response.builder()
                .headers(headers)
                .body(body.getBytes())
                .status(400)
                .reason("ERROR")
                .request(request)
                .build();
        ClientResponseErrorDecoder.ClientResponseException e = errorDecoder.decode("GET", response);
        Assert.assertThat(e, notNullValue());
        Assert.assertThat(e.response, notNullValue());
        Assert.assertThat(e.response.isError(), is(true));
        Assert.assertThat(e.response.getHttpStatus().get(), equalTo(400));
        Assert.assertThat(e.response.getError().getCode(), nullValue());
        Assert.assertThat(e.response.getError().getFields(), notNullValue());
    }

    @Test
    public void decodeWithNotStandardErrorResponse() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));
        headers.put("content-type", Collections.singleton("application/json"));

        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                null,
                Charset.defaultCharset()
        );

        InvalidObjectErrorResponse  objectErrorResponse = new InvalidObjectErrorResponse("1", "w");
        String body = jsonMapper.write(objectErrorResponse);
        Response response = Response.builder()
                .headers(headers)
                .body(body.getBytes())
                .status(422)
                .reason("ERROR")
                .request(request)
                .build();
        ClientResponseErrorDecoder.ClientResponseException e = errorDecoder.decode("GET", response);
        Assert.assertThat(e, notNullValue());
        Assert.assertThat(e.response, notNullValue());
        Assert.assertThat(e.response.isError(), is(true));
        Assert.assertThat(e.response.getHttpStatus().get(), equalTo(422));
        Assert.assertThat(e.response.getError().getCode(), notNullValue());
        Assert.assertThat(e.response.getError().getFields(), nullValue());
    }

    @Test
    public void decodeWithoutErrorResponse() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));

        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                null,
                Charset.defaultCharset()
        );

        Response response = Response.builder()
                .headers(headers)
                .body((Response.Body)null)
                .status(500)
                .reason("ERROR")
                .request(request)
                .build();
        ClientResponseErrorDecoder.ClientResponseException e = errorDecoder.decode("GET", response);
        Assert.assertThat(e, notNullValue());
        Assert.assertThat(e.response, notNullValue());
        Assert.assertThat(e.response.isError(), is(true));
        Assert.assertThat(e.response.getHttpStatus().get(), equalTo(500));
        Assert.assertThat(e.response.getError().getCode(), notNullValue());
        Assert.assertThat(e.response.getError().getFields(), nullValue());
    }

    @Test
    public void decodeWithErrorResponseAndExtraFields() throws Exception{

        String serialized = Utils.getPayload("payload/payment-custom-errorresponse.json");
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));
        headers.put("content-type", Collections.singleton("application/json"));

        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                null,
                Charset.defaultCharset()
        );

        Response response = Response.builder()
                .headers(headers)
                .body(serialized.getBytes())
                .status(500)
                .reason("ERROR")
                .request(request)
                .build();
        ClientResponseErrorDecoder.ClientResponseException e = errorDecoder.decode("GET", response);
        Assert.assertThat(e, notNullValue());
        Assert.assertThat(e.response, notNullValue());
        Assert.assertThat(e.response.isError(), is(true));
        Assert.assertThat(e.response.getHttpStatus().get(), equalTo(500));
        Assert.assertThat(e.response.getError().getCode(), notNullValue());
        Assert.assertThat(e.response.getError().metadata(), notNullValue());
        Assert.assertThat(e.response.getError().metadata().isEmpty(), is(false));
    }

    @Test
    public void decodeWithArrayResponse() throws Exception{

        String serialized = Utils.getPayload("payload/array-of-errorresponse.json");
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));
        headers.put("content-type", Collections.singleton("application/json"));

        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                null,
                Charset.defaultCharset()
        );

        Response response = Response.builder()
                .headers(headers)
                .body(serialized.getBytes())
                .status(500)
                .reason("ERROR")
                .request(request)
                .build();
        ClientResponseErrorDecoder.ClientResponseException e = errorDecoder.decode("GET", response);
        Assert.assertThat(e, notNullValue());
        Assert.assertThat(e.response, notNullValue());
        Assert.assertThat(e.response.isError(), is(true));
        Assert.assertThat(e.response.getHttpStatus().get(), equalTo(500));
        Assert.assertThat(e.response.getError().getCode(), notNullValue());
        Assert.assertThat(e.response.getError().getMessage(), notNullValue());
        Assert.assertThat(e.response.getError().metadata().isEmpty(), is(true));
    }
    public static class ObjectErrorResponse {

        public ObjectErrorResponse(String code, String message, Map<String, List<String>> fields){
            this.code = code;
            this.message = message;
            this.fields = fields;
        }

        public String code;
        public String message;
        public Map<String, List<String>> fields;

    }


    public static class InvalidObjectErrorResponse {

        public InvalidObjectErrorResponse(String invalidErrorCode, String otherMessage){
            this.invalidErrorCode = invalidErrorCode;
            this.otherMessage = otherMessage;
        }

        public String invalidErrorCode;
        public String otherMessage;
    }
}
