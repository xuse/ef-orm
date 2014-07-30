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
package jef.tools.security.cplus;

//Referenced classes of package com.asiainfo.openboss.obsystem.utility.security:
//DES3Byte

public class HByte {

	public HByte() {
		ibyte = new DES3Byte();
	}

	public byte getAbyte() {
		int array[] = { ibyte.bit0, ibyte.bit1, ibyte.bit2, ibyte.bit3,
				ibyte.bit4, ibyte.bit5, ibyte.bit6, ibyte.bit7 };
		String bString = "";
		for (int i = 0; i < array.length; i++)
			bString = array[i] + bString;

		return getByteFromChar((char) Integer.valueOf(bString, 2).intValue());
	}

	public static char getBinaryValue(String bString) {
		int result = 0;
		String temp = bString;
		for (int i = 0; i < 32 - bString.length(); i++)
			temp = "0" + temp;

		result += Integer.valueOf(temp, 2).intValue();
		return (char) result;
	}

	public static byte getByteFromChar(char c) {
		if (c >= '\200') {
			int t = ~c + 1;
			String bString = Integer.toBinaryString(t);
			bString = bString.substring(bString.length() - 8);
			int temp = Integer.valueOf(bString, 2).intValue() * -1;
			return (byte) temp;
		} else {
			return (byte) c;
		}
	}

	public static char getChar(byte b) {
		if (b >= 0)
			return (char) b;
		char temp = (char) (b * -1);
		if (b == -128) {
			return temp;
		} else {
			temp = (char) (~temp + 1);
			String bString = Integer.toBinaryString(temp);
			return getBinaryValue(bString.substring(bString.length() - 8));
		}
	}

	public void setAbyte(byte abyte) {
		String bString = null;
		this.abyte = getChar(abyte);
		if (this.abyte >= '\200') {
			bString = Integer.toBinaryString(this.abyte);
		} else {
			bString = Integer.toString(abyte, 2);
			int len = bString.length();
			for (int i = len; i < 8; i++)
				bString = "0" + bString;

		}
		ibyte.bit7 = (char) Integer.parseInt(bString.charAt(0) + "");
		ibyte.bit6 = (char) Integer.parseInt(bString.charAt(1) + "");
		ibyte.bit5 = (char) Integer.parseInt(bString.charAt(2) + "");
		ibyte.bit4 = (char) Integer.parseInt(bString.charAt(3) + "");
		ibyte.bit3 = (char) Integer.parseInt(bString.charAt(4) + "");
		ibyte.bit2 = (char) Integer.parseInt(bString.charAt(5) + "");
		ibyte.bit1 = (char) Integer.parseInt(bString.charAt(6) + "");
		ibyte.bit0 = (char) Integer.parseInt(bString.charAt(7) + "");
	}

	private char abyte;
	DES3Byte ibyte;

	public static class DES3Byte {

		public DES3Byte() {
		}

		public char bit0;
		public char bit1;
		public char bit2;
		public char bit3;
		public char bit4;
		public char bit5;
		public char bit6;
		public char bit7;
	}
}
