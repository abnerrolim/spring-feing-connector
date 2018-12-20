package br.org.abnerrolim.spring.feign.connector.obfuscate;

import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import feign.Request;
import feign.Response;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.*;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;

public class NoneHttpObfuscatorTest {
    FeignJsonMapper jsonMapper = new FeignJsonMapper();

    @Test
    public void notObfuscateRequest() throws Exception{

        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));

        String body = jsonMapper.write(new ObjectToObfuscate());

        Request request = Request.create(
                "GET",
                "http://www.com.br:8999/v1/secret-code/230329324/public-code/243143DadeE/",
                headers,
                body.getBytes(),
                Charset.defaultCharset()
        );

        HttpObfuscator obfuscator= new NoneHttpObfuscator();
        String logString = obfuscator.obfuscate(request).toString();
        Assert.assertThat(logString, allOf(containsString("230329324"), containsString("3924DJAU)#OJIDS"),containsString("daklj2#$#@"), containsString("sdjloijh234")));

    }
    @Test
    public void notObfuscateResponse() throws Exception{

        Map<String, Collection<String>> headers = new HashMap<>();
        headers.put("secret-header", Collections.singleton("3924DJAU)#OJIDS"));
        headers.put("public-header", Collections.singleton("1124DJAU)#OJITR"));
        headers.put("secret-collection", Arrays.asList("daklj2#$#@", "sdjloijh234"));

        String body = jsonMapper.write(new  ObjectToObfuscate());
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

        HttpObfuscator obfuscator= new NoneHttpObfuscator();
        String logString = obfuscator.obfuscate(response).toString();
        Assert.assertThat(logString, allOf(containsString("3924DJAU)#OJIDS"), containsString("daklj2#$#@"), containsString("sdjloijh234")));

    }


    public static class ObjectToObfuscate{
        public String name = "Secret Name";
        public Integer age = 10;
        public Collection<String> collect = Collections.singleton("Ahow");
        public ConfigurableHttpObfuscatorTest.InsideObject insideObject = new ConfigurableHttpObfuscatorTest.InsideObject();
    }
    public static class InsideObject{
        public String insideName = "InsideName";
        public String address = "Non Secret";
        public ConfigurableHttpObfuscatorTest.InsideInsideObject insideInsideObject = new ConfigurableHttpObfuscatorTest.InsideInsideObject();
    }
    public static class InsideInsideObject{
        public String deepNameHideMe = "deepNameHideMe";
        public String address = "Secret";
    }
}
