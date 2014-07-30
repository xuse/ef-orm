//package jef.tools;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.util.TreeMap;
//
//import jef.tools.IOUtils;
//
//public class IndexAccessFile {
//	private File file;
//	private TreeMap<String, Long> index;
//
//	public IndexAccessFile(File file) throws IOException {
//		this.file = file;
//		if (file.exists()) {
//			init();
//		} else {
//			create();
//		}
//	}
//
//	private void create() throws IOException {
//		index = new TreeMap<String, Long>();
//		RandomAccessFile access = new RandomAccessFile(this.file, "rw");
//		saveIndex(access);
//		access.close();
//	}
//
//	private void saveIndex(RandomAccessFile access) throws IOException {
//		byte[] data = IOUtils.serialize(index);
//		long lastIndexPos=-1;
//		short count=0;
//		int dataIndex=0;
//		
//		//写入索引块
//		while(left>0){
//			long pos=access.getFilePointer();
//			access.writeShort(count); //2字节
//			access.writeLong(lastIndexPos); //8字节
//			access.writeLong(0);  //8字节
//			if(data.length-dataIndex>32768){
//				access.write(data, dataIndex, dataIndex+32768);
//			}else{
//				access.write(data);
//			}
//			
//			access.write(paramArrayOfByte)
//		}
//		
//		while()
//		access.write(data,0,);
//		System.out.println(access.getFilePointer());
//		
//		
//	}
//
//	private void init() {
//		try {
//			RandomAccessFile access = new RandomAccessFile(this.file, "rw");
//			int indexSize = access.readShort();
//			IOUtils.deserialize(data)
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//
//	public static void main(String[] args) {
//		IndexAccessFile file=new IndexAccessFile(new File("test.db"));
//	}
//}
