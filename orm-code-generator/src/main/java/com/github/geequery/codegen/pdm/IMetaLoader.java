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

import java.io.File;

import com.github.geequery.codegen.pdm.model.MetaModel;
import jef.common.JefException;

/**
 * Creates and loads meta-model (<code>com.github.geequery.codegen.pdm.model.MetaModel</code>).
 * 
 * @author <a href="mailto:nikola.petkov@gmail.com">Nikola Petkov
 *         &lt;nikola.petkov@gmail.com&gt;</a>
 */
public interface IMetaLoader {

	/**
	 * Creates meta model and returns reference to it.
	 * 
	 * @param config
	 *            Configuration properties that may be diffrent for each
	 *            implementation. Every implementation should have its own
	 *            subset of properties and it should be documented under the
	 *            implementation documentation.
	 * 
	 * @return Newly created and loaded meta-model.
	 * @throws LoadingException.
	 */
	/**
	 * @return
	 * @throws LoadingException
	 */
	MetaModel getMetaModel(File file,String... args) throws JefException;

}
