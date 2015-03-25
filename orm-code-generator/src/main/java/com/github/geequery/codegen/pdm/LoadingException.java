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

/*
 * Created on Jan 9, 2005 at 6:19:12 PM
 */
/**
 * Loading exception could be thrown while the loading of meta data is in the
 * progress.
 * 
 * @author <a href="mailto:nikola.petkov@gmail.com">Nikola Petkov &lt;nikola.petkov@gmail.com&gt;</a>
 */
public class LoadingException extends Exception {
    final static long serialVersionUID = 0L;

    /**
     * @param message
     */
    public LoadingException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public LoadingException(Throwable cause) {
        super(cause);
    }
}
