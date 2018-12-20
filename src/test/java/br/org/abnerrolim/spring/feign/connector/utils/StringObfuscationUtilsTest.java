package br.org.abnerrolim.spring.feign.connector.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class StringObfuscationUtilsTest {

    FeignJsonMapper feignJsonMapper = new FeignJsonMapper();

    @Test
    public void shouldObfuscateBiggerString() throws Exception{
        String raw = "12-this-part-will-be-hide-34";
        String expected = "12************************34";
        String returned = StringObfuscationUtils.partialObfuscate(raw);
        Assert.assertEquals(expected, returned);
    }

    @Test
    public void shouldObfuscateMediumString() throws Exception{
        String raw = "12-hide";
        String expected = "12*****";
        String returned = StringObfuscationUtils.partialObfuscate(raw);
        Assert.assertEquals(expected, returned);
    }

    @Test
    public void shouldNotObfuscateVerySmallString() throws Exception{
        String raw = "12";
        String expected = "12";
        String returned = StringObfuscationUtils.partialObfuscate(raw);
        Assert.assertEquals(expected, returned);

        raw = "1";
        expected = "1";
        returned = StringObfuscationUtils.partialObfuscate(raw);
        Assert.assertEquals(expected, returned);

        raw = "";
        expected = "";
        returned = StringObfuscationUtils.partialObfuscate(raw);
        Assert.assertEquals(expected, returned);
    }

    @Test
    public void obfuscateObjectJsonNodeWithFieldsSelection(){
        ObjectToObfuscate test = new ObjectToObfuscate();
        String body = feignJsonMapper.write(test);
        JsonNode node = feignJsonMapper.read(body, JsonNode.class);

        List<String> fields = new ArrayList<>();
        fields.add("name");
        fields.add("age");
        fields.add("collect");
        fields.add("insideObject.insideName");
        fields.add("insideObject.insideInsideObject.deepNameHideMe");
        fields.add("insideObject.insideInsideObject.address");
        fields.add("notexist");
        JsonNode obfNode = StringObfuscationUtils.obfuscate(node, fields);

        ObjectToObfuscate obfuscated = feignJsonMapper.read(obfNode, ObjectToObfuscate.class);
        assertThat(obfuscated.name, equalTo("***********"));
        assertThat(obfuscated.age, equalTo(0));
        assertThat(obfuscated.collect, nullValue());
        assertThat(obfuscated.insideObject, notNullValue());
        assertThat(obfuscated.insideObject.insideName, equalTo("**********"));
        assertThat(obfuscated.insideObject.address, equalTo(test.insideObject.address));
        assertThat(obfuscated.insideObject.insideInsideObject, notNullValue());
        assertThat(obfuscated.insideObject.insideInsideObject.deepNameHideMe,  equalTo("**************"));
        assertThat(obfuscated.insideObject.insideInsideObject.address,  equalTo("******"));
    }
    @Test
    public void shouldNotObfuscateWhenJsonNodeIsNotObjectWithFieldsSelection(){
        JsonNode node = TextNode.valueOf("What?");
        List<String> fields = new ArrayList<>();
        fields.add("name");
        fields.add("age");
        JsonNode obfNode = StringObfuscationUtils.obfuscate(node, fields);
        assertThat(obfNode, notNullValue());
        assertThat(obfNode.asText(), equalTo(node.asText()));
    }

    @Test
    public void obfuscateAObjectNodeShouldResultNullNode(){
        ObjectToObfuscate test = new ObjectToObfuscate();
        String body = feignJsonMapper.write(test);
        JsonNode node = feignJsonMapper.read(body, JsonNode.class);
        JsonNode obfNode = StringObfuscationUtils.obfuscate(node);
        assertThat(obfNode, notNullValue());
        assertThat(obfNode.isNull(), is(true));
    }

    @Test
    public void obfuscateAnArrayNodeShouldResultNullNode(){
        List<String> test = Arrays.asList("test", "one", "two");
        String body = feignJsonMapper.write(test);
        JsonNode node = feignJsonMapper.read(body, JsonNode.class);
        JsonNode obfNode = StringObfuscationUtils.obfuscate(node);
        assertThat(obfNode, notNullValue());
        assertThat(obfNode.isNull(), is(true));
    }

    @Test
    public void obfuscateAnNumberNodeShouldResultNullNode(){
        JsonNode node = IntNode.valueOf(10);
        JsonNode obfNode = StringObfuscationUtils.obfuscate(node);
        assertThat(obfNode, notNullValue());
        assertThat(obfNode.asInt(), is(0));
        node = DecimalNode.valueOf(new BigDecimal(10.12));
        obfNode = StringObfuscationUtils.obfuscate(node);
        assertThat(obfNode.asInt(), is(0));
    }


    @Test
    public void shouldDoNothingWithFieldsEmptyList(){
        ObjectToObfuscate test = new ObjectToObfuscate();
        String body = feignJsonMapper.write(test);
        JsonNode node = feignJsonMapper.read(body, JsonNode.class);

        List<String> fields = Collections.emptyList();
        JsonNode obfNode = StringObfuscationUtils.obfuscate(node, fields);

        ObjectToObfuscate obfuscated = feignJsonMapper.read(obfNode, ObjectToObfuscate.class);
        assertThat(obfuscated.name, equalTo(test.name));
        assertThat(obfuscated.age, equalTo(test.age));
        assertThat(obfuscated.collect.iterator().next(), equalTo(test.collect.iterator().next()));
        assertThat(obfuscated.insideObject.insideName, equalTo(test.insideObject.insideName));
        assertThat(obfuscated.insideObject.address, equalTo(test.insideObject.address));
        assertThat(obfuscated.insideObject.insideInsideObject.address, equalTo(test.insideObject.insideInsideObject.address));
        assertThat(obfuscated.insideObject.insideInsideObject.deepNameHideMe, equalTo(test.insideObject.insideInsideObject.deepNameHideMe));
    }
    @Test
    public void shouldDoNothingWithFieldsNullList(){
        ObjectToObfuscate test = new ObjectToObfuscate();
        String body = feignJsonMapper.write(test);
        JsonNode node = feignJsonMapper.read(body, JsonNode.class);

        List<String> fields = null;
        JsonNode obfNode = StringObfuscationUtils.obfuscate(node, fields);

        ObjectToObfuscate obfuscated = feignJsonMapper.read(obfNode, ObjectToObfuscate.class);
        assertThat(obfuscated.name, equalTo(test.name));
        assertThat(obfuscated.age, equalTo(test.age));
        assertThat(obfuscated.collect.iterator().next(), equalTo(test.collect.iterator().next()));
        assertThat(obfuscated.insideObject.insideName, equalTo(test.insideObject.insideName));
        assertThat(obfuscated.insideObject.address, equalTo(test.insideObject.address));
        assertThat(obfuscated.insideObject.insideInsideObject.address, equalTo(test.insideObject.insideInsideObject.address));
        assertThat(obfuscated.insideObject.insideInsideObject.deepNameHideMe, equalTo(test.insideObject.insideInsideObject.deepNameHideMe));
    }

    @Test
    public void obfuscateUrl() throws Exception{
        String url1 = "http://wwww.mysecrets.com:900/v1/store/store1/customer/3232323/token/1223daeDEAed/";
        List<Integer> segmentsToHid = Arrays.asList(5,7);
        String obfuscated = StringObfuscationUtils.obfuscate(new URL(url1), segmentsToHid);
        assertThat(obfuscated, equalTo("http://wwww.mysecrets.com:900//v1/store/store1/customer/*******/token/************"));
    }

    public static class ObjectToObfuscate{
        public String name = "Secret Name";
        public Integer age = 10;
        public Collection<String> collect = Arrays.asList("Test", "Ahow", "Deiad");
        public InsideObject insideObject = new InsideObject();
    }
    public static class InsideObject{
        public String insideName = "InsideName";
        public String address = "Non Secret";
        public InsideInsideObject insideInsideObject = new InsideInsideObject();
    }
    public static class InsideInsideObject{
        public String deepNameHideMe = "deepNameHideMe";
        public String address = "Secret";
    }
}
