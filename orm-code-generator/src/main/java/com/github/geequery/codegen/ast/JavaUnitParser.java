package com.github.geequery.codegen.ast;

import java.io.File;

public interface JavaUnitParser {
	JavaUnit parse(File file,String charset);
}
