package jef.tools;

import java.io.IOException;

import org.w3c.dom.DocumentFragment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public interface IDOMFragmentParser {
	void parse(String systemId, DocumentFragment fragment)
			throws SAXException, IOException;
	
    void parse(InputSource source, DocumentFragment fragment) 
            throws SAXException, IOException;
}
