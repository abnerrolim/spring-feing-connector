package br.org.abnerrolim.spring.feign.connector.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.vavr.Tuple;
import io.vavr.Tuple3;

import java.net.URL;
import java.util.*;

public class StringObfuscationUtils {

    private StringObfuscationUtils(){}

    public static String partialObfuscate(String raw){
        if(Objects.isNull(raw))
            return raw;
        Tuple3<String, String, Integer> segments = strategy(raw);
        return segments._1 + String.join("",Collections.nCopies(segments._3, "*")) + segments._2;
    }

    private static Tuple3<String, String, Integer> strategy(String raw){
        if(raw.length() > 7){
            String prefix = raw.substring(0, 2);
            String sufix = raw.substring((raw.length()-2), raw.length());
            return Tuple.of(prefix, sufix, (raw.length()-4));
        }else if(raw.length() > 2){
            String prefix = raw.substring(0, 2);
            String sufix = "";
            return Tuple.of(prefix, sufix, (raw.length()-2));
        }else{
            return Tuple.of(raw, "", 0);
        }
    }

    public static Collection<String> obfuscate(Collection<String> values){
        Collection<String> obfValues = new ArrayList<>(values.size());
        for(String value : values)
            obfValues.add(obfuscate(value));
        return obfValues;
    }

    public static String obfuscate(String value){
        int size = value != null && !value.isEmpty() ? value.length() : 0;
        return String.join("",Collections.nCopies(size, "*"));
    }

    public static JsonNode obfuscate(JsonNode jsonNode){
        if (jsonNode.isContainerNode()) {
            jsonNode = NullNode.getInstance();
        } else if (jsonNode.isNumber()) {
            jsonNode = new LongNode(0);
        } else if (jsonNode.isTextual()) {
            jsonNode = new TextNode(obfuscate(jsonNode.asText()));
        } else {
            jsonNode = NullNode.getInstance();
        }
        return jsonNode;
    }

    public static ObjectNode obfuscate(ObjectNode rootNode, String field){
        if(!rootNode.at(fieldToJsonPointer(field)).isMissingNode()) {
            JsonNode obfuscate;
            if (field.contains(".")) {
                String fieldValue = field.substring(field.indexOf(".") + 1, field.length());
                field = field.substring(0, field.indexOf("."));
                ObjectNode nd = rootNode.get(field).deepCopy();
                obfuscate = obfuscate(nd, fieldValue);
            } else {
                obfuscate = obfuscate(rootNode.get(field));
            }
            ObjectNode obj = rootNode.deepCopy();
            obj.replace(field, obfuscate);
            return obj;
        }
        return rootNode;
    }


    private static String fieldToJsonPointer(String field){
        String path = "/".concat(field);
        if(path.contains("."))
            path = path.replaceAll("\\.","/");
        return path;
    }

    public static JsonNode obfuscate(JsonNode jsonNode, List<String> fieldsToObfuscate){
        if(fieldsToObfuscate == null || fieldsToObfuscate.isEmpty() || !jsonNode.isObject())
            return jsonNode;
        ObjectNode node = jsonNode.deepCopy();
        for(String st : fieldsToObfuscate){
            node = obfuscate(node, st);
        }
        return node;
    }

    public static String obfuscate(URL url, List<Integer> pathSegmentsToObfuscate){
        StringBuffer strBuff = new StringBuffer(url.getProtocol()).append("://").append(url.getAuthority());
        String path = url.getPath();
        String[] rawSegments = path.split("/");
        String segmentValue = "";
        for (int i = 0; i < rawSegments.length; i++) {
            segmentValue = rawSegments[i];
            if(pathSegmentsToObfuscate.contains(i)){
                segmentValue = StringObfuscationUtils.obfuscate(segmentValue);
            }
            strBuff.append("/").append(segmentValue);
        }
        return strBuff.toString();
    }
}
