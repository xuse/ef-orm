package jef.common;

import org.apache.commons.lang.ArrayUtils;

/**
 * 
 * @author jiyi
 *
 */
public abstract class MarkableObject {
	private int[] bitField_=new int[getMarkNums()];
	private static int[] POWER_OF_2=new int[]{1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608,16777216,
			33554432,67108864,134217728,268435456,0x20000000,0x40000000,0x80000000};
	
	
	/**
	 * 
	 * @return
	 */
	protected abstract String[] getFieldNames();
	protected abstract int getMarkNums();
	
	
	
	/**
	 * 序号从0开始
	 * @param n
	 * @param value
	 */
	public void _onFieldSet(int n,Object value){
		int llFieldBit = POWER_OF_2[n%32];
		bitField_[n/32]|=llFieldBit;
	}
	
	/**
	 * 
	 * @param n
	 * @return
	 */
	public boolean _isFieldSet(int n){
		int llFieldBit = POWER_OF_2[n%32];
		return (bitField_[n/32]&llFieldBit)>0;
	}
	
	public boolean _isFieldSet(String n){
		int index=ArrayUtils.indexOf(getFieldNames(),n);
		if(index>-1){
			return _isFieldSet(index);
		}else{
			throw new IllegalArgumentException(n);
		}
	}
}
