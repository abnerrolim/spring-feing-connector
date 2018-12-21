package br.org.abnerrolim.spring.feign.connector;

import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

public class ClientResponseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    static FeignJsonMapper jsonMapper = new FeignJsonMapper();

    private <T> T returnT(T tt) {
        return tt;
    }
    private <T> T throwClientResponseException(T tt, String message, ErrorResponse error, int http){
        throw  new ClientResponseErrorDecoder.ClientResponseException(message, error, http);
    }

    @Test
    public void createSuccessOf() {
        ObjectTest test = new ObjectTest();
        test.setAddress("Address");
        test.setName("Name");


        ClientResponse<ObjectTest> res = ClientResponse.of(() -> returnT(test));

        Assert.assertThat(res.isError(), is(false));
        Assert.assertThat(res.get(), equalTo(test));
        thrown.expect(NoSuchElementException.class);
        res.getError();
        Assert.assertTrue("Fold should return success response", res.fold(errorResponse -> false, success -> true));
    }

    @Test
    public void createErrorOf() throws IOException {
        ObjectTest expected = new ObjectTest();
        expected.setAddress("Address");
        expected.setName("Name");

        ErrorResponse error = ErrorResponse.of("CODE", "ERROR");
        ClientResponse<ObjectTest> res = ClientResponse.of(() -> throwClientResponseException(expected, "Message", error, 500));

        Assert.assertThat(res.isError(), is(true));
        Assert.assertThat(res.getError(), equalTo(error));
        thrown.expect(NoSuchElementException.class);
        res.get();
        Assert.assertTrue("Fold should return error response", res.fold(errorResponse -> true, success -> false));
    }

    @Test
    public void shouldWrapExtraFieldsAsMetadata() throws IOException {

        String serialized = Utils.getPayload("payload/errorresponse-with-extra-fields.json");

        ErrorResponse response = jsonMapper.getMAPPER().readValue(serialized, ErrorResponse.class);

        ClientResponse<String> res = ClientResponse.of(() -> throwClientResponseException("", "Message", response, 500));

        ErrorResponse error = res.getError();
        Assert.assertThat(error, notNullValue());
        Assert.assertThat(error.getCode(), equalTo("Code"));
        Assert.assertThat(error.getMessage(), equalTo("message message"));

        Assert.assertThat(error.metadata("mydata"), equalTo("my expression"));
        Assert.assertThat(error.metadata("myobject"), notNullValue());
    }

    @Test
    public void createErrorBadRequestOf() {
        ErrorResponse clientResponseError = ErrorResponse.of("CODE", "MyPrefix");

        ClientResponse<String> res = ClientResponse.of(() -> throwClientResponseException("", "Message", clientResponseError, HttpStatus.BAD_REQUEST.value()));
        Assert.assertThat(res.isError(), is(true));
        Assert.assertThat(res.isBadRequest(), is(true));
        Assert.assertThat(res.isNotFound(), is(false));
        Assert.assertThat(res.getError().getCode(), equalTo(clientResponseError.getCode()));
        Assert.assertThat(res.getError().getMessage(), equalTo(clientResponseError.getMessage()));
        Assert.assertThat(res.getError().getFields(), nullValue());
        Assert.assertThat(res.getHttpStatus().get(), equalTo(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void createErrorNotFoundOf() {
        ErrorResponse clientResponseError = ErrorResponse.of("CODE", "MyPrefix");

        ClientResponse<String> res = ClientResponse.of(() -> throwClientResponseException("", "Message", clientResponseError, HttpStatus.NOT_FOUND.value()));
        Assert.assertThat(res.isError(), is(true));
        Assert.assertThat(res.isBadRequest(), is(false));
        Assert.assertThat(res.isNotFound(), is(true));
        Assert.assertThat(res.getError().getCode(), equalTo(clientResponseError.getCode()));
        Assert.assertThat(res.getError().getMessage(), equalTo(clientResponseError.getMessage()));
        Assert.assertThat(res.getError().getFields(), nullValue());
        Assert.assertThat(res.getHttpStatus().get(), equalTo(HttpStatus.NOT_FOUND.value()));
    }

    public static class ObjectTest {
        private String name;
        private String address;

        public ObjectTest() {
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return this.address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        @Override
        public boolean equals(Object obj) {
            boolean equals = false;
            if (obj instanceof ObjectTest) {
                ObjectTest objTst = (ObjectTest) obj;
                equals = objTst.name == null ? this.name == null : objTst.name.equals(this.name);
                equals = equals ? objTst.address == null ? this.address == null : objTst.address.equals(this.address) : equals;
            }
            return equals;
        }
    }

    public static class MyObject {
        private String name;
        private String age;
    }

}
