package br.org.abnerrolim.spring.feign.connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.http.HttpStatus;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ClientResponse<T> {

    private T response;
    private ErrorResponse error;
    private Integer httpStatus;
    private boolean isError = false;

    private ClientResponse(ErrorResponse error, T response, Integer httpStatus, boolean isError) {
        this.error = error;
        this.response = response;
        this.httpStatus = httpStatus;
        this.isError = isError;
    }

    ClientResponse() {
    }

    private static <T> ClientResponse<T> successFull(T response){
        return new ClientResponse<>(null, response, null, false);
    }
    private static <T> ClientResponse<T> error(ErrorResponse error, int httpStatus){
        return new ClientResponse<>(error, null, httpStatus, true);
    }

    static <T> ClientResponse<T> of(Supplier<T> feingCall) {
        try{
           return successFull(feingCall.get());
        }catch (ClientResponseErrorDecoder.ClientResponseException e){
            return error(e.response, e.httpStatusResponse);
        }
    }

    @JsonIgnore
    public boolean isError() {
        return this.isError;
    }

    @JsonIgnore
    public boolean isBadRequest() {
        return isError() &&
                sameStatus(HttpStatus.BAD_REQUEST);
    }

    @JsonIgnore
    public boolean isNotFound() {
        return isError() &&
                sameStatus(HttpStatus.NOT_FOUND);
    }

    private boolean sameStatus(HttpStatus toCompare) {
        return this.httpStatus != null &&
                this.httpStatus.equals(toCompare.value());
    }

    @JsonIgnore
    public Optional<Integer> getHttpStatus() {
        return Optional.ofNullable(this.httpStatus);
    }


    public ErrorResponse getError() {
        if(!isError)
            throw new NoSuchElementException("get error on success");
        return error;
    }

    public T get() {
        if (isError())
            throw new NoSuchElementException("get value on error");
        return response;
    }

    public <U> U fold(Function<? super ErrorResponse, ? extends U> errorMapper, Function<? super T, ? extends U> successMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper is null");
        Objects.requireNonNull(successMapper, "successMapper is null");
        if (isError()) {
            return errorMapper.apply(getError());
        } else {
            return successMapper.apply(get());
        }
    }
}
