package org.easyframe.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Currency;

import org.easyframe.fastjson.parser.DefaultJSONParser;
import org.easyframe.fastjson.parser.JSONToken;
import org.easyframe.fastjson.parser.deserializer.ObjectDeserializer;

public class CurrencyCodec implements ObjectSerializer, ObjectDeserializer {

    public final static CurrencyCodec instance = new CurrencyCodec();

    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType) throws IOException {
        final SerializeWriter out = serializer.getWriter();
        if (object == null) {
            out.writeNull();
        } else {
            Currency currency = (Currency) object;
            out.writeString(currency.getCurrencyCode());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        String text = (String) parser.parse();

        if (text == null || text.length() == 0) {
            return null;
        }
        
        return (T) Currency.getInstance(text);
    }

    public int getFastMatchToken() {
        return JSONToken.LITERAL_STRING;
    }

}
