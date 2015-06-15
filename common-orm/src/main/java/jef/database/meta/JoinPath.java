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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.OrderBy;

import jef.database.annotation.JoinDescription;
import jef.database.annotation.JoinType;
import jef.database.query.Join;
import jef.database.query.JoinElement;
import jef.database.query.Query;
import jef.tools.ArrayUtils;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * 描述两表间的一组关系
 * 
 * @author Administrator
 * 
 */
public class JoinPath {
	private static final JoinKey[] EMPTY = new JoinKey[0];
	private JoinKey[] joinKeys;
	private JoinKey[] joinExpression;
	private JoinType type;
	private JoinDescription description;
	private OrderBy orderBy;

	/**
	 * ManyToMany时三表连接
	 */
	private TupleMetadata relationTable;
	private JoinPath relationToTarget;	

	public TupleMetadata getRelationTable() {
		return relationTable;
	}


	public JoinPath getRelationToTarget() {
		return relationToTarget;
	}
	public void setRelationTable(TupleMetadata relationTable,JoinPath toTarget) {
		this.relationTable = relationTable;
		this.relationToTarget=toTarget;
	}

	public JoinDescription getDescription() {
		return description;
	}

	public void setDescription(JoinDescription description, OrderBy orderBy) {
		this.description = description;
		this.orderBy = orderBy;
	}

	public JoinKey[] getJoinKeys() {
		if (joinKeys == null)
			return EMPTY;
		return joinKeys;
	}

	public JoinKey[] getJoinExpression() {
		if (joinExpression == null)
			return EMPTY;
		return joinExpression;
	}

	public void addJoinKey(JoinKey s) {
		if (joinKeys == null) {
			joinKeys = new JoinKey[] { s };
		} else {
			joinKeys = (JoinKey[]) ArrayUtils.add(joinKeys, s);
		}
		this.flip = null;
		check();
	}

	public void setJoinKeys(JoinKey[] joinKeys) {
		this.joinKeys = joinKeys;
		check();
		this.flip = null;
	}

	public void setType(JoinType type) {
		this.type = type;
	}

	public JoinType getType() {
		return type;
	}

	private void check() {
		// 整理并移动
		if (joinKeys.length == 0)
			return;
		List<JoinKey> moved = new ArrayList<JoinKey>();
		for (JoinKey s : this.joinKeys) {
			if (!s.isSimple()) {
				moved.add(s);
			}
		}
		for (JoinKey s : moved) {
			joinKeys = (JoinKey[]) ArrayUtils.removeElement(joinKeys, s);
			joinExpression = ArrayUtils.addElement(joinExpression, s);
		}
	}

	public JoinPath(JoinType type, JoinKey... relationships) {
		this.type = type;
		joinKeys = relationships;
		check();
	}

	JoinPath() {
	};

	// 其实就一个join条件表达式来说，颠倒与否没有多大的实际意义。纯粹视觉效果。
	private JoinPath flip;

	public JoinPath flip() {
		if (flip == null) {
			JoinPath flipObj = new JoinPath();
			flipObj.joinKeys = new JoinKey[this.joinKeys.length];
			flipObj.type = JoinType.flip(type);
			for (int n = 0; n < this.joinKeys.length; n++) {
				flipObj.joinKeys[n] = joinKeys[n].flip();
			}
			flipObj.flip = this;
			this.flip = flipObj;
		}
		return flip;
	}

