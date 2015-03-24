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

import java.util.HashMap;
import java.util.Map;
import jef.tools.StringUtils;

public final class MimeTypes {
	private static final Map<String,String> MIME_TYPES=new HashMap<String,String>();
	static{
		//Office
		MIME_TYPES.put("xls","application/msexcel");
		MIME_TYPES.put("xlsx","application/msexcel");
		MIME_TYPES.put("xla","application/msexcel");
		MIME_TYPES.put("hlp","application/mshelp");
		MIME_TYPES.put("chm","application/mshelp");
		MIME_TYPES.put("ppt","application/mspowerpoint");
		MIME_TYPES.put("pptx","application/mspowerpoint");
		MIME_TYPES.put("pps","application/mspowerpoint");
		MIME_TYPES.put("ppz","application/mspowerpoint");
		MIME_TYPES.put("pot","application/mspowerpoint");
		MIME_TYPES.put("doc","application/msword");
		MIME_TYPES.put("docx","application/msword");
		MIME_TYPES.put("dot","application/msword");
		MIME_TYPES.put("mdb","application/access");
		//Text
		MIME_TYPES.put("css","text/css");
		MIME_TYPES.put("htm","text/html");
		MIME_TYPES.put("html","text/html");
		MIME_TYPES.put("shtml","text/html");
		MIME_TYPES.put("js","text/javascript");
		MIME_TYPES.put("txt","text/plain");
		MIME_TYPES.put("c","text/plain");
		MIME_TYPES.put("cpp","text/plain");
		MIME_TYPES.put("java","text/plain");
		MIME_TYPES.put("log","text/plain");
		MIME_TYPES.put("conf","text/plain");
		MIME_TYPES.put("ini","text/plain");
		MIME_TYPES.put("inf","text/plain");
		MIME_TYPES.put("sql","text/plain");
		MIME_TYPES.put("pl","text/plain");
		MIME_TYPES.put("xml","text/xml");
		MIME_TYPES.put("dtd","text/xml");
		//Compress package
		MIME_TYPES.put("rar","application/x-rar-compressed");
		MIME_TYPES.put("cab","application/x-shockwave-flash");
		MIME_TYPES.put("jar","application/java-archive");
		MIME_TYPES.put("zip","application/zip");
		MIME_TYPES.put("gz","application/x-gzip");
		MIME_TYPES.put("tgz","application/x-tgz3");
		MIME_TYPES.put("tar","application/x-tar");
		MIME_TYPES.put("dvi","application/x-dvi");
		//Applications
		MIME_TYPES.put("rtf","application/rtf");
		MIME_TYPES.put("pdf","application/pdf");
		MIME_TYPES.put("exe","application/octet-stream");
		MIME_TYPES.put("pac","application/x-ns-proxy-autoconfig");
		MIME_TYPES.put("bz2","application/x-bzip");
		MIME_TYPES.put("torrent","application/x-bittorrent");
		//WebPages
		MIME_TYPES.put("php","application/x-httpd-php");
		MIME_TYPES.put("phtml","application/x-httpd-php");
		MIME_TYPES.put("swf","application/x-shockwave-flash");
		MIME_TYPES.put("asp","application/x-asap");
		MIME_TYPES.put("aspx","application/x-asap");
		//Image
		MIME_TYPES.put("gif","image/gif");
		MIME_TYPES.put("jpeg","image/jpeg");
		MIME_TYPES.put("jpg","image/jpeg");
		MIME_TYPES.put("jpe","image/jpeg");
		MIME_TYPES.put("png","image/png");
		MIME_TYPES.put("xwd","image/x-windowdump");
		//audio
		MIME_TYPES.put("au","audio/basic");
		MIME_TYPES.put("snd","audio/basic");
		MIME_TYPES.put("mp3","audio/mpeg");
		MIME_TYPES.put("wav","audio/x-wav");
		MIME_TYPES.put("m3u","audio/x-mpegurl");
		MIME_TYPES.put("wma","audio/x-ms-wma");
		MIME_TYPES.put("wxa","audio/x-ms-wax");
		MIME_TYPES.put("ogg","application/ogg");
		//video
		MIME_TYPES.put("avi","video/x-msvideo");
		MIME_TYPES.put("mpeg","video/mpeg");
		MIME_TYPES.put("mpe","video/mpeg");
		MIME_TYPES.put("mpg","video/mpeg");
		MIME_TYPES.put("rmvb","video/vnd.rn-realvideo");
		MIME_TYPES.put("rm","video/vnd.rn-realvideo");
		MIME_TYPES.put("qt","video/quicktime");
		MIME_TYPES.put("mov","video/quicktime");
		MIME_TYPES.put("asf","video/x-ms-asf");
		MIME_TYPES.put("asx","video/x-ms-asf");
		MIME_TYPES.put("wmv","video/x-ms-wmv");
		//Others
		MIME_TYPES.put("*","application/octet-stream");
	}
	
	public static String getByFileName(String fileName){
		return get(StringUtils.substringAfterLast(fileName,".").toLowerCase());
	}
	
	public static String get(String fileExtName){
		String mimeType=MIME_TYPES.get(fileExtName.toLowerCase());
		if(mimeType==null)mimeType=MIME_TYPES.get("*");
		return mimeType;
	}
	
	public static boolean contains(String extName){
		return MIME_TYPES.containsKey(extName);
	}
	
	public static Map<String,String> getAllTypes(){
		return MIME_TYPES;
	}
	
	public static boolean isValid(String mimeType){
		return MIME_TYPES.values().contains(StringUtils.lowerCase(mimeType));
	}
}
