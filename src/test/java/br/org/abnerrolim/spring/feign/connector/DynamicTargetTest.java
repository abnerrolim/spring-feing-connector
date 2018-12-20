package br.org.abnerrolim.spring.feign.connector;

import feign.Request;
import feign.RequestTemplate;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class DynamicTargetTest {


    DynamicHostResolver dynamicHostResolver;

    @Before
    public void init(){
        dynamicHostResolver = ()-> "http://www.google.com";
    }


    @Test
    public void create() throws Exception {
        String name = "Dynamic";
        String url = "http://localhost";
        DynamicTarget<String> d = new DynamicTarget<>(
            String.class,
                name,
                url,
                dynamicHostResolver
        );

        assertThat(d.name(), equalTo(name));
        assertThat(d.url(), equalTo(url));
    }

    @Test
    public void apply() throws Exception {
        RequestTemplate requestTemplate = new RequestTemplate();
        String oldHost = "http://www.org.pt";
        String path = "/myservice/callHere";
        String body = "Body";
        String method = "POST";
        String headerKey = "HEADER";
        String headerValue = "HEADER_VALUE";
        requestTemplate.append(oldHost+path)
                .body(body)
                .method(method)
                .header(headerKey, headerValue);

        DynamicTarget<String> d = new DynamicTarget<>(
                String.class,
                "Name",
                "url",
                dynamicHostResolver
        );
        Request request = d.apply(requestTemplate);
        assertThat(request.body(), equalTo(requestTemplate.body()));
        assertThat(request.charset(), equalTo(requestTemplate.charset()));
        assertThat(request.headers().get(headerKey), hasItem(headerValue));
        assertThat(request.url(), containsString(path));
        assertThat(request.url(), equalTo(dynamicHostResolver.getHost()+path));
    }

}
