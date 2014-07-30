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

public interface TarConstants {
	public static final int NAMELEN = 100;
	public static final int MODELEN = 8;
	public static final int UIDLEN = 8;
	public static final int GIDLEN = 8;
	public static final int CHKSUMLEN = 8;
	public static final int SIZELEN = 12;
	public static final long MAXSIZE = 8589934591L;
	public static final int MAGICLEN = 8;
	public static final int MODTIMELEN = 12;
	public static final int UNAMELEN = 32;
	public static final int GNAMELEN = 32;
	public static final int DEVLEN = 8;
	public static final byte LF_OLDNORM = 0;
	public static final byte LF_NORMAL = 48;
	public static final byte LF_LINK = 49;
	public static final byte LF_SYMLINK = 50;
	public static final byte LF_CHR = 51;
	public static final byte LF_BLK = 52;
	public static final byte LF_DIR = 53;
	public static final byte LF_FIFO = 54;
	public static final byte LF_CONTIG = 55;
	public static final String TMAGIC = "ustar";
	public static final String GNU_TMAGIC = "ustar  ";
	public static final String GNU_LONGLINK = "././@LongLink";
	public static final byte LF_GNUTYPE_LONGNAME = 76;
}
