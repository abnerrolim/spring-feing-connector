package br.org.abnerrolim.spring.feign.connector;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;

import java.net.URI;

public class DynamicTarget<T> implements Target<T> {


    private final DynamicHostResolver dynamicHostResolver;
    private final Class<T> type;
    private final String name;
    private final String url;

    protected DynamicTarget(Class<T> type, String name, String url, DynamicHostResolver dynamicHostResolver) {
        this.type = type;
        this.url = url;
        this.name = name;
        this.dynamicHostResolver = dynamicHostResolver;
    }

    public static <T> DynamicTarget<T> create(Class<T> type, String url, DynamicHostResolver dynamicHostResolver) {
        URI asUri = URI.create(url);
        return new DynamicTarget(type, asUri.getHost(), (asUri.getScheme() + asUri.getPath()), dynamicHostResolver);
    }

    @Override
    public Request apply(RequestTemplate input) {
        URI asUri = URI.create(input.url());
        String onlyPath = asUri.getPath();
        String finalUrl = dynamicHostResolver.getHost() + onlyPath;
        return alterUrl(input, finalUrl);
    }

    private Request alterUrl(RequestTemplate request, String newUrl) {
        return Request.create(
                request.method(),
                newUrl,
                request.headers(),
                request.body(),
                request.charset()
        );
    }

    public Class<T> type() {
        return this.type;
    }

    public String name() {
        return this.name;
    }

    public String url() {
        return this.url;
    }

}