	/**
	 * 这个方法用来检测当前连接路径是否满足left,right的连接需要
	 * 
	 * 要注意 1、这个方法只需对用户输入的条件进行检查，不能对系统自身Reference中存在的对象进行绑定操作
	 * 
	 * @param left
	 * @param obj
	 * @return
	 */
	public JoinPath accept(JoinElement left, Query<?> obj) {
		if (relationTable != null) {
			return pathAccept(obj);
		}

		if (joinKeys != null) {
			for (int i = 0; i < joinKeys.length; i++) {
				JoinKey jk = joinKeys[i];
				int flag = jk.validate(left, obj);
				if (flag == 0) {
					return null;
				} else if (flag == -1) {
					joinKeys[i] = jk.flip();
				}

			}
		}
		if (joinExpression != null) {
			for (int i = 0; i < joinExpression.length; i++) {
				JoinKey jk = joinExpression[i];
				int flag = jk.validate(left, obj);
				if (flag == 0) {
					return null;
				} else if (flag == -1) {
					joinExpression[i] = jk.flip();
				}
			}
		}

		if (left instanceof Join) {
			Join j = (Join) left;
			if (joinKeys != null) {
				for (int i = 0; i < joinKeys.length; i++) {
					JoinKey jk = joinKeys[i];
					jk.findAndLockLeft(j);
				}
			}
			if (joinExpression != null) {
				for (int i = 0; i < joinExpression.length; i++) {
					JoinKey jk = joinExpression[i];
					jk.findAndLockLeft(j);
				}
			}
		}
		return this;
	}

	private JoinPath pathAccept( Query<?> obj) {
		// 先在当前表中查询已定义的可用引用关系。
		for (Reference ref : relationTable.getRefFieldsByRef().keySet()) {
			if (ref.getThisType() == relationTable && ref.getTargetType() == obj.getMeta() && ref.getHint() != null) {
				return this;
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(joinKeys).append(joinExpression).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JoinPath))
			return false;
		JoinPath o = (JoinPath) obj;
		if (!ArrayUtils.equals(this.joinKeys, o.joinKeys))
			return false;
		if (type != o.type)
			return false;
		// Annotation自身已经很好的实现的equals方法，不会受代理类等因素影响
		if(relationTable!=o.relationTable) {
			return false;
		}
		
		if (!ObjectUtils.equals(this.orderBy, o.orderBy)) {
			return false;
		}
		return ObjectUtils.equals(this.description, o.description);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (joinKeys != null) {
			for (JoinKey jk : joinKeys) {
				if (sb.length() > 0)
					sb.append(" and ");
				sb.append(jk.toString());
			}
		}
		if (joinExpression != null) {
			for (JoinKey jk : joinExpression) {
				if (sb.length() > 0)
					sb.append(" and ");
				if (jk.getField() == null) {
					sb.append(jk.getValue());
				} else {
					sb.append(jk.toString());
				}
			}
		}
		return sb.toString();
	}

	// Assert all left meta are assiagn to one table
	public ITableMetadata getLeftMeta() {
		ITableMetadata left = null;
		if (joinKeys != null) {
			for (JoinKey j : joinKeys) {
				ITableMetadata m = j.getLeftTableMeta();
				if (left == null) {
					left = m;
				} else if (m == null) {
				} else {
					if (left != m) {
						throw new IllegalArgumentException();
					}
				}
			}
		}
		if (joinExpression != null) {
			for (JoinKey j : joinExpression) {
				ITableMetadata m = j.getLeftTableMeta();
				if (left == null) {
					left = m;
				} else if (m == null) {
				} else {
					if (left != m) {
						throw new IllegalArgumentException();
					}
				}
			}
		}
		return left;
	}

	public ITableMetadata getRightMeta() {
		ITableMetadata right = null;
		if (joinKeys != null) {
			for (JoinKey j : joinKeys) {
				ITableMetadata m = j.getRightTableMeta();
				if (right == null) {
					right = m;
				} else if (m == null) {
				} else {
					if (right != m) {
						throw new IllegalArgumentException();
					}
				}
			}
		}
		if (joinExpression != null) {
			for (JoinKey j : joinExpression) {
				ITableMetadata m = j.getRightTableMeta();
				if (right == null) {
					right = m;
				} else if (m == null) {
				} else {
					if (right != m) {
						throw new IllegalArgumentException();
					}
				}
			}
		}
		return right;
	}

	public String getOrderBy() {
		return orderBy == null ? null : orderBy.value();
	}
}
