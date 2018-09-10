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
package jef.database.dialect;

import jef.database.query.function.StandardSQLFunction;

public class MySQL8Dialect extends MySQL57Dialect {

	@Override
	public String getDriverClass(String url) {
		return "com.mysql.cj.jdbc.Driver";
	}

	@Override
	protected void initKeywods() {
		loadKeywords("mysql8_keywords.properties");
	}

	@Override
	protected void initFunctions() {
		super.initFunctions();
		registerNative(new StandardSQLFunction("cume_dist"));
		registerNative(new StandardSQLFunction("cume_dist"));
		registerNative(new StandardSQLFunction("dense_rank"));
		registerNative(new StandardSQLFunction("first_value"));
		registerNative(new StandardSQLFunction("lag"));
		registerNative(new StandardSQLFunction("last_value"));
		registerNative(new StandardSQLFunction("lead"));
		registerNative(new StandardSQLFunction("nth_value"));
		registerNative(new StandardSQLFunction("ntile"));
		registerNative(new StandardSQLFunction("percent_rank"));
		registerNative(new StandardSQLFunction("rank"));
		registerNative(new StandardSQLFunction("row_number"));
	}

}
