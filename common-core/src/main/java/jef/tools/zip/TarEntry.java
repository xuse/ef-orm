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
package jef.tools.zip;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Locale;

//Referenced classes of package org.apache.tools.tar:
//         TarConstants, TarUtils
public class TarEntry implements TarConstants {
	public static String DEFAULT_NAME_ENCODING = Charset.defaultCharset().name();
	
	private TarEntry() {
		magic = new StringBuffer("ustar");
		name = new StringBuffer();
		linkName = new StringBuffer();
		String user = System.getProperty("user.name", "");
		if (user.length() > 31)
			user = user.substring(0, 31);
		userId = 0;
		groupId = 0;
		userName = new StringBuffer(user);
		groupName = new StringBuffer("");
		file = null;
	}

	public TarEntry(String name) {
		this();
		boolean isDir = name.endsWith("/");
		devMajor = 0;
		devMinor = 0;
		this.name = new StringBuffer(name);
		mode = isDir ? 16877 : 33188;
		linkFlag = ((byte) (isDir ? 53 : 48));
		userId = 0;
		groupId = 0;
		size = 0L;
		modTime = (new Date()).getTime() / 1000L;
		linkName = new StringBuffer("");
		userName = new StringBuffer("");
		groupName = new StringBuffer("");
		devMajor = 0;
		devMinor = 0;
	}

	public TarEntry(String name, byte linkFlag) {
		this(name);
		this.linkFlag = linkFlag;
	}

	public TarEntry(File file) {
		this();
		this.file = file;
		String fileName = file.getPath();
		String osname = System.getProperty("os.name").toLowerCase(Locale.US);
		if (osname != null)
			if (osname.startsWith("windows")) {
				if (fileName.length() > 2) {
					char ch1 = fileName.charAt(0);
					char ch2 = fileName.charAt(1);
					if (ch2 == ':' && (ch1 >= 'a' && ch1 <= 'z' || ch1 >= 'A' && ch1 <= 'Z'))
						fileName = fileName.substring(2);
				}
			} else if (osname.indexOf("netware") > -1) {
				int colon = fileName.indexOf(':');
				if (colon != -1)
					fileName = fileName.substring(colon + 1);
			}
		for (fileName = fileName.replace(File.separatorChar, '/'); fileName.startsWith("/"); fileName = fileName.substring(1));
		linkName = new StringBuffer("");
		name = new StringBuilder(fileName);
		if (file.isDirectory()) {
			mode = 16877;
			linkFlag = 53;
			if (name.charAt(name.length() - 1) != '/')
				((StringBuilder)name).append("/");
		} else {
			mode = 33188;
			linkFlag = 48;
		}
		size = file.length();
		modTime = file.lastModified() / 1000L;
		devMajor = 0;
		devMinor = 0;
	}

	public TarEntry(byte headerBuf[]) {
		this();
		parseTarHeader(headerBuf);
	}

	public boolean equals(TarEntry it) {
		return getName().equals(it.getName());
	}

	public boolean equals(Object it) {
		if (it == null || getClass() != it.getClass())
			return false;
		else
			return equals((TarEntry) it);
	}

	public int hashCode() {
		return getName().hashCode();
	}

	public boolean isDescendent(TarEntry desc) {
		return desc.getName().startsWith(getName());
	}

	public String getName() {
		return name.toString();
	}

