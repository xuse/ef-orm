package jef.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.Test;

import jef.common.log.LogUtil;
import jef.tools.reflect.BeanUtils;
import jef.tools.support.JefBase64;

public class IOUtilsTest extends org.junit.Assert{
	public static void main(String[] args) {
		String n1 = ".sjfdnsdj";
		String n2 = "asdas.yxy.txt";
		String n3 = "fsdfs.TXT";
		String n4 = "sdfmwsjfldsfds";
		FileName f = new FileName(n1);
		System.out.println(f.getMain());
		System.out.println(f.getExt());

		f = new FileName(n2);
		f.append("(part2)");
		System.out.println(f.getMain());

		f = new FileName(n3);
		System.out.println(f.getMain());
		System.out.println(f.getExt());

		f = new FileName(n4);
		System.out.println(f.getMain());
		System.out.println(f.getExt());
		URL u = Thread.currentThread().getContextClassLoader().getResource("");
		System.out.println(u);

	}
	
	@Test
	@Ignore
	public void testChannelClose() throws IOException{
		File source=new File("c:/bootmgr");
		File newFile=new File("test.txt");
		FileInputStream fin=new FileInputStream(source);
		FileChannel in = fin.getChannel();
		FileOutputStream fout=new FileOutputStream(newFile);
		FileChannel out = fout.getChannel();
		in.transferTo(0, source.length(), out);
		in.close();
		System.out.println("===============1================");
		System.out.println(in.isOpen());
		System.out.println(BeanUtils.getFieldValue(fin, "closed"));
		System.out.println(BeanUtils.getFieldValue(fout, "closed"));
		out.close();
		System.out.println("===============2================");
		System.out.println(out.isOpen());
		System.out.println(BeanUtils.getFieldValue(fin, "closed"));
		System.out.println(BeanUtils.getFieldValue(fout, "closed"));
		
	}

	@Test
	public void test1() throws IOException {
		Map<String, String> o = IOUtils.loadProperties(this.getClass().getResource("/a.properties"),null);
		LogUtil.show(o);
		System.out.println("===== output =====");
		IOUtils.storeProperties(new PrintWriter(System.out), o, true,null,0);
	}
	
	
	@Test
	public void test2() throws IOException {
		
		Properties pp=new Properties();
		pp.load(this.getClass().getResource("/a.properties").openStream());
		
		LogUtil.show(pp);
	}


