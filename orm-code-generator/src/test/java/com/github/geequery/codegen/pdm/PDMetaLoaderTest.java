/* 
 * This file is part of Mosquito meta-loader.
 *
 * Mosquito meta-loader is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mosquito meta-loader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.geequery.codegen.pdm;

import java.io.File;
import java.util.List;

import jef.common.JefException;
import jef.common.log.LogUtil;
import junit.framework.Assert;

import org.junit.Test;

import com.github.geequery.codegen.pdm.IMetaLoader;
import com.github.geequery.codegen.pdm.PDMetaLoader;
import com.github.geequery.codegen.pdm.model.MetaColumn;
import com.github.geequery.codegen.pdm.model.MetaModel;
import com.github.geequery.codegen.pdm.model.MetaTable;


public class PDMetaLoaderTest {
	
	private File createNonExistingFileProperties() {
		return new File("THIS IS NON EXISTING FILE");
	}

	private File createProperties() {
		return new File("src/test/resources/meta-test-v12.pdm");
	}
	

	@Test
	public void getMetaModel() throws Exception {
		IMetaLoader metaLoader = new PDMetaLoader();

		MetaModel model = metaLoader.getMetaModel(createProperties());
		LogUtil.show("Meta model successfully retrieved!");

		Assert.assertEquals("META", model.getCode());
		Assert.assertEquals(18, model.getAllColumnCount());
		Assert.assertEquals("", model.getComment());
		Assert.assertEquals("meta", model.getName());
		Assert.assertEquals(5, model.getTableCount());

		LogUtil.show("Asserting meta model: " + model.getCode());
		
		for (MetaTable table : model.getTables()) {
			String tableCode = table.getCode();
			
			assertTable(tableCode, model);

			Assert.assertNotNull(model.getTable(tableCode));
		}
		
	}
	
	@Test
	public void getMetaModel2() throws Exception {
		IMetaLoader metaLoader = new PDMetaLoader();

		MetaModel model = metaLoader.getMetaModel(new File("C:/Users/jiyi/Desktop/CMDB.pdm"));
		LogUtil.show("Meta model successfully retrieved!");

	

		for (MetaTable table : model.getTables()) {
			String tableCode = table.getCode();
			if("UAM_ALARM_INFO".equalsIgnoreCase(tableCode)){
				System.out.println(table);
				List<MetaColumn> cs=table.getColumns();
				LogUtil.show(cs);
			}
		}
		
	}
	

	@Test(expected = JefException.class)
	public void notSuchFileGetMetamodel() throws JefException {
		IMetaLoader metaLoader = new PDMetaLoader();
		metaLoader.getMetaModel(createNonExistingFileProperties());
	}
	
	private void assertTable(String tableCode, MetaModel model) {
		
		LogUtil.show("    Asserting table[code]: " + tableCode);
		
		MetaTable table = model.getTable(tableCode);
		Assert.assertNotNull(table);	
		
		
		for (MetaColumn column : table) {
			LogUtil.show("        Column [code]: " + column.getCode());
			LogUtil.show("            - name: " + column.getName());
			LogUtil.show("            - type: " + column.getType());
			LogUtil.show("            - length: " + column.getLength());
			LogUtil.show("            - precision: " + column.getPrecision());
			LogUtil.show("            - comment: " + column.getComment());
			LogUtil.show("            - defaultVal: " + column.getDefaultVal());
			
			Assert.assertSame(column, table.getColumnByCode(column.getCode()));
		}
	}
}
