package br.org.abnerrolim.spring.feign.connector;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;

public class Utils {

    private Utils(){}

    public static String getPayload(String filePath) throws IOException {
        return IOUtils.toString(Utils.class.getClassLoader().getResource(filePath), Charset.defaultCharset());
    }
}