	@Test
	public void testExtract() throws IOException {
		String s = "yv66vgAAADIA0AcAAgEAMmNvbS9oaWt2aXNpb24vc2VjdXJpdHkvcGF0Y2gvdXRpbC9BRVNFbmNyeXB0b3JVdGlsBwAEAQAQamF2YS9sYW5nL09iamVjdAcABgEALGNvbS9oaWt2aXNpb24vc2VjdXJpdHkvcGF0Y2gvdXRpbC9JSUlJSUlsbElJAQAGbG9nZ2VyAQAaTGphdmEvdXRpbC9sb2dnaW5nL0xvZ2dlcjsBAAlBTEdPUklUSE0BABJMamF2YS9sYW5nL1N0cmluZzsBAA1Db25zdGFudFZhbHVlCAANAQADQUVTAQAHQ0hBUlNFVAgAEAEABXV0Zi04AQAIPGNsaW5pdD4BAAMoKVYBAARDb2RlCAAVAQAEVXRpbAoAFwAZBwAYAQAYamF2YS91dGlsL2xvZ2dpbmcvTG9nZ2VyDAAaABsBAAlnZXRMb2dnZXIBAC4oTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL3V0aWwvbG9nZ2luZy9Mb2dnZXI7CQABAB0MAAcACAEAD0xpbmVOdW1iZXJUYWJsZQEAEkxvY2FsVmFyaWFibGVUYWJsZQEABjxpbml0PgoAAwAiDAAgABIBAAR0aGlzAQA0TGNvbS9oaWt2aXNpb24vc2VjdXJpdHkvcGF0Y2gvdXRpbC9BRVNFbmNyeXB0b3JVdGlsOwEAFWdldERlZmF1bHRFbmNyeXB0U2VlZAEABCgpW0IIACgBABAvU3RyaW5nVXRpbHMudHh0CgAqACwHACsBAA9qYXZhL2xhbmcvQ2xhc3MMAC0ALgEAE2dldFJlc291cmNlQXNTdHJlYW0BACkoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2lvL0lucHV0U3RyZWFtOwoAMAAyBwAxAQATamF2YS9pby9JbnB1dFN0cmVhbQwAMwA0AQAEcmVhZAEABShbQilJCgAwADYMADMANwEAByhbQklJKUkIADkBACZnZXREZWZhdWx0U2VlZCBmYWlsZWQsIGJlY2F1c2Ugb2Y6WyVzXQoAOwA9BwA8AQATamF2YS9pby9JT0V4Y2VwdGlvbgwAPgA/AQAKZ2V0TWVzc2FnZQEAFCgpTGphdmEvbGFuZy9TdHJpbmc7CgBBAEMHAEIBABBqYXZhL2xhbmcvU3RyaW5nDABEAEUBAAZmb3JtYXQBADkoTGphdmEvbGFuZy9TdHJpbmc7W0xqYXZhL2xhbmcvT2JqZWN0OylMamF2YS9sYW5nL1N0cmluZzsKABcARwwASABJAQAGc2V2ZXJlAQAVKExqYXZhL2xhbmcvU3RyaW5nOylWCgBLAE0HAEwBACljb20vaGlrdmlzaW9uL3NlY3VyaXR5L3BhdGNoL3V0aWwvSU9VdGlscwwATgBPAQAKY2xvc2VRdWlldAEAFihMamF2YS9pby9DbG9zZWFibGU7KVYBAARzZWVkAQACW0IBAAJpcwEAFUxqYXZhL2lvL0lucHV0U3RyZWFtOwEAAWUBABVMamF2YS9pby9JT0V4Y2VwdGlvbjsBAA1TdGFja01hcFRhYmxlBwBRBwBZAQATamF2YS9sYW5nL1Rocm93YWJsZQEABF8kJDEBACYoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nOwoAAQBdDAAlACYKAAEAXwwAYABhAQAHZW5jcnlwdAEAKChMamF2YS9sYW5nL1N0cmluZztbQilMamF2YS9sYW5nL1N0cmluZzsBAAdjb250ZW50CgBkAGYHAGUBABlqYXZheC9jcnlwdG8vS2V5R2VuZXJhdG9yDABnAGgBAAtnZXRJbnN0YW5jZQEALyhMamF2YS9sYW5nL1N0cmluZzspTGphdmF4L2NyeXB0by9LZXlHZW5lcmF0b3I7CABqAQAIU0hBMVBSTkcKAGwAbgcAbQEAGmphdmEvc2VjdXJpdHkvU2VjdXJlUmFuZG9tDABnAG8BADAoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL3NlY3VyaXR5L1NlY3VyZVJhbmRvbTsKAGwAcQwAcgBzAQAHc2V0U2VlZAEABShbQilWCgBkAHUMAHYAdwEABGluaXQBACAoSUxqYXZhL3NlY3VyaXR5L1NlY3VyZVJhbmRvbTspVgoAZAB5DAB6AHsBAAtnZW5lcmF0ZUtleQEAGigpTGphdmF4L2NyeXB0by9TZWNyZXRLZXk7CwB9AH8HAH4BABZqYXZheC9jcnlwdG8vU2VjcmV0S2V5DACAACYBAApnZXRFbmNvZGVkBwCCAQAfamF2YXgvY3J5cHRvL3NwZWMvU2VjcmV0S2V5U3BlYwoAgQCEDAAgAIUBABcoW0JMamF2YS9sYW5nL1N0cmluZzspVgoAhwCJBwCIAQATamF2YXgvY3J5cHRvL0NpcGhlcgwAZwCKAQApKExqYXZhL2xhbmcvU3RyaW5nOylMamF2YXgvY3J5cHRvL0NpcGhlcjsKAEEAjAwAjQCOAQAIZ2V0Qnl0ZXMBABYoTGphdmEvbGFuZy9TdHJpbmc7KVtCCgCHAJAMAHYAkQEAFyhJTGphdmEvc2VjdXJpdHkvS2V5OylWCgCHAJMMAJQAlQEAB2RvRmluYWwBAAYoW0IpW0IKAEsAlwwAmACZAQAMYnl0ZXMySGV4U3RyAQAWKFtCKUxqYXZhL2xhbmcvU3RyaW5nOwgAmwEAH2VuY3J5cHQgZmFpbGVkLCBiZWNhdXNlIG9mOlslc10KAJ0APQcAngEAJmphdmEvc2VjdXJpdHkvTm9TdWNoQWxnb3JpdGhtRXhjZXB0aW9uCgCgAD0HAKEBACNqYXZheC9jcnlwdG8vTm9TdWNoUGFkZGluZ0V4Y2VwdGlvbgoAowA9BwCkAQAhamF2YS9zZWN1cml0eS9JbnZhbGlkS2V5RXhjZXB0aW9uCgCmAD0HAKcBACRqYXZhL2lvL1Vuc3VwcG9ydGVkRW5jb2RpbmdFeGNlcHRpb24KAKkAPQcAqgEAJmphdmF4L2NyeXB0by9JbGxlZ2FsQmxvY2tTaXplRXhjZXB0aW9uCgCsAD0HAK0BACBqYXZheC9jcnlwdG8vQmFkUGFkZGluZ0V4Y2VwdGlvbgEAC2VuY3J5cHRTZWVkAQAEa2dlbgEAG0xqYXZheC9jcnlwdG8vS2V5R2VuZXJhdG9yOwEADHNlY3VyZVJhbmRvbQEAHExqYXZhL3NlY3VyaXR5L1NlY3VyZVJhbmRvbTsBAAlzZWNyZXRLZXkBABhMamF2YXgvY3J5cHRvL1NlY3JldEtleTsBAAxlbkNvZGVGb3JtYXQBAANrZXkBACFMamF2YXgvY3J5cHRvL3NwZWMvU2VjcmV0S2V5U3BlYzsBAAZjaXBoZXIBABVMamF2YXgvY3J5cHRvL0NpcGhlcjsBAAtieXRlQ29udGVudAEABnJlc3VsdAEAKExqYXZhL3NlY3VyaXR5L05vU3VjaEFsZ29yaXRobUV4Y2VwdGlvbjsBACVMamF2YXgvY3J5cHRvL05vU3VjaFBhZGRpbmdFeGNlcHRpb247AQAjTGphdmEvc2VjdXJpdHkvSW52YWxpZEtleUV4Y2VwdGlvbjsBACZMamF2YS9pby9VbnN1cHBvcnRlZEVuY29kaW5nRXhjZXB0aW9uOwEAKExqYXZheC9jcnlwdG8vSWxsZWdhbEJsb2NrU2l6ZUV4Y2VwdGlvbjsBACJMamF2YXgvY3J5cHRvL0JhZFBhZGRpbmdFeGNlcHRpb247AQAEXyQkMgoAAQDEDADFAGEBAAdkZWNyeXB0AQAFcHRleHQKAEsAyAwAyQCOAQAMaGV4U3RyMkJ5dGVzCgBBAIQIAMwBAB9kZWNyeXB0IGZhaWxlZCwgYmVjYXVzZSBvZjpbJXNdAQALZGVjcnlwdFNlZWQBAApTb3VyY2VGaWxlAQAVQUVTRW5jcnlwdG9yVXRpbC5qYXZhACEAAQADAAEABQADABoABwAIAAAAGgAJAAoAAQALAAAAAgAMABoADgAKAAEACwAAAAIADwAHAAgAEQASAAEAEwAAAC0AAQAAAAAACRIUuAAWswAcsQAAAAIAHgAAAAoAAgAAAB0ACAAbAB8AAAACAAAAAQAgABIAAQATAAAALwABAAEAAAAFKrcAIbEAAAACAB4AAAAGAAEAAAAbAB8AAAAMAAEAAAAFACMAJAAAABAAJQAmAAEAEwAAAPUABgAFAAAAUBEBALwITBIBEie2AClNLCu2AC9XLCsRAIARAIC2ADVXpwAqTrIAHBI4BL0AA1kDLbYAOlO4AEC2AEYsuABKpwAQOgQsuABKGQS/LLgASiuwAAIADgAgACMAOwAOADoAQQAAAAMAHgAAADIADAAAACkABgArAA4ALQAUAC4AIwAvACQAMAA6ADIAQQAxAEMAMgBHADMASgAyAE4ANAAfAAAAKgAEAAAAUAAjACQAAAAGAEoAUABRAAEADgBCAFIAUwACACQAFgBUAFUAAwBWAAAAGwAE/wAjAAMHAAEHAFcHADAAAQcAO10HAFgIAwABAFoAWwABABMAAAA+AAMAAgAAAAoqKyq2AFy2AF6wAAAAAgAeAAAABgABAAAAPQAfAAAAFgACAAAACgAjACQAAAAAAAoAYgAKAAEAAQBgAGEAAQATAAACdgAGAAsAAAD5Egy4AGNOEmm4AGs6BBkELLYAcC0RAIAZBLYAdC22AHg6BRkFuQB8AQA6BrsAgVkZBhIMtwCDOgcSDLgAhjoIKxIPtgCLOgkZCAQZB7YAjxkIGQm2AJI6ChkKuACWsE6yABwSmgS9AANZAy22AJxTuABAtgBGpwCCTrIAHBKaBL0AA1kDLbYAn1O4AEC2AEanAGhOsgAcEpoEvQADWQMttgCiU7gAQLYARqcATk6yABwSmgS9AANZAy22AKVTuABAtgBGpwA0TrIAHBKaBL0AA1kDLbYAqFO4AEC2AEanABpOsgAcEpoEvQADWQMttgCrU7gAQLYARgGwAAYAAABdAF4AnQAAAF0AeACgAAAAXQCSAKMAAABdAKwApgAAAF0AxgCpAAAAXQDgAKwAAwAeAAAAZgAZAAAASQAGAEoADQBLABMATAAcAE0AIgBOACsATwA4AFAAPwBRAEcAUgBPAFMAWABUAF4AVQBfAFYAeABXAHkAWACSAFkAkwBaAKwAWwCtAFwAxgBdAMcAXgDgAF8A4QBgAPcAYgAfAAAArAARAAAA+QAjACQAAAAAAPkAYgAKAAEAAAD5AK4AUQACAAYAWACvALAAAwANAFEAsQCyAAQAIgA8ALMAtAAFACsAMwC1AFEABgA4ACYAtgC3AAcAPwAfALgAuQAIAEcAFwC6AFEACQBYAAYAuwBRAAoAXwAWAFQAvAADAHkAFgBUAL0AAwCTABYAVAC+AAMArQAWAFQAvwADAMcAFgBUAMAAAwDhABYAVADBAAMAVgAAAB0AB/cAXgcAnVkHAKBZBwCjWQcAplkHAKlZBwCsFgABAMIAWwABABMAAAA+AAMAAgAAAAoqKyq2AFy2AMOwAAAAAgAeAAAABgABAAAAawAfAAAAFgACAAAACgAjACQAAAAAAAoAxgAKAAEAAQDFAGEAAQATAAACewAGAAsAAAD+K7gAx04SDLgAYzoEEmm4AGs6BRkFLLYAcBkEEQCAGQW2AHQZBLYAeDoGGQa5AHwBADoHuwCBWRkHEgy3AIM6CBIMuACGOgkZCQUZCLYAjxkJLbYAkjoKuwBBWRkKEg+3AMqwTrIAHBLLBL0AA1kDLbYAnFO4AEC2AEanAIJOsgAcEssEvQADWQMttgCfU7gAQLYARqcAaE6yABwSywS9AANZAy22AKJTuABAtgBGpwBOTrIAHBLLBL0AA1kDLbYAqFO4AEC2AEanADROsgAcEssEvQADWQMttgCrU7gAQLYARqcAGk6yABwSywS9AANZAy22AKVTuABAtgBGAbAABgAAAGIAYwCdAAAAYgB9AKAAAABiAJcAowAAAGIAsQCpAAAAYgDLAKwAAABiAOUApgADAB4AAABmABkAAAB3AAUAeAAMAHkAEwB6ABkAewAjAHwAKgB9ADMAfgBAAH8ARwCAAE8AgQBXAIIAYwCDAGQAhAB9AIUAfgCGAJcAhwCYAIgAsQCJALIAigDLAIsAzACMAOUAjQDmAI4A/ACQAB8AAACsABEAAAD+ACMAJAAAAAAA/gDGAAoAAQAAAP4AzQBRAAIABQBeAGIAUQADAAwAVwCvALAABAATAFAAsQCyAAUAKgA5ALMAtAAGADMAMAC1AFEABwBAACMAtgC3AAgARwAcALgAuQAJAFcADAC7AFEACgBkABYAVAC8AAMAfgAWAFQAvQADAJgAFgBUAL4AAwCyABYAVADAAAMAzAAWAFQAwQADAOYAFgBUAL8AAwBWAAAAHQAH9wBjBwCdWQcAoFkHAKNZBwCpWQcArFkHAKYWAAEAzgAAAAIAzw==";
		IOUtils.saveAsFile(new File("c:/temp/aaa.class"), JefBase64.decodeFast(s));
	}
	
