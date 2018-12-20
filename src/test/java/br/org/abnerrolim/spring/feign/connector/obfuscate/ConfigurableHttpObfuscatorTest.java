package br.org.abnerrolim.spring.feign.connector.obfuscate;

import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import feign.Request;
import feign.Response;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;

public class ConfigurableHttpObfuscatorTest {

    ConfigurableHttpObfuscator configurableHttpObfuscator;
    FeignJsonMapper jsonMapper = new FeignJsonMapper();

    @Test
    public void obfuscateRequest() throws Exception{

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

        ConfigurableHttpObfuscator configurableHttpObfuscator =  ConfigurableHttpObfuscator.builder()
                .headers("secret-header", "secret-collection")
                .jsonBodyFields("name", "insideObject", "insideName")
                .pathSegments(Arrays.asList("/v1/secret-code/${obfuscate}/public-code/${value}/"))
                .build();

        String logString = configurableHttpObfuscator.obfuscate(request).toString();
        Assert.assertThat(logString, allOf(not(containsString("230329324")), not(containsString("3924DJAU)#OJIDS")), not(containsString("daklj2#$#@")), not(containsString("sdjloijh234"))));

    }

    @Test
    public void obfuscateResponse() throws Exception{

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

        Response response = Response.builder()
                .headers(headers)
                .body(body.getBytes())
                .status(200)
                .reason("OK")
                .request(request)
                .build();


        ConfigurableHttpObfuscator configurableHttpObfuscator =  ConfigurableHttpObfuscator.builder()
                .headers("secret-header", "secret-collection")
                .jsonBodyFields("name", "insideObject.insideName", "insideObject.insideInsideObject.deepNameHideMe", "insideName")
                .pathSegments(Arrays.asList("/v1/secret-code/${obfuscate}/public-code/${value}/"))
                .build();

        String logString = configurableHttpObfuscator.obfuscate(response).toString();
        Assert.assertThat(logString, allOf(not(containsString("230329324")), not(containsString("3924DJAU)#OJIDS")), not(containsString("daklj2#$#@")), not(containsString("sdjloijh234"))));

    }


    public static class ObjectToObfuscate{
        public String name = "Secret Name";
        public Integer age = 10;
        public Collection<String> collect = Collections.singleton("Ahow");
        public InsideObject insideObject = new InsideObject();
    }
    public static class InsideObject{
        public String insideName = "InsideName";
        public String address = "Non Secret";
        public InsideInsideObject insideInsideObject = new InsideInsideObject();
    }
    public static class InsideInsideObject{
        public String deepNameHideMe = "deepNameHideMe";
        public String address = "Secret";
    }
}
