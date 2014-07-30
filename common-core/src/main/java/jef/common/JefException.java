/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.common;

/**
 * 自定义的异常类
 * @author Administrator
 *
 */
public final class JefException extends Exception{

	private static final long serialVersionUID = 1L;
	
	public JefException(String string) {
		super(string);
	}
	public JefException(){
		super();
	}
    public JefException(String message, Throwable cause) {
        super(message, cause);
    }
    public JefException(Throwable cause) {
        super(cause);
    }
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
    
}
