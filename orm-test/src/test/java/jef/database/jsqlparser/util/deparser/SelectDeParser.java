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
package jef.database.jsqlparser.util.deparser;

import java.util.Iterator;
import java.util.List;

import jef.database.jsqlparser.expression.Expression;
import jef.database.jsqlparser.expression.ExpressionVisitor;
import jef.database.jsqlparser.expression.JpqlParameter;
import jef.database.jsqlparser.schema.Column;
import jef.database.jsqlparser.schema.Table;
import jef.database.jsqlparser.statement.select.AllColumns;
import jef.database.jsqlparser.statement.select.AllTableColumns;
import jef.database.jsqlparser.statement.select.FromItem;
import jef.database.jsqlparser.statement.select.FromItemVisitor;
import jef.database.jsqlparser.statement.select.Join;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.jsqlparser.statement.select.OrderByVisitor;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.SelectItem;
import jef.database.jsqlparser.statement.select.SelectItemVisitor;
import jef.database.jsqlparser.statement.select.SelectVisitor;
import jef.database.jsqlparser.statement.select.SubJoin;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Top;
import jef.database.jsqlparser.statement.select.Union;

/**
 * A class to de-parse (that is, tranform from JSqlParser hierarchy into a string)
 * a {@link jef.database.jsqlparser.statement.select.Select}
 */
public class SelectDeParser implements SelectVisitor, OrderByVisitor, SelectItemVisitor, FromItemVisitor {

    protected StringBuffer buffer;

    protected ExpressionVisitor expressionVisitor;

    public SelectDeParser() {
    }

    /**
	 * @param expressionVisitor a {@link ExpressionVisitor} to de-parse expressions. It has to share the same<br>
	 * StringBuffer (buffer parameter) as this object in order to work
	 * @param buffer the buffer that will be filled with the select
	 */
    public SelectDeParser(ExpressionVisitor expressionVisitor, StringBuffer buffer) {
        this.buffer = buffer;
        this.expressionVisitor = expressionVisitor;
    }

    public void visit(PlainSelect plainSelect) {
        buffer.append("select ");
        Top top = plainSelect.getTop();
        if (top != null) top.toString();
        if (plainSelect.getDistinct() != null) {
            buffer.append("DISTINCT ");
            if (plainSelect.getDistinct().getOnSelectItems() != null) {
                buffer.append("ON (");
                for (Iterator<SelectItem> iter = plainSelect.getDistinct().getOnSelectItems().iterator(); iter.hasNext(); ) {
                    SelectItem selectItem = iter.next();
                    selectItem.accept(this);
                    if (iter.hasNext()) {
                        buffer.append(", ");
                    }
                }
                buffer.append(") ");
            }
        }
        for (Iterator<SelectItem> iter = plainSelect.getSelectItems().iterator(); iter.hasNext(); ) {
            SelectItem selectItem = iter.next();
            selectItem.accept(this);
            if (iter.hasNext()) {
                buffer.append(",");
            }
        }
        buffer.append(" ");
        if (plainSelect.getFromItem() != null) {
            buffer.append("from ");
            plainSelect.getFromItem().accept(this);
        }
        if (plainSelect.getJoins() != null) {
            for (Iterator<Join> iter = plainSelect.getJoins().iterator(); iter.hasNext(); ) {
                Join join = iter.next();
                deparseJoin(join);
            }
        }
        if (plainSelect.getWhere() != null) {
            buffer.append(" where ");
            plainSelect.getWhere().accept(expressionVisitor);
        }
        if (plainSelect.getGroupByColumnReferences() != null) {
            buffer.append(" group by ");
            for (Iterator<Expression> iter = plainSelect.getGroupByColumnReferences().iterator(); iter.hasNext(); ) {
                Expression columnReference = iter.next();
                columnReference.accept(expressionVisitor);
                if (iter.hasNext()) {
                    buffer.append(",");
                }
            }
        }
        if (plainSelect.getHaving() != null) {
            buffer.append(" having ");
            plainSelect.getHaving().accept(expressionVisitor);
        }
        if (plainSelect.getOrderByElements() != null) {
            deparseOrderBy(plainSelect.getOrderByElements());
        }
        if (plainSelect.getLimit() != null) {
            deparseLimit(plainSelect.getLimit());
        }
    }