	public void setName(String name) {
		this.name = new StringBuffer(name);
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public String getLinkName() {
		return linkName.toString();
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public String getUserName() {
		return userName.toString();
	}

	public void setUserName(String userName) {
		this.userName = new StringBuffer(userName);
	}

	public String getGroupName() {
		return groupName.toString();
	}

	public void setGroupName(String groupName) {
		this.groupName = new StringBuffer(groupName);
	}

	public void setIds(int userId, int groupId) {
		setUserId(userId);
		setGroupId(groupId);
	}

	public void setNames(String userName, String groupName) {
		setUserName(userName);
		setGroupName(groupName);
	}

	public void setModTime(long time) {
		modTime = time / 1000L;
	}

	public void setModTime(Date time) {
		modTime = time.getTime() / 1000L;
	}

	public Date getModTime() {
		return new Date(modTime * 1000L);
	}

	public File getFile() {
		return file;
	}

	public int getMode() {
		return mode;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public boolean isGNULongNameEntry() {
		return linkFlag == 76 && name.toString().equals("././@LongLink");
	}

	public boolean isDirectory() {
		if (file != null)
			return file.isDirectory();
		if (linkFlag == 53)
			return true;
		return getName().endsWith("/");
	}

	public TarEntry[] getDirectoryEntries() {
		if (file == null || !file.isDirectory())
			return new TarEntry[0];
		String list[] = file.list();
		TarEntry result[] = new TarEntry[list.length];
		for (int i = 0; i < list.length; i++)
			result[i] = new TarEntry(new File(file, list[i]));
		return result;
	}

	public void writeEntryHeader(byte outbuf[]) throws UnsupportedEncodingException {
		int offset = 0;
		offset = TarUtils.getNameBytes(name,  DEFAULT_NAME_ENCODING,outbuf, offset, 100);
		offset = TarUtils.getOctalBytes(mode, outbuf, offset, 8);
		offset = TarUtils.getOctalBytes(userId, outbuf, offset, 8);
		offset = TarUtils.getOctalBytes(groupId, outbuf, offset, 8);
		offset = TarUtils.getLongOctalBytes(size, outbuf, offset, 12);
		offset = TarUtils.getLongOctalBytes(modTime, outbuf, offset, 12);
		int csOffset = offset;
		for (int c = 0; c < 8; c++)
			outbuf[offset++] = 32;
		outbuf[offset++] = linkFlag;
		offset = TarUtils.getNameBytes(linkName, DEFAULT_NAME_ENCODING,outbuf, offset, 100);
		offset = TarUtils.getNameBytes(magic, DEFAULT_NAME_ENCODING,outbuf, offset, 8);
		offset = TarUtils.getNameBytes(userName, DEFAULT_NAME_ENCODING,outbuf, offset, 32);
		offset = TarUtils.getNameBytes(groupName, DEFAULT_NAME_ENCODING,outbuf, offset, 32);
		offset = TarUtils.getOctalBytes(devMajor, outbuf, offset, 8);
		for (offset = TarUtils.getOctalBytes(devMinor, outbuf, offset, 8); offset < outbuf.length;)
			outbuf[offset++] = 0;
		long chk = TarUtils.computeCheckSum(outbuf);
		TarUtils.getCheckSumOctalBytes(chk, outbuf, csOffset, 8);
	}

	public void parseTarHeader(byte header[]) {
		int offset = 0;
		name = TarUtils.parseName(header, offset, 100);
		offset += 100;
		mode = (int) TarUtils.parseOctal(header, offset, 8);
		offset += 8;
		userId = (int) TarUtils.parseOctal(header, offset, 8);
		offset += 8;
		groupId = (int) TarUtils.parseOctal(header, offset, 8);
		offset += 8;
		size = TarUtils.parseOctal(header, offset, 12);
		offset += 12;
		modTime = TarUtils.parseOctal(header, offset, 12);
		offset += 12;
		offset += 8;
		linkFlag = header[offset++];
		linkName = TarUtils.parseName(header, offset, 100);
		offset += 100;
		magic = TarUtils.parseName(header, offset, 8);
		offset += 8;
		userName = TarUtils.parseName(header, offset, 32);
		offset += 32;
		groupName = TarUtils.parseName(header, offset, 32);
		offset += 32;
		devMajor = (int) TarUtils.parseOctal(header, offset, 8);
		offset += 8;
		devMinor = (int) TarUtils.parseOctal(header, offset, 8);
	}
	private CharSequence name;
	private int mode;
	private int userId;
	private int groupId;
	private long size;
	private long modTime;
	private byte linkFlag;
	private CharSequence linkName;
	private CharSequence magic;
	private CharSequence userName;
	private CharSequence groupName;
	private int devMajor;
	private int devMinor;
	private File file;
	public static final int MAX_NAMELEN = 31;
	public static final int DEFAULT_DIR_MODE = 16877;
	public static final int DEFAULT_FILE_MODE = 33188;
	public static final int MILLIS_PER_SECOND = 1000;
}
