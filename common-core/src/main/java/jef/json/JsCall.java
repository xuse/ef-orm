package jef.json;

import java.util.Iterator;
import java.util.List;

public class JsCall implements JsCommand{
	private String name;
	private List<Object> args;
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append(name).append('(');
		Iterator<Object> iter=args.iterator();
		if(iter.hasNext()){
			sb.append(JsonUtil.toJsonScriptCode(iter.next()));
		}
		for(;iter.hasNext();){
			sb.append(',');
			sb.append(JsonUtil.toJsonScriptCode(iter.next()));
		}
		return sb.toString();
	}
}
