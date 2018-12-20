package br.org.abnerrolim.spring.feign.connector;

import br.org.abnerrolim.spring.feign.connector.obfuscate.HttpObfuscator;
import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Request;
import feign.RequestInterceptor;
import feign.httpclient.ApacheHttpClient;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class FeignConnectorConfigHelper {

    private Integer connectTimeout;
    private Integer readTimeout;
    private String moduleName;
    private String errorPrefix;

    public FeignConnectorConfigHelper(@Value("${feign.connect.timeout}") Integer connectTimeout,
                                      @Value("${feign.read.timeout}") Integer readTimeout,
                                      @Value("${realwave.feing.module.client.name}") String moduleName,
                                      @Value("${realwave.feing.module.client.error.prefix}") String errorPrefix){
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.moduleName = moduleName;
        this.errorPrefix = errorPrefix;
    }

    public Config config(ObjectMapper objectMapper){
        return new Config(objectMapper);
    }

    public Config config(){
        return new Config();
    }

    public class Config{
        private Feign.Builder feignBuilder;
        private FeignJsonMapper feignJsonMapper;

        private Config(){
            this.feignJsonMapper = new FeignJsonMapper();
            minimal();
        }
        private Config(ObjectMapper objectMapper){
            this.feignJsonMapper = new FeignJsonMapper(objectMapper);
            minimal();
        }
        private void minimal(){
            feignBuilder = Feign.builder()
                    .encoder(new JacksonEncoder(feignJsonMapper.getMAPPER()))
                    .decoder(new ClientResponseDecoder(new JacksonDecoder(feignJsonMapper.getMAPPER()),moduleName))
                    .errorDecoder(new ClientResponseErrorDecoder(feignJsonMapper, errorPrefix, moduleName))
                    .client(new ApacheHttpClient())
                    .options(requestOptions());
        }

        public Config withObfuscator(HttpObfuscator httpObfuscator){
            feignBuilder
                    .decoder(new ClientResponseDecoder(new JacksonDecoder(feignJsonMapper.getMAPPER()), httpObfuscator, moduleName))
                    .errorDecoder(new ClientResponseErrorDecoder(feignJsonMapper, httpObfuscator, errorPrefix, moduleName));
            return this;
        }

        public Config withSpringContract(){
            feignBuilder
                    .contract(new SpringMvcContract());
            return this;
        }

        public Config withRequestInterceptor(RequestInterceptor... requestInterceptors){
            for(RequestInterceptor requestInterceptor : requestInterceptors)
                feignBuilder.requestInterceptor(requestInterceptor);
            return this;
        }
        public Feign.Builder feignBuilder(){
            return this.feignBuilder;
        }
        public <T> T buildSimpleClient(Class<T> clazz, String url){
            return this.feignBuilder.target(clazz, url);
        }
        public <T> T buildDynamicHostClient(Class<T> clazz, DynamicHostResolver dynamicHostResolver){
            return this.feignBuilder.target(DynamicTarget.create(clazz, "http://localhost", dynamicHostResolver));
        }
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(connectTimeout, readTimeout);
    }

}
