package br.org.abnerrolim.spring.feign.connector;

import br.org.abnerrolim.spring.feign.connector.utils.FeignJsonMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;

public class ClientResponseTest {


    static FeignJsonMapper jsonMapper = new FeignJsonMapper();

    @Test
    public void createSuccessOf() {
        ObjectTest test = new ObjectTest();
        test.setAddress("Address");
        test.setName("Name");

        ClientResponse<ObjectTest> res = ClientResponse.successOf(test, 200);

        Assert.assertThat(res.isError(), is(false));
        Assert.assertThat(res.getHttpStatus().get(), equalTo(200));
        Assert.assertThat(res.get(), equalTo(test));
    }

    @Test
    public void shouldUnwrapSuccessfulResponse() throws IOException {
        ObjectTest expected = new ObjectTest();
        expected.setAddress("Address");
        expected.setName("Name");

        String serialized = Utils.getPayload("payload/objecttest-allfields.json");

        ObjectTest originalResponse = jsonMapper.read(serialized, ObjectTest.class);

        Assert.assertThat(originalResponse, notNullValue());
        Assert.assertThat(expected, equalTo(originalResponse));
    }


    @Test
    public void shouldWrapExtraFieldsAsMetadata() throws IOException {

        String serialized = Utils.getPayload("payload/errorresponse-with-extra-fields.json");

        ClientResponse response = jsonMapper.getMAPPER().readValue(serialized, ClientResponse.class);

        ErrorResponse error = response.getError();
        Assert.assertThat(error, notNullValue());
        Assert.assertThat(error.getCode(), equalTo("Code"));
        Assert.assertThat(error.getMessage(), equalTo("message message"));

        Assert.assertThat(error.metadata("mydata"), equalTo("my expression"));
        Assert.assertThat(error.metadata("myobject"), notNullValue());
    }


    @Test
    public void createErrorOf() {
        ErrorResponse clientResponseError = ErrorResponse.of("CODE", "MyPrefix");
        ClientResponse<ObjectTest> res = ClientResponse.errorOf(clientResponseError, 500);
        Assert.assertThat(res.isError(), is(true));
        Assert.assertThat(res.getError().getCode(), equalTo(clientResponseError.getCode()));
        Assert.assertThat(res.getError().getMessage(), equalTo(clientResponseError.getMessage()));
        Assert.assertThat(res.getError().getFields(), nullValue());
        Assert.assertThat(res.getHttpStatus().get(), equalTo(500));
    }


    @Test
    public void createErrorBadRequestOf() {
        ErrorResponse clientResponseError = ErrorResponse.of("CODE", "MyPrefix");
        ClientResponse<ObjectTest> res = ClientResponse.errorOf(clientResponseError, HttpStatus.BAD_REQUEST.value());
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
        ClientResponse<ObjectTest> res = ClientResponse.errorOf(clientResponseError, HttpStatus.NOT_FOUND.value());
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
            if(obj instanceof ObjectTest){
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
