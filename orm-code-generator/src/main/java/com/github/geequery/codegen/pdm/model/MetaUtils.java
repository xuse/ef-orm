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

package com.github.geequery.codegen.pdm.model;


/**
 *
 * @author <a href="mailto:nikola.petkov@gmail.com">Nikola Petkov &lt;nikola.petkov@gmail.com&gt;</a>
 */
public class MetaUtils {
    private MetaUtils() {
    }

    /**
     * 
     * @param aStringToFormat
     * @param aBreakOn
     * @return formatted html string with breaks
     */
    public static String insertHTMLBreaks(String aStringToFormat, int aBreakOn) {
        String retVal = new String();
        int start = 0, end = 0;
        final int len = aStringToFormat.length();
        final int lastIndex = (len == 0) ? 0 : len - 1;
        while (true) {
            start = end;
            end += aBreakOn;
            if (end >= lastIndex) {
                end = lastIndex;
                retVal += aStringToFormat.subSequence(start, end);
                break;
            }
            retVal += aStringToFormat.subSequence(start, end) + "<BR>";
        }
        return retVal;
    }

}
