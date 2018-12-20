package br.org.abnerrolim.spring.feign.connector.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

@Component
public class FeignJsonMapper {

    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String UTC = "UTC";
    private final ObjectMapper MAPPER;

    public FeignJsonMapper() {
        MAPPER = initializeWithDefaultFeatures();
    }

    public static ObjectMapper initializeWithDefaultFeatures() {

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .setDateFormat(new SimpleDateFormat(JSON_DATE_FORMAT))
                .setTimeZone(TimeZone.getTimeZone(UTC))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public FeignJsonMapper(ObjectMapper objectMapper) {
        MAPPER = objectMapper;
    }

    public ObjectMapper getMAPPER() {
        return MAPPER;
    }

    public <T> T read(Object json, Class<T> tClass) {
        return MAPPER.convertValue(json, tClass);
    }

    public <T> T read(String json, Class<T> tClass) {
        try {
            if (json == null || json.isEmpty()) {
                return null;
            }
            return MAPPER.readValue(json, tClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(InputStream stream, Class<T> tClass) {
        try {
            if (stream == null) {
                return null;
            }
            return MAPPER.readValue(stream, tClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T read(Reader reader, Class<T> tClass) {
        try {
            if (reader == null) {
                return null;
            }
            return MAPPER.readValue(reader, tClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> String write(T model) {
        try {
            if (model == null) {
                return null;
            }
            return MAPPER.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}
