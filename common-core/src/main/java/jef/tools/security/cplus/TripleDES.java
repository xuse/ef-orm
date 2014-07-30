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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Asiainfo提供的3DES算法，和C++版本的兼容。
 * 原先的代码有点问题，不支持双字节码，已经修正。
 * 总的来说功能和性能较差，仅为兼容某些C++加密出来的字符串使用。
 * 在padding算法等地方和java以及C#的版本不兼容。
 * @author Administrator
 */
public class TripleDES {
	public TripleDES() {
	}

	public void Gsubkey(byte key[], byte subkey[][]) {
		byte cup[] = new byte[4];
		byte dup[] = new byte[4];
		byte ci[] = new byte[4];
		byte di[] = new byte[4];
		byte lsi[] = new byte[16];
		HByte ip1 = new HByte();
		HByte ipr = new HByte();
		ip1.setAbyte((byte) 0);
		ipr.setAbyte((byte) 0);
		lsi[0] = lsi[1] = lsi[8] = lsi[15] = 1;
		lsi[2] = lsi[3] = lsi[4] = lsi[5] = lsi[6] = lsi[7] = 2;
		lsi[9] = lsi[10] = lsi[11] = lsi[12] = lsi[13] = lsi[14] = 2;
		ip1.setAbyte(key[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(key[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(key[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit7;
		ip1.setAbyte(key[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(key[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(key[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit7;
		ip1.setAbyte(key[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit7;
		ip1.setAbyte(key[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		ci[3] = ipr.getAbyte();
		ip1.setAbyte(key[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit6;
		ip1.setAbyte(key[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit6;
		ip1.setAbyte(key[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(key[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(key[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit6;
		ip1.setAbyte(key[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ip1.setAbyte(key[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(key[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit6;
		ci[2] = ipr.getAbyte();
		ip1.setAbyte(key[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(key[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit5;
		ip1.setAbyte(key[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(key[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit5;
		ip1.setAbyte(key[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(key[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(key[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(key[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit5;
		ci[1] = ipr.getAbyte();
		ip1.setAbyte(key[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(key[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(key[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(key[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ci[0] = ipr.getAbyte();
		ip1.setAbyte(key[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(key[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(key[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(key[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit1;
		ip1.setAbyte(key[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(key[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit1;
		ip1.setAbyte(key[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit1;
		ip1.setAbyte(key[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit1;
		di[3] = ipr.getAbyte();
		ip1.setAbyte(key[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(key[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(key[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit2;
		ip1.setAbyte(key[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(key[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(key[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit2;
		ip1.setAbyte(key[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit2;
		ip1.setAbyte(key[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit2;
		di[2] = ipr.getAbyte();
		ip1.setAbyte(key[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(key[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(key[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(key[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(key[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(key[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(key[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit3;
		ip1.setAbyte(key[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit3;
		di[1] = ipr.getAbyte();
		ip1.setAbyte(key[4]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(key[5]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(key[6]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(key[7]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		di[0] = ipr.getAbyte();
		for (int i = 0; i < 16; i++) {
			cup[3] = ci[3];
			cup[2] = ci[2];
			cup[1] = ci[1];
			cup[0] = ci[0];
			dup[3] = di[3];
			dup[2] = di[2];
			dup[1] = di[1];
			dup[0] = di[0];
			rotatebits(cup, ci, lsi[i]);
			rotatebits(dup, di, lsi[i]);
			pc2(ci, di, subkey[i]);
		}

	}

	void SDes(byte orientation, byte PlainText[], byte Key[], byte Encipher[]) {
		byte m[] = new byte[8];
		byte k[] = new byte[8];
		m[0] = PlainText[7];
		m[1] = PlainText[6];
		m[2] = PlainText[5];
		m[3] = PlainText[4];
		m[4] = PlainText[3];
		m[5] = PlainText[2];
		m[6] = PlainText[1];
		m[7] = PlainText[0];
		k[0] = Key[7];
		k[1] = Key[6];
		k[2] = Key[5];
		k[3] = Key[4];
		k[4] = Key[3];
		k[5] = Key[2];
		k[6] = Key[1];
		k[7] = Key[0];
		if (orientation == 0)
			des(m, k);
		else
			undes(m, k);
		Encipher[0] = m[7];
		Encipher[1] = m[6];
		Encipher[2] = m[5];
		Encipher[3] = m[4];
		Encipher[4] = m[3];
		Encipher[5] = m[2];
		Encipher[6] = m[1];
		Encipher[7] = m[0];
	}

	public void TDes(byte orientation, byte PlainText[], byte key[],
			byte ucEncipher[]) {
		byte En[] = new byte[8];
		byte key2[] = new byte[key.length - 8];
		if (orientation == 0) {
			SDes((byte) 0, PlainText, key, En);
			for (int i = 0; i < key2.length; i++)
				key2[i] = key[i + 8];

			SDes((byte) 1, En, key2, En);
			for (int i = 0; i < key2.length; i++)
				key[i + 8] = key2[i];

			SDes((byte) 0, En, key, ucEncipher);
		} else {
			SDes((byte) 1, PlainText, key, En);
			for (int i = 0; i < key2.length; i++)
				key2[i] = key[i + 8];

			SDes((byte) 0, En, key2, En);
			for (int i = 0; i < key2.length; i++)
				key[i + 8] = key2[i];

			SDes((byte) 1, En, key, ucEncipher);
		}
	}

	public byte asc_bcd(byte what1, byte what2) {
		char digit = (char) (what1 < 97 ? what1 - 48 : (what1 - 97) + 10);
		digit *= '\020';
		digit += what2 < 97 ? ((char) (what2 - 48))
				: ((char) ((what2 - 97) + 10));
		return HByte.getByteFromChar(digit);
	}

	public String cipher2(byte key[], String plain_textStr) {
		byte[] plain_text=plain_textStr.getBytes();
		byte keyBytes[] = key;
		byte t_plain[] = new byte[1024];
		byte t_crypt[] = new byte[1024];
		int pad_cnt = 0;
		int length=plain_text.length;
		if (length > 1024)
			throw new IllegalArgumentException("length exceed");
		for (int i = 0; i < plain_text.length; i++)
			t_plain[i] = (byte) plain_text[i];

		int en_cnt;
		if (length % 8 == 0) {
			en_cnt = length / 8;
		} else {
			en_cnt = length / 8 + 1;
			pad_cnt = 8 - length % 8;
			for (int i = 0; i < pad_cnt; i++)
				t_plain[length + i] = 0;

		}
		for (int i = 0; i < en_cnt; i++) {
			byte t_plain_tmp[] = new byte[t_plain.length - 8 * i];
			byte t_crypt_tmp[] = new byte[t_crypt.length - 8 * i];
			for (int j = 0; j < t_plain_tmp.length; j++)
				t_plain_tmp[j] = t_plain[j + i * 8];

			for (int j = 0; j < t_crypt_tmp.length; j++)
				t_crypt_tmp[j] = t_crypt[j + i * 8];

			TDes((byte) 0, t_plain_tmp, keyBytes, t_crypt_tmp);
			for (int j = 0; j < t_plain_tmp.length; j++)
				t_plain[j + i * 8] = t_plain_tmp[j];

			for (int j = 0; j < t_crypt_tmp.length; j++)
				t_crypt[j + i * 8] = t_crypt_tmp[j];

		}

		StringBuilder sb=new StringBuilder();
		for (int i = 0; i < 8 * en_cnt; i++) {
			byte temp = t_crypt[i];
			if (temp < 10 && temp >= 0) {
				sb.append("0" + temp);
			} else {
				String hexString = Integer.toHexString(HByte.getChar(temp));
				if (temp > 0 && temp <= 15)
					sb.append("0");
				sb.append(hexString);
			}
		}
		return sb.toString();
	}

	public void compress(byte in[], byte out[]) {
		byte s[][][] = {
				{
						{ 14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7 },
						{ 0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8 },
						{ 4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0 },
						{ 15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13 } },
				{
						{ 15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10 },
						{ 3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5 },
						{ 0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15 },
						{ 13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9 } },
				{
						{ 10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8 },
						{ 13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1 },
						{ 13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7 },
						{ 1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12 } },
				{
						{ 7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15 },
						{ 13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9 },
						{ 10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4 },
						{ 3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14 } },
				{
						{ 2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9 },
						{ 14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6 },
						{ 4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14 },
						{ 11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3 } },
				{
						{ 12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11 },
						{ 10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8 },
						{ 9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6 },
						{ 4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13 } },
				{
						{ 4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1 },
						{ 13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6 },
						{ 1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2 },
						{ 6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12 } },
				{
						{ 13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7 },
						{ 1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2 },
						{ 7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8 },
						{ 2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11 } } };
		HByte ip1 = new HByte();
		HByte ipr = new HByte();
		byte tmp[] = new byte[8];
		byte c[] = new byte[8];
		ip1.setAbyte((byte) 0);
		ipr.setAbyte((byte) 0);
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit6 = ip1.ibyte.bit6;
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit2;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[7] = ipr.getAbyte();
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(in[5]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit5 = ip1.ibyte.bit7;
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[6] = ipr.getAbyte();
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(in[4]);
		ipr.ibyte.bit4 = ip1.ibyte.bit0;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[5] = ipr.getAbyte();
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit2 = ip1.ibyte.bit0;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[4] = ipr.getAbyte();
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit6 = ip1.ibyte.bit6;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit2 = ip1.ibyte.bit2;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[3] = ipr.getAbyte();
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit7;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[2] = ipr.getAbyte();
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit4 = ip1.ibyte.bit0;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[1] = ipr.getAbyte();
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit2 = ip1.ibyte.bit0;
		ipr.ibyte.bit1 = '\0';
		ipr.ibyte.bit0 = '\0';
		tmp[0] = ipr.getAbyte();
		for (int i = 7; i >= 0; i--) {
			ip1.setAbyte(tmp[i]);
			ipr.ibyte.bit1 = ip1.ibyte.bit7;
			ipr.ibyte.bit0 = ip1.ibyte.bit2;
			byte hang = (byte) (ipr.getAbyte() & 3);
			ipr.ibyte.bit3 = ip1.ibyte.bit6;
			ipr.ibyte.bit2 = ip1.ibyte.bit5;
			ipr.ibyte.bit1 = ip1.ibyte.bit4;
			ipr.ibyte.bit0 = ip1.ibyte.bit3;
			byte lie = (byte) (ipr.getAbyte() & 15);
			c[i] = s[7 - i][hang][lie];
		}

		out[3] = (byte) (c[7] << 4 | c[6]);
		out[2] = (byte) (c[5] << 4 | c[4]);
		out[1] = (byte) (c[3] << 4 | c[2]);
		out[0] = (byte) (c[1] << 4 | c[0]);
	}

	public String decipher2(byte key[], String crypted_text) {
		byte keyBytes[] = makeUpKey(key, 16);
		byte t_crypt[] = new byte[1024];
		byte plain_text[] = new byte[1024];
		int length=crypted_text.length();
		byte crypted_text_array[] = crypted_text.getBytes();
		for (int i = 0; i < length / 2; i++)
			t_crypt[i] = asc_bcd(crypted_text_array[i * 2],
					crypted_text_array[i * 2 + 1]);

		int en_cnt = length / 16;
		for (int i = 0; i < en_cnt; i++) {
			byte plain_text_tmp[] = new byte[plain_text.length - 8 * i];
			byte t_crypt_tmp[] = new byte[t_crypt.length - 8 * i];
			for (int j = 0; j < plain_text_tmp.length; j++)
				plain_text_tmp[j] = plain_text[j + i * 8];

			for (int j = 0; j < t_crypt_tmp.length; j++)
				t_crypt_tmp[j] = t_crypt[j + i * 8];

			TDes((byte) 1, t_crypt_tmp, keyBytes, plain_text_tmp);
			for (int j = 0; j < plain_text_tmp.length; j++)
				plain_text[j + i * 8] = plain_text_tmp[j];

			for (int j = 0; j < t_crypt_tmp.length; j++)
				t_crypt[j + i * 8] = t_crypt_tmp[j];

		}

		plain_text[length / 2] = 0;
		int n=0;
		for (int i = 0; i < plain_text.length; i++) {
			if (plain_text[i] == 0){
				n=i;
				break;
			}
		}
		try {
			return new String(plain_text,0,n,Charset.defaultCharset().name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public void des(byte m[], byte key[]) {
		byte ip[] = new byte[8];
		byte lin[] = new byte[4];
		byte rin[] = new byte[4];
		byte lup[] = new byte[4];
		byte rup[] = new byte[4];
		byte tmp[] = new byte[6];
		byte tmp4[] = new byte[4];
		byte subkey[][] = new byte[16][6];
		HByte ip1 = new HByte();
		HByte ipr = new HByte();
		ip1.setAbyte((byte) 0);
		ipr.setAbyte((byte) 0);
		Gsubkey(key, subkey);
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit6;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit6;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit6;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit6;
		ip[7] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit4;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		ip[6] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit2;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit2;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit2;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit2;
		ip[5] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit0;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit0;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit0;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit0;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit0;
		ip[4] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit7;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit7;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit7;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		ip[3] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit5;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit5;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit5;
		ip[2] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit3;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit3;
		ip[1] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit1;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit1;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit1;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit1;
		ip[0] = ipr.getAbyte();
		lin[3] = ip[7];
		lin[2] = ip[6];
		lin[1] = ip[5];
		lin[0] = ip[4];
		rin[3] = ip[3];
		rin[2] = ip[2];
		rin[1] = ip[1];
		rin[0] = ip[0];
		for (int j = 0; j < 16; j++) {
			for (int i = 0; i < 4; i++)
				lup[i] = lin[i];

			for (int i = 0; i < 4; i++)
				rup[i] = rin[i];

			expand(rup, tmp);
			for (int i = 0; i < 6; i++)
				tmp[i] = (byte) (tmp[i] ^ subkey[j][i]);

			compress(tmp, rin);
			permutate(rin, tmp4);
			for (int i = 0; i < 4; i++)
				rin[i] = (byte) (lup[i] ^ tmp4[i]);

			for (int i = 0; i < 4; i++)
				lin[i] = rup[i];

		}

		for (int i = 0; i < 4; i++)
			tmp4[i] = rin[i];

		for (int i = 0; i < 4; i++)
			rin[i] = lin[i];

		for (int i = 0; i < 4; i++)
			lin[i] = tmp4[i];

		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit0;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit0;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit0;
		ip[7] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit1;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit1;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit1;
		ip[6] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit2;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit2;
		ip[5] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit3;
		ip[4] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		ip[3] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit5;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit5;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit5;
		ip[2] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit6;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit6;
		ip[1] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit7;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		ip[0] = ipr.getAbyte();
		m[7] = ip[7];
		m[6] = ip[6];
		m[5] = ip[5];
		m[4] = ip[4];
		m[3] = ip[3];
		m[2] = ip[2];
		m[1] = ip[1];
		m[0] = ip[0];
	}

	public void expand(byte in[], byte out[]) {
		HByte ip1 = new HByte();
		HByte ipr = new HByte();
		ip1.setAbyte((byte) 0);
		ipr.setAbyte((byte) 0);
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit0;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit5;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit3 = ip1.ibyte.bit4;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit0 = ip1.ibyte.bit3;
		out[5] = ipr.getAbyte();
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit2 = ip1.ibyte.bit7;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit0 = ip1.ibyte.bit5;
		out[4] = ipr.getAbyte();
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit2 = ip1.ibyte.bit1;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit1 = ip1.ibyte.bit0;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		out[3] = ipr.getAbyte();
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit0;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit4 = ip1.ibyte.bit5;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit4;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit0 = ip1.ibyte.bit3;
		out[2] = ipr.getAbyte();
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit2 = ip1.ibyte.bit7;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit5;
		out[1] = ipr.getAbyte();
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit2 = ip1.ibyte.bit1;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit0;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		out[0] = ipr.getAbyte();
	}

	public byte[] makeUpKey(byte key[], int length) {
		if (key.length < length) {
			byte result[] = new byte[length];
			for (int i = 0; i < key.length; i++)
				result[i] = key[i];

			return result;
		} else {
			return key;
		}
	}

	public void pc2(byte keyc[], byte keyd[], byte subkey[]) {
		HByte ip1 = new HByte();
		HByte ipr = new HByte();
		ip1.setAbyte((byte) 0);
		ipr.setAbyte((byte) 0);
		ip1.setAbyte(keyc[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(keyc[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(keyc[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(keyc[1]);
		ipr.ibyte.bit4 = ip1.ibyte.bit0;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(keyc[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		subkey[5] = ipr.getAbyte();
		ip1.setAbyte(keyc[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(keyc[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(keyc[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(keyc[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(keyc[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(keyc[2]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		subkey[4] = ipr.getAbyte();
		ip1.setAbyte(keyc[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit6;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(keyc[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit1;
		ip1.setAbyte(keyc[0]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(keyc[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ip1.setAbyte(keyc[2]);
		ipr.ibyte.bit1 = ip1.ibyte.bit3;
		ip1.setAbyte(keyc[3]);
		ipr.ibyte.bit0 = ip1.ibyte.bit6;
		subkey[3] = ipr.getAbyte();
		ip1.setAbyte(keyd[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(keyd[3]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(keyd[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(keyd[0]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(keyd[3]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(keyd[2]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		subkey[2] = ipr.getAbyte();
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(keyd[3]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ip1.setAbyte(keyd[2]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(keyd[2]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(keyd[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		subkey[1] = ipr.getAbyte();
		ip1.setAbyte(keyd[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(keyd[0]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(keyd[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(keyd[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(keyd[3]);
		ipr.ibyte.bit2 = ip1.ibyte.bit0;
		ip1.setAbyte(keyd[3]);
		ipr.ibyte.bit1 = ip1.ibyte.bit7;
		ip1.setAbyte(keyd[3]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		subkey[0] = ipr.getAbyte();
	}

	public void permutate(byte in[], byte out[]) {
		HByte ip1 = new HByte();
		HByte ipr = new HByte();
		ip1.setAbyte((byte) 0);
		ipr.setAbyte((byte) 0);
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit7 = ip1.ibyte.bit0;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		out[3] = ipr.getAbyte();
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit1;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit0 = ip1.ibyte.bit6;
		out[2] = ipr.getAbyte();
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit6;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		out[1] = ipr.getAbyte();
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit5 = ip1.ibyte.bit2;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(in[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(in[2]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(in[3]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(in[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		out[0] = ipr.getAbyte();
	}

	public void rotatebits(byte key[], byte skey[], byte bits) {
		HByte c[] = new HByte[4];
		HByte d[] = new HByte[4];
		for (int i = 0; i < c.length; i++) {
			c[i] = new HByte();
			d[i] = new HByte();
		}

		c[0].setAbyte(key[0]);
		c[1].setAbyte(key[1]);
		c[2].setAbyte(key[2]);
		c[3].setAbyte(key[3]);
		if (bits == 1) {
			d[0].ibyte.bit4 = c[3].ibyte.bit7;
			d[0].ibyte.bit5 = c[0].ibyte.bit4;
			d[0].ibyte.bit6 = c[0].ibyte.bit5;
			d[0].ibyte.bit7 = c[0].ibyte.bit6;
			d[1].ibyte.bit0 = c[0].ibyte.bit7;
			d[1].ibyte.bit1 = c[1].ibyte.bit0;
			d[1].ibyte.bit2 = c[1].ibyte.bit1;
			d[1].ibyte.bit3 = c[1].ibyte.bit2;
			d[1].ibyte.bit4 = c[1].ibyte.bit3;
			d[1].ibyte.bit5 = c[1].ibyte.bit4;
			d[1].ibyte.bit6 = c[1].ibyte.bit5;
			d[1].ibyte.bit7 = c[1].ibyte.bit6;
			d[2].ibyte.bit0 = c[1].ibyte.bit7;
			d[2].ibyte.bit1 = c[2].ibyte.bit0;
			d[2].ibyte.bit2 = c[2].ibyte.bit1;
			d[2].ibyte.bit3 = c[2].ibyte.bit2;
			d[2].ibyte.bit4 = c[2].ibyte.bit3;
			d[2].ibyte.bit5 = c[2].ibyte.bit4;
			d[2].ibyte.bit6 = c[2].ibyte.bit5;
			d[2].ibyte.bit7 = c[2].ibyte.bit6;
			d[3].ibyte.bit0 = c[2].ibyte.bit7;
			d[3].ibyte.bit1 = c[3].ibyte.bit0;
			d[3].ibyte.bit2 = c[3].ibyte.bit1;
			d[3].ibyte.bit3 = c[3].ibyte.bit2;
			d[3].ibyte.bit4 = c[3].ibyte.bit3;
			d[3].ibyte.bit5 = c[3].ibyte.bit4;
			d[3].ibyte.bit6 = c[3].ibyte.bit5;
			d[3].ibyte.bit7 = c[3].ibyte.bit6;
		} else {
			d[0].ibyte.bit4 = c[3].ibyte.bit6;
			d[0].ibyte.bit5 = c[3].ibyte.bit7;
			d[0].ibyte.bit6 = c[0].ibyte.bit4;
			d[0].ibyte.bit7 = c[0].ibyte.bit5;
			d[1].ibyte.bit0 = c[0].ibyte.bit6;
			d[1].ibyte.bit1 = c[0].ibyte.bit7;
			d[1].ibyte.bit2 = c[1].ibyte.bit0;
			d[1].ibyte.bit3 = c[1].ibyte.bit1;
			d[1].ibyte.bit4 = c[1].ibyte.bit2;
			d[1].ibyte.bit5 = c[1].ibyte.bit3;
			d[1].ibyte.bit6 = c[1].ibyte.bit4;
			d[1].ibyte.bit7 = c[1].ibyte.bit5;
			d[2].ibyte.bit0 = c[1].ibyte.bit6;
			d[2].ibyte.bit1 = c[1].ibyte.bit7;
			d[2].ibyte.bit2 = c[2].ibyte.bit0;
			d[2].ibyte.bit3 = c[2].ibyte.bit1;
			d[2].ibyte.bit4 = c[2].ibyte.bit2;
			d[2].ibyte.bit5 = c[2].ibyte.bit3;
			d[2].ibyte.bit6 = c[2].ibyte.bit4;
			d[2].ibyte.bit7 = c[2].ibyte.bit5;
			d[3].ibyte.bit0 = c[2].ibyte.bit6;
			d[3].ibyte.bit1 = c[2].ibyte.bit7;
			d[3].ibyte.bit2 = c[3].ibyte.bit0;
			d[3].ibyte.bit3 = c[3].ibyte.bit1;
			d[3].ibyte.bit4 = c[3].ibyte.bit2;
			d[3].ibyte.bit5 = c[3].ibyte.bit3;
			d[3].ibyte.bit6 = c[3].ibyte.bit4;
			d[3].ibyte.bit7 = c[3].ibyte.bit5;
		}
		skey[0] = d[0].getAbyte();
		skey[1] = d[1].getAbyte();
		skey[2] = d[2].getAbyte();
		skey[3] = d[3].getAbyte();
	}

	public void undes(byte m[], byte key[]) {
		byte ip[] = new byte[8];
		byte lin[] = new byte[4];
		byte rin[] = new byte[4];
		byte lup[] = new byte[4];
		byte rup[] = new byte[4];
		byte tmp[] = new byte[6];
		byte tmp4[] = new byte[4];
		byte subkey[][] = new byte[16][6];
		HByte ip1 = new HByte();
		HByte ipr = new HByte();
		ip1.setAbyte((byte) 0);
		ipr.setAbyte((byte) 0);
		Gsubkey(key, subkey);
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit6;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit6;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit6;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit6;
		ip[7] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit4;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		ip[6] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit2;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit2;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit2;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit2;
		ip[5] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit0;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit0;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit0;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit0;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit0;
		ip[4] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit7;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit7;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit7;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		ip[3] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit5;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit5;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit5;
		ip[2] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit3;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit3;
		ip[1] = ipr.getAbyte();
		ip1.setAbyte(m[0]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(m[1]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(m[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(m[3]);
		ipr.ibyte.bit4 = ip1.ibyte.bit1;
		ip1.setAbyte(m[4]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(m[5]);
		ipr.ibyte.bit2 = ip1.ibyte.bit1;
		ip1.setAbyte(m[6]);
		ipr.ibyte.bit1 = ip1.ibyte.bit1;
		ip1.setAbyte(m[7]);
		ipr.ibyte.bit0 = ip1.ibyte.bit1;
		ip[0] = ipr.getAbyte();
		lin[3] = ip[7];
		lin[2] = ip[6];
		lin[1] = ip[5];
		lin[0] = ip[4];
		rin[3] = ip[3];
		rin[2] = ip[2];
		rin[1] = ip[1];
		rin[0] = ip[0];
		for (int j = 0; j < 16; j++) {
			for (int i = 0; i < 4; i++)
				lup[i] = lin[i];

			for (int i = 0; i < 4; i++)
				rup[i] = rin[i];

			expand(rup, tmp);
			for (int i = 0; i < 6; i++)
				tmp[i] = (byte) (tmp[i] ^ subkey[15 - j][i]);

			compress(tmp, rin);
			permutate(rin, tmp4);
			for (int i = 0; i < 4; i++)
				rin[i] = (byte) (lup[i] ^ tmp4[i]);

			for (int i = 0; i < 4; i++)
				lin[i] = rup[i];

		}

		for (int i = 0; i < 4; i++)
			tmp4[i] = rin[i];

		for (int i = 0; i < 4; i++)
			rin[i] = lin[i];

		for (int i = 0; i < 4; i++)
			lin[i] = tmp4[i];

		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit0;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit0;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit0;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit0;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit0;
		ip[7] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit1;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit1;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit1;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit1;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit1;
		ip[6] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit2;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit2;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit2;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit2;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit2;
		ip[5] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit3;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit3;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit3;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit3;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit3;
		ip[4] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit4;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit4;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit4;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit4;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit4;
		ip[3] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit5;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit5;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit5;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit5;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit5;
		ip[2] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit6;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit6;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit6;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit6;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit6;
		ip[1] = ipr.getAbyte();
		ip1.setAbyte(rin[3]);
		ipr.ibyte.bit7 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[3]);
		ipr.ibyte.bit6 = ip1.ibyte.bit7;
		ip1.setAbyte(rin[2]);
		ipr.ibyte.bit5 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[2]);
		ipr.ibyte.bit4 = ip1.ibyte.bit7;
		ip1.setAbyte(rin[1]);
		ipr.ibyte.bit3 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[1]);
		ipr.ibyte.bit2 = ip1.ibyte.bit7;
		ip1.setAbyte(rin[0]);
		ipr.ibyte.bit1 = ip1.ibyte.bit7;
		ip1.setAbyte(lin[0]);
		ipr.ibyte.bit0 = ip1.ibyte.bit7;
		ip[0] = ipr.getAbyte();
		m[7] = ip[7];
		m[6] = ip[6];
		m[5] = ip[5];
		m[4] = ip[4];
		m[3] = ip[3];
		m[2] = ip[2];
		m[1] = ip[1];
		m[0] = ip[0];
	}

	public static final int MAX_CI_LEN = 1024;
}
