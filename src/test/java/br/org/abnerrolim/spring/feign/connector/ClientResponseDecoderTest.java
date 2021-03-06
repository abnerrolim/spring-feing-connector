package br.org.abnerrolim.spring.feign.connector;

import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import br.org.abnerrolim.spring.feign.connector.utils.ParameterizedTypeImpl;
import feign.Request;
import feign.Response;
import feign.jackson.JacksonDecoder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;

public class ClientResponseDecoderTest {

    private FeignJsonMapper jsonMapper;
    private ClientResponseDecoder decoder;

    @Before
    public void init(){
        jsonMapper = new FeignJsonMapper();
        decoder = new ClientResponseDecoder(new JacksonDecoder(jsonMapper.getMAPPER()), "Prefix");
    }

    @Test
    public void decodeObjectResponseWithWrapper() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));

        ObjectResponse objectResponse = new ObjectResponse();
        String body = jsonMapper.write(objectResponse);
        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                body.getBytes(),
                Charset.defaultCharset()
        );

        Response response = Response.builder()
                .headers(headers)
                .body(body.getBytes())
                .status(200)
                .reason("OK")
                .request(request)
                .build();

        ParameterizedType parameterizedType = ParameterizedTypeImpl.make(ClientResponse.class, new Type[]{ObjectResponse.class}, null);
        Object resEntity = decoder.decode(response, ObjectResponse.class);

        Assert.assertThat(resEntity.getClass(), equalTo(ObjectResponse.class));
        ObjectResponse parsed = (ObjectResponse) resEntity;
        Assert.assertThat(parsed, is(equalTo(objectResponse)));
    }


    @Test
    public void decodeObjectResponseWithHttpAndWrapper() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));

        ObjectResponse objectResponse = new ObjectResponse();
        String body = jsonMapper.write(objectResponse);
        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                body.getBytes(),
                Charset.defaultCharset()
        );

        Response response = Response.builder()
                .headers(headers)
                .body(body.getBytes())
                .status(200)
                .reason("OK")
                .request(request)
                .build();

        ParameterizedType parameterizedType = ParameterizedTypeImpl.make(ResponseEntity.class, new Type[]{ObjectResponse.class}, null);
        Object decoded = decoder.decode(response, parameterizedType);

        Assert.assertThat(decoded, notNullValue());
        Assert.assertThat(decoded.getClass(), equalTo(ResponseEntity.class));
        ResponseEntity resEntity = (ResponseEntity) decoded;
        Assert.assertThat(resEntity.getBody().getClass(), equalTo(ObjectResponse.class));
        ObjectResponse parsed = (ObjectResponse) resEntity.getBody();
        Assert.assertThat(parsed, equalTo(objectResponse));
    }

    @Test
    public void decodeObjectResponse() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));

        ObjectResponse objectResponse = new ObjectResponse();
        String body = jsonMapper.write(objectResponse);
        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                body.getBytes(),
                Charset.defaultCharset()
        );

        Response response = Response.builder()
                .headers(headers)
                .body(body.getBytes())
                .status(200)
                .reason("OK")
                .request(request)
                .build();
        Object decoded = decoder.decode(response, ObjectResponse.class);
        Assert.assertThat(decoded, notNullValue());
        Assert.assertThat(decoded.getClass(), equalTo(ObjectResponse.class));
    }


    @Test
    public void decodeVoidResponse() throws Exception{
        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));

        ObjectResponse objectResponse = new ObjectResponse();
        String body = jsonMapper.write(objectResponse);
        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                body.getBytes(),
                Charset.defaultCharset()
        );

        Response response = Response.builder()
                .headers(headers)
                .body((Response.Body) null)
                .status(200)
                .reason("OK")
                .request(request)
                .build();

        Type[] types = new Type[]{Void.class};
        ParameterizedType parameterizedType = ParameterizedTypeImpl.make(ResponseEntity.class,types, null );

        Object decoded = decoder.decode(response, parameterizedType);


        Assert.assertThat(decoded, notNullValue());
        Assert.assertThat(decoded.getClass(), equalTo(ResponseEntity.class));
        ResponseEntity resEntity = (ResponseEntity) decoded;
        Assert.assertThat(resEntity.getBody(), is(nullValue()));

    }


    public static class ObjectResponse {
        public String name = "Secret Name";
        public Integer age = 10;
        public Collection<String> collect = Collections.singleton("Ahow");

        @Override
        public String toString() {
            return "ObjectResponse{" + "name='" + name + '\'' + ", age=" + age + ", collect=" + collect + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ObjectResponse that = (ObjectResponse) o;
            return Objects.equals(name, that.name) && Objects.equals(age, that.age) && collect.size() == that.collect.size() && collect.containsAll(that.collect);
        }

        @Override
        public int hashCode() {

            return Objects.hash(name, age, collect);
        }
    }

}
