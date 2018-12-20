package br.org.abnerrolim.spring.feign.connector;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final Map<String, List<String>> fields;
    private final String code;
    private final String message;

    private Map<String, Object> metadata = new HashMap<>();

    @JsonCreator
    ErrorResponse(@JsonProperty("fields") Map<String, List<String>> fields, @JsonProperty("code") String code, @JsonProperty("message") String message) {
        this.fields = fields;
        this.code = code;
        this.message = message;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(null, code, message);
    }

    public static ErrorResponse of(Map<String, List<String>> fields) {
        return new ErrorResponse(fields, null, null);
    }

    @JsonAnySetter
    public void metadata(String name, Object value) {
        metadata.put(name, value);
    }

    public Object metadata(String name) {
        return metadata.get(name);
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    void metadata(Map<String,Object> metadata){
        this.metadata = metadata;
    }

    public String getCode(){
        return this.code;
    }

    public String getMessage(){
        return this.message;
    }

    public Map<String, List<String>> getFields() {
        return this.fields;
    }

    public static ErrorResponse.ErrorResponseBuilder builder() {
        return new ErrorResponse.ErrorResponseBuilder();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof ErrorResponse)) {
            return false;
        } else {
            ErrorResponse other = (ErrorResponse)o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                label59: {
                    Object this$fields = this.getFields();
                    Object other$fields = other.getFields();
                    if (this$fields == null) {
                        if (other$fields == null) {
                            break label59;
                        }
                    } else if (this$fields.equals(other$fields)) {
                        break label59;
                    }

                    return false;
                }

                Object this$code = this.getCode();
                Object other$code = other.getCode();
                if (this$code == null) {
                    if (other$code != null) {
                        return false;
                    }
                } else if (!this$code.equals(other$code)) {
                    return false;
                }

                Object this$message = this.getMessage();
                Object other$message = other.getMessage();
                if (this$message == null) {
                    if (other$message != null) {
                        return false;
                    }
                } else if (!this$message.equals(other$message)) {
                    return false;
                }

                Object this$metadata = this.metadata;
                Object other$metadata = other.metadata;
                if (this$metadata == null) {
                    if (other$metadata != null) {
                        return false;
                    }
                } else if (!this$metadata.equals(other$metadata)) {
                    return false;
                }

                return true;
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof ErrorResponse;
    }

    public int hashCode() {
        int result = 1;
        Object $fields = this.getFields();
        result = result * 59 + ($fields == null ? 43 : $fields.hashCode());
        Object $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        Object $message = this.getMessage();
        result = result * 59 + ($message == null ? 43 : $message.hashCode());
        Object $metadata = this.metadata;
        result = result * 59 + ($metadata == null ? 43 : $metadata.hashCode());
        return result;
    }

    public String toString() {
        return "ErrorResponse(fields=" + this.getFields() + ", code=" + this.getCode() + ", message=" + this.getMessage() + ", metadata=" + this.metadata + ")";
    }
    public static class ErrorResponseBuilder {
        private Map<String, List<String>> fields;
        private String code;
        private String message;

        ErrorResponseBuilder() {
        }

        public ErrorResponse.ErrorResponseBuilder fields(Map<String, List<String>> fields) {
            this.fields = fields;
            return this;
        }

        public ErrorResponse.ErrorResponseBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ErrorResponse.ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponse build() {
            if(fields != null && !fields.isEmpty())
                return ErrorResponse.of(fields);
            else
                return ErrorResponse.of(code, message);
        }

        public String toString() {
            return "ErrorResponse.ErrorResponseBuilder(fields=" + this.fields + ", code=" + this.code + ", message=" + this.message + ")";
        }
    }
}
