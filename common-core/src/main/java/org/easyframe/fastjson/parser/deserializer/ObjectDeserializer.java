package org.easyframe.fastjson.parser.deserializer;

import java.lang.reflect.Type;

import org.easyframe.fastjson.parser.DefaultJSONParser;

public interface ObjectDeserializer {
    <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName);
    
    int getFastMatchToken();
}
