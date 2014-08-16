package jef.database.jsqlparser;

import java.util.Arrays;

import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.SelectItem;

@Deprecated
public class SelectToCountWrapper extends PlainSelect{
	public SelectToCountWrapper(Union union){
		//将Select部分重写为新的Count Function
		Function count=new Function();
		count.setName("count");
		count.setAllColumns(true);
		SelectExpressionItem countItem=new SelectExpressionItem();
		countItem.setExpression(count);
		countItem.setAlias("count");
		this.selectItems=Arrays.<SelectItem>asList(countItem);
		
		//将原来的Union部分套在SubSelect内。
		SubSelect from=new SubSelect();
		Union newUnion=new Union(union);
		newUnion.setOrderBy(null);
		from.setSelectBody(newUnion);
		this.fromItem=from;
	}
	
	public SelectToCountWrapper(PlainSelect select,DatabaseDialect profile){
		this.distinct=null; //特殊处理1 distinct去除
		SelectItem convert=new CountSelectItemConverter(select.getSelectItems(),select.getDistinct(),profile);
		this.selectItems=Arrays.asList(convert);//Select部分重写
		this.top=select.getTop();
		this.into=select.getInto();
		this.fromItem=select.getFromItem();
		this.joins=select.getJoins();
		this.where=select.getWhere();
		this.groupByColumnReferences=select.getGroupByColumnReferences();
		this.having=select.getHaving();
		this.limit=select.getLimit();
		//this.orderByElements=select.getOrderByElements(); //Order不要
	}
}
