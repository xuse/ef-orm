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

import java.util.Arrays;
import java.util.Set;

import jef.database.annotation.DateGenerateType;
import jef.database.dialect.ColumnType.AutoIncrement;
import jef.database.meta.Feature;
import jef.database.query.Func;
import jef.tools.collection.CollectionUtils;

/**
 * MySQL 特性
 * <p>
 * <ol>
 * <li>自增特性 AUTO_INCREMENT 表示自增 SELECT LAST_INSERT_ID();获取自增量值 SELECT @@IDENTITY
 * 获取新分配的自增量值</li>
 * 
 * <li>支持无符号数、支持BLOB，支持TEXT。数据类型较多。但是考虑兼容性，很多都不在本框架使用</li>
 * 
 * <li>MYSQL VARCHAR长度 最大长度65535，在utf8编码时最大65535/3=21785，GBK则是65535/2.
 * 但是用varchar做主键的长度是受MYSQL索引限制的。 MYSQL索引只能索引768个字节。（过去某个版本好像是1000字节）
 * 当数据库默认使用GBK编码时。这个长度是383. UTF8这个是255，latin5是767. （建表时可以在尾部加上charset=latin5;
 * 来指定表的语言。） <br>
 * 因此在MYSQL中不能将长度超过这个限制的字段设置为主键。一般来说设置varchar也就到255，这是比较安全的。</li>
 * 
 * <li>MY SQL中对不同大小的文有多种类型
 * <ul>
 * <li>TINYTEXT，最大长度为255，占用空间也是(实际长度+1)；</li>
 * <li>TEXT，最大长度65535，占用空间是(实际长度+2)；</li>
 * <li>MEDIUMTEXT，最大长度16777215，占用空间是(实际长度+3)；</li>
 * <li>LONGTEXT，最大长度4294967295，占用空间是(实际长度+4)。</li>
 * </ul>
 * 
 * <li>BLOB和TEXT 作为主键、或者建立索引时，需要指定索引长度，限制见上。</li>
 * 
 * <li>日期DATE、TIME、DATETIME、TIMESTAMP和YEAR等。</li>
 * </ol>
 * 
 * <p>
 * 常用命令
 * <ol>
 * <li>查看表的所有信息：show create table 表名;</li>
 * <li>添加主键约束：alter table 表名 add constraint 主键 （形如：PK_表名） primary key 表名(主键字段);
 * <li>添加外键约束：alter table 从表 add constraint 外键（形如：FK_从表_主表） foreign key 从表(外键字段)
 * references 主表(主键字段);
 * <li>删除主键约束：alter table 表名 drop primary key;</li>
 * <li>删除外键约束：alter table 表名 drop foreign key 外键（区分大小写）;</li>
 * <li>修改表名： alter table t_book rename to bbb;</li>
 * <li>添加列： alter table 表名 add column 列名 varchar(30);</li>
 * <li>删除列： alter table 表名 drop column 列名;</li>
 * <li>修改列名： alter table bbb change nnnnn hh int;</li>
 * <li>修改列属性：alter table t_book modify name varchar(22);</li>
 * </ol>
 * 
 * MySQL的四种BLOB类型 类型 大小(单位：字节) TinyBlob 最大 255 Blob 最大 65K MediumBlob 最大 16M
 * LongBlob 最大 4G
 * 
 * 
 * 遗留问题：MySQL能不能做成表名大小写不敏感的。目前是敏感的。这造成大小写不一致会认为是两张表。
 */
public class MySQL55Dialect extends MySQL57Dialect {

	@Override
	protected void initFeatures() {
		Set<Feature> features = CollectionUtils.identityHashSet();
		features.addAll(Arrays.asList(Feature.DBNAME_AS_SCHEMA, Feature.SUPPORT_INLINE_COMMENT, Feature.ALTER_FOR_EACH_COLUMN, Feature.NOT_FETCH_NEXT_AUTOINCREAMENTD, Feature.SUPPORT_LIMIT, Feature.COLUMN_DEF_ALLOW_NULL, Feature.DATE_TIME_VALUE_WITHOUT_DEFAULT_FUNC));
		this.features=features;
	}

	/*
	 * 
	 * 关于MySQL的Auto_increament访问是比较复杂的 SELECT `AUTO_INCREMENT` FROM
	 * INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'DatabaseName' AND
	 * TABLE_NAME = 'TableName';
	 * 
	 * 
	 * $result = mysql_query("SHOW TABLE STATUS LIKE 'table_name'"); $row =
	 * mysql_fetch_array($result); $nextId = $row['Auto_increment'];
	 * mysql_free_result($result);
	 * 
	 * 
	 * 
	 * down vote accepted Use this:
	 * 
	 * ALTER TABLE users AUTO_INCREMENT = 1001;or if you haven't already id
	 * column, also add it
	 * 
	 * ALTER TABLE users ADD id INT UNSIGNED NOT NULL AUTO_INCREMENT, ADD INDEX
	 * (id);
	 */
	@Override
	public String getCreationComment(ColumnType column, boolean flag) {
		DateGenerateType generateType = null;
		if (column instanceof SqlTypeDateTimeGenerated) {
			generateType = ((SqlTypeDateTimeGenerated) column).getGenerateType();
			Object defaultValue = column.defaultValue;
			/*
			 * 根据用户设置的defaultValue进行修正
			 */
			if (generateType == null && (defaultValue == Func.current_date || defaultValue == Func.current_time || defaultValue == Func.now)) {
				generateType = DateGenerateType.created;
			}
			if (generateType == null && defaultValue != null) {
				String dStr = defaultValue.toString().toLowerCase();
				if (dStr.startsWith("current") || dStr.startsWith("sys")) {
					generateType = DateGenerateType.created;
				}
			}
		}
		if (generateType == DateGenerateType.created) {
			return "datetime NOT NULL";
		} else if (generateType == DateGenerateType.modified) {
			return "timestamp NOT NULL DEFAULT current_timestamp ON UPDATE current_timestamp";
		}
		return super.getCreationComment(column, flag);
	}

	protected String getComment(AutoIncrement column, boolean flag) {
		StringBuilder sb = new StringBuilder();
		// sb.append("INT UNSIGNED");
		// 2016-4-19日从 int unsigned改为int，因为当一个表主键被另一个表作为外键引用时，双方类型必须完全一样。
		// 实际测试发现，由于一般建表时普通int字段不会处理为 int unsigned，造成外键创建失败。所以此处暂时为int
		sb.append("INT ");
		if (flag) {
			if (!column.nullable)
				sb.append(" NOT NULL");
		}
		sb.append(" AUTO_INCREMENT");
		return sb.toString();
	}
}