    public void visit(Union union) {
        for (Iterator<PlainSelect> iter = union.getPlainSelects().iterator(); iter.hasNext(); ) {
            buffer.append("(");
            PlainSelect plainSelect = iter.next();
            plainSelect.accept(this);
            buffer.append(")");
            if (iter.hasNext()) {
                buffer.append(" UNION ");
            }
        }
        if (union.getOrderByElements() != null) {
            deparseOrderBy(union.getOrderByElements());
        }
        if (union.getLimit() != null) {
            deparseLimit(union.getLimit());
        }
    }

    public void visit(OrderByElement orderBy) {
        orderBy.getExpression().accept(expressionVisitor);
        if (orderBy.isAsc()) buffer.append(" ASC"); else buffer.append(" DESC");
    }

    public void visit(Column column) {
        buffer.append(column.getWholeColumnName());
    }

    public void visit(AllColumns allColumns) {
        buffer.append("*");
    }

    public void visit(AllTableColumns allTableColumns) {
        buffer.append(allTableColumns.getTable().getWholeTableName() + ".*");
    }

    public void visit(SelectExpressionItem selectExpressionItem) {
        selectExpressionItem.getExpression().accept(expressionVisitor);
        if (selectExpressionItem.getAlias() != null) {
            buffer.append(" AS " + selectExpressionItem.getAlias());
        }
    }

    public void visit(SubSelect subSelect) {
        buffer.append("(");
        subSelect.getSelectBody().accept(this);
        buffer.append(")");
    }

    public void visit(Table tableName) {
        buffer.append(tableName.getWholeTableName());
        String alias = tableName.getAlias();
        if (alias != null && alias.length()>0) {
            buffer.append(" " + alias);
        }
    }

    public void deparseOrderBy(List<OrderByElement> orderByElements) {
        buffer.append(" order by ");
        for (Iterator<OrderByElement> iter = orderByElements.iterator(); iter.hasNext(); ) {
            OrderByElement orderByElement = (OrderByElement) iter.next();
            orderByElement.accept(this);
            if (iter.hasNext()) {
                buffer.append(",");
            }
        }
    }

    public void deparseLimit(Limit limit) {
        buffer.append(" LIMIT ");
        if (limit.isRowCountJdbcParameter()) {
            buffer.append("?");
        } else if (limit.getRowCount() != 0) {
            buffer.append(limit.getRowCount());
        } else {
            buffer.append("18446744073709551615");
        }
        if (limit.getOffsetJdbcParameter()!=null) {
            buffer.append(" OFFSET ?");
        } else if (limit.getOffset() != 0) {
            buffer.append(" OFFSET " + limit.getOffset());
        }
    }

    public StringBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(StringBuffer buffer) {
        this.buffer = buffer;
    }

    public ExpressionVisitor getExpressionVisitor() {
        return expressionVisitor;
    }

    public void setExpressionVisitor(ExpressionVisitor visitor) {
        expressionVisitor = visitor;
    }

    public void visit(SubJoin subjoin) {
        buffer.append("(");
        subjoin.getLeft().accept(this);
        buffer.append(" ");
        deparseJoin(subjoin.getJoin());
        buffer.append(")");
    }

    public void deparseJoin(Join join) {
        if (join.isSimple()) buffer.append(", "); else {
            if (join.isRight()) buffer.append("RIGHT "); else if (join.isNatural()) buffer.append("NATURAL "); else if (join.isFull()) buffer.append("FULL "); else if (join.isLeft()) buffer.append("LEFT ");
            if (join.isOuter()) buffer.append("OUTER "); else if (join.isInner()) buffer.append("INNER ");
            buffer.append("JOIN ");
        }
        FromItem fromItem = join.getRightItem();
        fromItem.accept(this);
        if (join.getOnExpression() != null) {
            buffer.append(" ON ");
            join.getOnExpression().accept(expressionVisitor);
        }
        if (join.getUsingColumns() != null) {
            buffer.append(" USING ( ");
            for (Iterator<Column> iterator = join.getUsingColumns().iterator(); iterator.hasNext(); ) {
                Column column = (Column) iterator.next();
                buffer.append(column.getWholeColumnName());
                if (iterator.hasNext()) {
                    buffer.append(" ,");
                }
            }
            buffer.append(")");
        }
    }

	public void visit(JpqlParameter tableClip) {
		buffer.append(tableClip.toString());
	}
}
