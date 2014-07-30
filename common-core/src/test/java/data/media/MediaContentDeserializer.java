package data.media;

import java.lang.reflect.Type;

import org.easyframe.fastjson.parser.DefaultJSONParser;
import org.easyframe.fastjson.parser.JSONScanner;
import org.easyframe.fastjson.parser.JSONToken;
import org.easyframe.fastjson.parser.deserializer.ObjectDeserializer;

public class MediaContentDeserializer implements ObjectDeserializer {
    private ObjectDeserializer mediaDeserializer;
    private ObjectDeserializer imageDeserializer;
    private final char[] mediaPrefix = "\"media\":".toCharArray();
    private final char[] imagePrefix = "\"images\":".toCharArray();

    public <T> T deserialze(DefaultJSONParser parser, Type clazz, Object fieldName) {
        final JSONScanner lexer = (JSONScanner) parser.getLexer();
        
        MediaContent object = new MediaContent();
        
        lexer.matchField(mediaPrefix);
        if (mediaDeserializer == null) {
            //mediaDeserializer = parser.getMapping().getDeserializer(ObjectDeserializer.class);
        }
        mediaDeserializer.deserialze(parser, clazz, null);
        
        lexer.matchField(imagePrefix);
        imageDeserializer.deserialze(parser, clazz, null);
        
//        if (lexer.token() != JSONToken.RBRACE)
        
        // TODO Auto-generated method stub
        return null;
    }
    

    public int getFastMatchToken() {
        return JSONToken.LBRACE;
    }

}
