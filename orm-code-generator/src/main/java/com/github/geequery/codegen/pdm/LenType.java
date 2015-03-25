/* 
 * This file is part of Mosquito meta-loader.
 *
 * Mosquito meta-loader is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mosquito meta-loader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.geequery.codegen.pdm;

/**
 * @author <a href="mailto:nikola.petkov@gmail.com">Nikola Petkov &lt;nikola.petkov@gmail.com&gt;</a>
 */
public class LenType {

    private String jClass;

    private int length = -1;
    /**
     * 
     */
    public LenType() {
    }

    /**
     * @param aLength
     * @param aJClass
     */
    public LenType(int aLength, String aJClass) {
        length = aLength;
        jClass = aJClass;
    }

    /**
     * @return type.
     */
    public String getJClass() {
        return jClass;
    }

    /**
     * @return len.
     */
    public int getLength() {
        return length;
    }

    /**
     * @param aType
     *            The type to set.
     */
    public void setJClass(String aType) {
        jClass = aType;
    }

    /**
     * @param aLength
     *            The len to set.
     */
    public void setLength(int aLength) {
        length = aLength;
    }
}
