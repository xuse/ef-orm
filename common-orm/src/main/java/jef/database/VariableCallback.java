package jef.database;


import java.util.List;

import jef.database.Condition.Operator;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.meta.Feature;
import jef.database.meta.ITableMetadata;
import jef.database.query.SqlContext;
import jef.database.wrapper.processor.BindVariableDescription;
import jef.tools.StringUtils;

/**
 * 描述提供值修正的回调 实现
 */
public interface VariableCallback {
	Object process(Object result);

	public static class Like implements VariableCallback {
		public static final Operator[] LIKE_OPERATORS={Operator.MATCH_ANY,Operator.MATCH_END,Operator.MATCH_START};
		private static final String ESCAPE_CLAUSE = " escape '/' ";
		private Object value;
		private Operator operator;
		private boolean escape;// 仅当非绑定模式下，表示数值需要转义
		private Field field;
		// 转义替换列表，注意:/的替换一定要最先进行
		private static final String[] replaceFrom = new String[] { "/", "%",
				"_" };
		private static final String[] replaceTo = new String[] { "//", "/%",
				"/_" };

		public String name() {
			return " like ";
		}

		// 构造和初始化计算
		public Like(Field field, Operator oper, Object value) {
			this.field = field;
			this.operator = oper;
			this.value = value;
		}

		// 转义、并且根据运算符加上前后百分号
		public Object process(Object data) {
			if(data instanceof Expression){
				return data.toString();
			}
			String valueStr = StringUtils.toString(data);
			if (escape) {
				valueStr = StringUtils.replaceEach(valueStr, replaceFrom,
						replaceTo);
			}
			if (valueStr.length() == 0) {
				return "%";
			}

			if (operator == Operator.MATCH_ANY) {
				return new StringBuilder(valueStr.length() + 2).append('%')
						.append(valueStr).append('%').toString();
			} else if (operator == Operator.MATCH_END) {
				return "%".concat(valueStr);
			} else if (operator == Operator.MATCH_START) {
				return valueStr.concat("%");
			} else {
				throw new RuntimeException("Operator is not a like operation:"
						+ operator.name());
			}
		}

		public String toPrepareSql(List<BindVariableDescription> fields,
				ITableMetadata meta, DatabaseDialect dialect, SqlContext context,
				IQueryableEntity instance) {
			// 只要使用了绑定变量方式获取，那么一定要做转义
			escape = !dialect.has(
					Feature.NOT_SUPPORT_LIKE_ESCAPE);

			StringBuilder sb = new StringBuilder();
			String alias = context == null ? null : context.getCurrentAliasAndCheck(field);
			String columnName = DbUtils.toColumnName(field, dialect, alias);
			sb.append(columnName).append(name()).append("?");
			if (escape) {
				sb.append(ESCAPE_CLAUSE);
			}
			BindVariableDescription bind = new BindVariableDescription(field,
					operator, value);
			bind.setCallback(this);
			fields.add(bind);
			return sb.toString();
		}

		public String toSql(ITableMetadata meta, DatabaseDialect dialect,
				SqlContext context, IQueryableEntity instance) {
			String valueStr = StringUtils.toString(value);
			if(!(value instanceof Expression)){
				if (valueStr.indexOf('%') > -1 || valueStr.indexOf('_') > -1) {
					escape = true;
				}
				valueStr = (String) process(valueStr);
			}
			
			StringBuilder sb = new StringBuilder();
			String alias = context == null ? null : context.getCurrentAliasAndCheck(field);
			String columnName = DbUtils.toColumnName(field,dialect,alias);
			sb.append(columnName).append(name());
			sb.append('\'');
			sb.append(valueStr);
			sb.append('\'');
			if (escape) {
				sb.append(ESCAPE_CLAUSE);
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			return field.name() + " " + operator.getKey();
		}
	}
}
