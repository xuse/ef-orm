package jef.tools.csvreader;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jef.tools.IOUtils;
import jef.tools.support.JefBase64;

public class Codecs {
	private Codecs(){
	}
	
	private static final Map<Type,Codec<?>> CACHE=new HashMap<Type,Codec<?>>();
	
	static{
		init();
	}
	
	private static Codec<String> STRING=new Codec<String>(){
		public String toString(String t) {
			return t;
		}
		public String fromString(String s) {
			return s;
		}
	};
	private static Codec<Integer> I=new Codec<Integer>(){
		public String toString(Integer t) {
			return String.valueOf(t);
		}
		public Integer fromString(String s) {
			if(s==null||s.length()==0)return 0;
			return Integer.parseInt(s);
		}
	};
	private static Codec<Boolean> Z=new Codec<Boolean>(){
		public String toString(Boolean t) {
			return String.valueOf(t);
		}
		public Boolean fromString(String s) {
			if(s==null||s.length()==0)return false;
			return Boolean.parseBoolean(s);
		}
	};
	private static Codec<Byte> B=new Codec<Byte>(){
		public String toString(Byte t) {
			return String.valueOf(t);
		}
		public Byte fromString(String s) {
			if(s==null||s.length()==0)return 0;
			return Byte.parseByte(s);
		}
	};
	private static Codec<Short> S=new Codec<Short>(){
		public String toString(Short t) {
			return String.valueOf(t);
		}
		public Short fromString(String s) {
			if(s==null||s.length()==0)return 0;
			return Short.parseShort(s);
		}
	};
	private static Codec<Double> D=new Codec<Double>(){
		public String toString(Double t) {
			return String.valueOf(t);
		}
		public Double fromString(String s) {
			if(s==null||s.length()==0)return 0D;
			return Double.parseDouble(s);
		}
	};
	private static Codec<Long> L=new Codec<Long>(){
		public String toString(Long t) {
			return String.valueOf(t);
		}
		public Long fromString(String s) {
			if(s==null||s.length()==0)return 0L;
			return Long.parseLong(s);
		}
	};
	private static Codec<Float> F=new Codec<Float>(){
		public String toString(Float t) {
			return String.valueOf(t);
		}
		public Float fromString(String s) {
			if(s==null||s.length()==0)return 0F;
			return Float.parseFloat(s);
		}
	};
	private static Codec<Character> C=new Codec<Character>(){
		public String toString(Character t) {
			return String.valueOf(t);
		}
		public Character fromString(String s) {
			if(s==null||s.length()==0)return 0;
			return s.charAt(0);
		}
	};
	private static Codec<Date> uDate=new Codec<Date>(){
		public String toString(Date t) {
			if(t==null)return "";
			return String.valueOf(t.getTime());
		}
		public Date fromString(String s) {
			if(s==null||s.length()==0)return null;
			return new Date(Long.parseLong(s));
		}
	};
	private static Codec<java.sql.Date> sDate=new Codec<java.sql.Date>(){
		public String toString(java.sql.Date t) {
			if(t==null)return "";
			return String.valueOf(t.getTime());
		}
		public java.sql.Date fromString(String s) {
			if(s==null||s.length()==0)return null;
			return new java.sql.Date(Long.parseLong(s));
		}
	};
	private static Codec<java.sql.Time> sTime=new Codec<java.sql.Time>(){
		public String toString(java.sql.Time t) {
			if(t==null)return "";
			return String.valueOf(t.getTime());
		}
		public java.sql.Time fromString(String s) {
			if(s==null||s.length()==0)return null;
			return new java.sql.Time(Long.parseLong(s));
		}
	};
	private static Codec<Timestamp> TIMESTAMP=new Codec<Timestamp>(){
		public String toString(Timestamp t) {
			if(t==null)return "";
			return String.valueOf(t.getTime());
		}
		public Timestamp fromString(String s) {
			if(s==null||s.length()==0)return null;
			return new Timestamp(Long.parseLong(s));
		}
	};
	private static Codec<byte[]> BIN=new Codec<byte[]>(){
		public String toString(byte[] t) {
			if(t==null)return "";
			return JefBase64.encode(t);
		}
		public byte[] fromString(String s) {
			if(s==null||s.length()==0)return null;
			return JefBase64.decodeFast(s);
		}
	};
	private static Codec<Serializable> OTHER=new Codec<Serializable>(){
		public String toString(Serializable t) {
			return JefBase64.encode(IOUtils.saveObject(t));
		}
		public Serializable fromString(String s) {
			if(s==null||s.length()==0)return null;
			return (Serializable) IOUtils.loadObject(JefBase64.decodeFast(s));
		}
	};
	

	private static void init() {
		CACHE.put(String.class, STRING);
		
		CACHE.put(Integer.class, I);
		CACHE.put(Integer.TYPE, I);
		
		CACHE.put(Short.class, S);
		CACHE.put(Short.TYPE, S);
		
		CACHE.put(Long.class, L);
		CACHE.put(Long.TYPE, L);
		
		CACHE.put(Float.class, F);
		CACHE.put(Float.TYPE, F);
		
		CACHE.put(Double.class, D);
		CACHE.put(Double.TYPE, D);
		
		CACHE.put(Character.class, C);
		CACHE.put(Character.TYPE, C);
		
		CACHE.put(Byte.class, B);
		CACHE.put(Byte.TYPE, B);
		
		CACHE.put(Boolean.class, Z);
		CACHE.put(Boolean.TYPE, Z);
		
		CACHE.put(Byte[].class, BIN);
		CACHE.put(Date.class, uDate);
		CACHE.put(java.sql.Date.class, sDate);
		CACHE.put(java.sql.Time.class, sTime);
		CACHE.put(java.sql.Timestamp.class, TIMESTAMP);
	}
	
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static String toString(Object obj,Type type){
		if(type instanceof Class<?>){
			Class<?> clz=(Class<?>) type;
			if(clz.isEnum()){
				return ((Enum<?>)obj).name();
			}
		}
		Codec codec=CACHE.get(type);
		if(codec==null){
			return OTHER.toString((Serializable) obj);
//			throw new UnsupportedOperationException("Object to String error, type "+type+" was not supported.");
		}
		return codec.toString(obj);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object fromString(String s,Type type){
		if(type instanceof Class<?>){
			Class<?> clz=(Class<?>) type;
			if(clz.isEnum()){
				return Enum.valueOf(clz.asSubclass(Enum.class), s);
			}
		}
		Codec codec=CACHE.get(type);
		if(codec==null){
			return OTHER.fromString(s);
//			throw new UnsupportedOperationException("Object to String error, type "+type+" was not supported.");
		}
		return codec.fromString(s);
	}
}
