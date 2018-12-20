package br.org.abnerrolim.spring.feign.connector;

@FunctionalInterface
public interface DynamicHostResolver {
    String getHost();
}
