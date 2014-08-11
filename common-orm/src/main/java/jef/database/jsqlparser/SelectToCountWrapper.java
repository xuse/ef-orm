package jef.database.jsqlparser;

import java.util.Arrays;

import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.SelectItem;

public class SelectToCountWrapper extends PlainSelect{
	public SelectToCountWrapper(Union union){
		Function count=new Function();
		count.setName("count");
		count.setAllColumns(true);
		SelectExpressionItem countItem=new SelectExpressionItem();
		countItem.setExpression(count);
		countItem.setAlias("count");
		this.selectItems=Arrays.<SelectItem>asList(countItem);
		
		SubSelect from=new SubSelect();
		Union newUnion=new Union(union);
		newUnion.setOrderBy(null);
		from.setSelectBody(newUnion);
		this.fromItem=from;
	}
	
	public SelectToCountWrapper(PlainSelect select,DatabaseDialect profile){
		this.fromItem=select.getFromItem();
		this.groupByColumnReferences=select.getGroupByColumnReferences();
		this.having=select.getHaving();
		this.into=select.getInto();
		this.where=select.getWhere();
		this.joins=select.getJoins();
		this.limit=select.getLimit();
		//this.orderByElements=select.getOrderByElements(); //不要
		this.top=select.getTop();
		
		SelectItem convert=new CountSelectItemConverter(select.getSelectItems(),select.getDistinct(),profile);
		this.distinct=null;
		this.selectItems=Arrays.asList(convert);
	}
}
