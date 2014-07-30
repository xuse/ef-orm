package jef.json;

import java.io.Serializable;

import jef.tools.StringUtils;


public class JSFunction implements Serializable,JScriptExpression{
	private String name;
	private String[] args;
	private String code;
	private boolean raw;
	public JSFunction(){
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String[] getArgs() {
		return args;
	}
	public JSFunction setArgs(String... args) {
		this.args = args;
		return this;
	}
	public String getCode() {
		return code;
	}

	public JSFunction addCodeLine(String line){
		if(StringUtils.isEmpty(code)){
			code=line;
		}else{
			code+=code+"\r\n"+line;
		}
		return this;
	}
	
	public JSFunction setCode(String code) {
		this.code = code;
		return this;
	}
	@Override
	public String toString() {
		if(raw){
			return code;
		}else{
			StringBuilder sb=new StringBuilder("function");
			if(name!=null && name.length()>0){
				sb.append(' ').append(name);
			}
			sb.append('(');
			if(args!=null){
				for(int i=0;i<args.length;i++){
					if(i>0){
						sb.append(',');
					}
					sb.append(args[i]);
				}
			}
			sb.append(')');
			sb.append("{");
			if(code!=null){
				sb.append('\n').append(code);
			}
			sb.append("}");
			return sb.toString();
		}
	}
	public boolean isRaw() {
		return raw;
	}
	public void setRaw(boolean raw) {
		this.raw = raw;
	}
}