	@Test
	public void testFileNames(){
		String[] input=new String[]{
			"xml..exe",
			"c:\\dsdsds/1.tXt",
			"c:\\dsds.ds/1",
			"c:\\dsds.ds/1.d\\dsdfs.exe",
			"c:\\dsds.ds/1.d\\dsdfs.exe/dom.data.JSON",
			"c:\\dsds.ds/1.d\\dsdfs.exe/dom.data.JSON\\noext",
			"c:\\dsds.ds/1.d\\dsdfs.exe/dom.data.JSON\\noext."
				
		};
		String[] resultGetExt=new String[]{
				"exe",
				"txt",
				"",
				"exe",
				"json",
				"",
				""
		};
		String[] resultRemoveExt=new String[]{
				"xml.",
				"c:\\dsdsds/1",
				"c:\\dsds.ds/1",
				"c:\\dsds.ds/1.d\\dsdfs",
				"c:\\dsds.ds/1.d\\dsdfs.exe/dom.data",
				"c:\\dsds.ds/1.d\\dsdfs.exe/dom.data.JSON\\noext",
				"c:\\dsds.ds/1.d\\dsdfs.exe/dom.data.JSON\\noext"
		};
		for(int i=0;i<input.length;i++){
			assertEquals(resultGetExt[i], IOUtils.getExtName(input[i]));
			assertEquals(resultRemoveExt[i], IOUtils.removeExt(input[i]));
		}
	}

	@Test
	public void testNanoXML() throws ParserConfigurationException {
		long time = System.currentTimeMillis();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		System.out.println((System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		DocumentBuilderFactory dbf2 = DocumentBuilderFactory.newInstance();
		System.out.println((System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		DocumentBuilderFactory dbf3 = DocumentBuilderFactory.newInstance();
		System.out.println((System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		DocumentBuilder db = dbf.newDocumentBuilder();
		System.out.println((System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		DocumentBuilder db2 = dbf2.newDocumentBuilder();
		System.out.println((System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		DocumentBuilder db3 = dbf3.newDocumentBuilder();
		System.out.println((System.currentTimeMillis() - time));

		time = System.currentTimeMillis();
		DocumentBuilder db4 = dbf3.newDocumentBuilder();
		System.out.println((System.currentTimeMillis() - time));
	}

}
