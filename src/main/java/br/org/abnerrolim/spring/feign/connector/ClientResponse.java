package br.org.abnerrolim.spring.feign.connector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.springframework.http.HttpStatus;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientResponse<T> {
    @JsonUnwrapped
    private T response;
    @JsonUnwrapped
    private ErrorResponse error;
    @JsonIgnore
    private Integer httpStatus;
    @JsonIgnore
    private boolean isError = false;

    private ClientResponse(ErrorResponse error, T response, Integer httpStatus) {
        this.error = error;
        this.response = response;
        this.httpStatus = httpStatus;
    }

    private ClientResponse(ErrorResponse error, T response, Integer httpStatus, boolean isError) {
        this.error = error;
        this.response = response;
        this.httpStatus = httpStatus;
        this.isError = isError;
    }

    ClientResponse() {
    }

    static <T> ClientResponse<T> successOf(T resp, Integer httpStatus) {
        return new ClientResponse<>(null, resp, httpStatus);
    }

    public static <T> ClientResponse<T> successOf(T resp) {
        return successOf(resp, null);
    }

    public static <T> ClientResponse<T> errorOf(ErrorResponse errorMessageResponse) {
        return new ClientResponse<>(errorMessageResponse, null, null, true);
    }

    public static <T> ClientResponse<T> errorOf(ErrorResponse errorMessageResponse, Integer httpStatus) {
        return new ClientResponse<>(errorMessageResponse, null, httpStatus, true);
    }

    static <P> ClientResponse<P> cloneErrorTo(ClientResponse origin, Class<P> of) {
        return new ClientResponse<>(origin.error, null, origin.httpStatus, true);
    }

    void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
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
        return error;
    }

    public T get() {
        if (isError())
            throw new IllegalStateException("Is error response, can't convert to expected value!");
        return response;
    }

    public <U> U fold(Function<? super ErrorResponse, ? extends U> leftMapper, Function<? super T, ? extends U> rightMapper) {
        Objects.requireNonNull(leftMapper, "leftMapper is null");
        Objects.requireNonNull(rightMapper, "rightMapper is null");
        if (isError()) {
            return leftMapper.apply(getError());
        } else {
            return rightMapper.apply(get());
        }
    }
}
