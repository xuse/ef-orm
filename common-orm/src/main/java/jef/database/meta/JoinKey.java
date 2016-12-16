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
package jef.database.meta;

import jef.database.Condition;
import jef.database.DbUtils;
import jef.database.Field;
import jef.database.MetadataContainer;
import jef.database.QueryAlias;
import jef.database.query.Join;
import jef.database.query.JoinElement;
import jef.database.query.JpqlExpression;
import jef.database.query.LazyQueryBindField;
import jef.database.query.Query;
import jef.database.query.RefField;

/**
 * 描述一个连接键对
 * 
 * @author Administrator
 *
 */
public class JoinKey extends Condition {
	private static final long serialVersionUID = 1L;

	private boolean isValueField; // 右侧是Field
	private boolean isSimple; // 简单模式

	private Query<?> leftLock; // 左侧固定查询对象
	private Query<?> rightLock; // 右侧固定查询对象
	// 左右翻转

	public JoinKey flip() {
		if (isSimple) {
			JoinKey r = new JoinKey((Field) this.value, this.operator, this.field);
			r.rightLock = this.leftLock;
			r.leftLock = this.rightLock;
			return r;
		} else {
			return this;
		}
	}

	public Query<?> getLeftLock() {
		return leftLock;
	}

	public Query<?> getRightLock() {
		return rightLock;
	}

	/**
	 * 给定Field和外连接信息，构造连接
	 * 
	 * @param left
	 * @param right
	 */
	public JoinKey(Field left, Field right) {
		this(left, Operator.EQUALS, right);
	}

	public JoinKey(Field field, Operator operator, Object value) {
		this.field = field;
		this.value = value;
		this.operator = operator;
		init();
	}

	private void init() {
		// 处理左侧锁定
		if (field instanceof RefField) {
			if (((RefField) field).isBind()) {
				this.leftLock = ((RefField) field).getInstanceQuery(null);
			}
			field = ((RefField) field).getField();
		} else if (field instanceof JpqlExpression) {
			JpqlExpression jpql = (JpqlExpression) field;
			if (jpql.isBind()) {
				this.leftLock = jpql.getInstanceQuery(null);
			}
		}

		isValueField = (value instanceof Field);
		if (isValueField) {
			// 处理右侧锁定
			if (value instanceof RefField) {
				if (((RefField) value).isBind()) {
					this.rightLock = ((RefField) value).getInstanceQuery(null);
				}
				value = ((RefField) value).getField();
			} else if (value instanceof JpqlExpression) {
				if (((JpqlExpression) value).isBind()) {
					this.rightLock = ((JpqlExpression) value).getInstanceQuery(null);
				}
			}
			if (!(field instanceof JpqlExpression)) {
				if ((value instanceof Enum || value instanceof TupleField) && Operator.EQUALS == operator) {
					isSimple = true;
				}
			}
		}
	}

	public boolean isValueField() {
		return isValueField;
	}

	public boolean isSimple() {
		return isSimple;
	}

	public Field getLeft() {
		return field;
	}

	/**
	 * 得到Join关系右边的字段
	 * 
	 * @return
	 */
	public Field getRightAsField() {
		if (isValueField) {
			return (Field) value;
		} else {
			return null;
		}
	}

	public ITableMetadata getLeftTableMeta() {
		if (leftLock != null) {
			return leftLock.getMeta();
		}
		return DbUtils.getTableMeta(this.field);
	}

	public ITableMetadata getRightTableMeta() {
		if (rightLock != null) {
			return rightLock.getMeta();
		}
		ITableMetadata meta = null;
		if (this.value instanceof LazyQueryBindField) {
			meta = ((LazyQueryBindField) value).getMeta();
		} else if (value instanceof Field) {
			meta = DbUtils.getTableMeta((Field) value);
		}
		return meta;
	}

	public void findAndLockLeft(Join left) {
		if (leftLock != null)
			return;
		ITableMetadata meta = DbUtils.getTableMeta(this.field);
		if (meta == null)
			return;
		for (QueryAlias qa : ((Join) left).allElements()) {
			if (qa.getQuery().getMeta() == meta) {
				this.leftLock = qa.getQuery();
				return;
			}
		}
	}

	public int validate(JoinElement left, Query<?> obj) {
		if (validateLeft(left)) {
			return validateRight(obj) ? 1 : 0;
		}
		if (validateLeft(obj)) {
			return (validateRight(left) || validateRight(obj)) ? -1 : 0;
		}
		return 0;
	}

	public int validate(ITableMetadata left, ITableMetadata obj) {
		if (validateLeft(left)) {
			return validateRight(obj) ? 1 : 0;
		}
		if (validateLeft(obj)) {
			return (validateRight(left) || validateRight(obj)) ? -1 : 0;
		}
		return 0;
	}

	private boolean validateRight(ITableMetadata obj) {
		if (rightLock != null) {
			return obj == rightLock.getMeta();
		} else {
			ITableMetadata meta = null;
			if (this.value instanceof MetadataContainer) {
				meta = ((MetadataContainer) value).getMeta();
			} else if (value instanceof Field && value instanceof Enum) {
				meta = DbUtils.getTableMeta((Field) value);
			}
			return meta==null || meta==obj || meta.getThisType().isAssignableFrom(obj.getThisType());
		}
	}

	private boolean validateLeft(ITableMetadata left) {
		if (leftLock != null) {
			return left == leftLock.getMeta();
		} else if (field == null) {// 纯表达式
			return true;
		} else {
			// 这里从Field获得meta,但有风险，有可能会得到实际用的left的一个父类Meta
			ITableMetadata meta = DbUtils.getTableMeta(this.field);
			return meta == null || meta == left || meta.getThisType().isAssignableFrom(left.getThisType());
		}
	}

	private boolean validateLeft(JoinElement q) {
		if (leftLock != null) {
			if (q == leftLock) {
				return true;
			}
			if (q instanceof Join) {
				for (QueryAlias qa : ((Join) q).allElements()) {
					if (qa.getQuery() == leftLock) {
						return true;
					}
				}
			}
		} else if (field == null) {// 纯表达式
			return true;
		} else {
			// 这里从Field获得meta,但有风险，实际用的meta可能是这个meta的子类。
			ITableMetadata meta = DbUtils.getTableMeta(this.field);
			if (meta == null)
				return true;
			if (q instanceof Query<?>) {
				return isMatch(meta, ((Query<?>) q).getMeta());
			}
			if (q instanceof Join) {
				for (QueryAlias qa : ((Join) q).allElements()) {
					if (isMatch(meta, qa.getQuery().getMeta())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isMatch(ITableMetadata meta, ITableMetadata meta2) {
		if (meta == meta2)
			return true;
		return meta.getThisType().isAssignableFrom(meta2.getThisType());
	}

	private boolean validateRight(JoinElement q) {
		if (rightLock != null) {
			if (q == rightLock) {
				return true;
			}
			if (q instanceof Join) {
				for (QueryAlias qa : ((Join) q).allElements()) {
					if (qa.getQuery() == rightLock) {
						return true;
					}
				}
			}
		} else {
			ITableMetadata meta = null;
			if (this.value instanceof MetadataContainer) {
				meta = ((MetadataContainer) value).getMeta();
			} else if (value instanceof Field && value instanceof Enum) {
				meta = DbUtils.getTableMeta((Field) value);
			}
			if (meta == null)
				return true;
			if (q instanceof Query<?>) {
				return isMatch(meta, ((Query<?>) q).getMeta());
			}
			if (q instanceof Join) {
				for (QueryAlias qa : ((Join) q).allElements()) {
					if (isMatch(meta, qa.getQuery().getMeta())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
