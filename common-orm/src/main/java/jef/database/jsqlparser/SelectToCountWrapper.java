package jef.database.jsqlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.statement.select.PlainSelect;
import jef.database.jsqlparser.statement.select.SelectExpressionItem;
import jef.database.jsqlparser.statement.select.SubSelect;
import jef.database.jsqlparser.statement.select.Union;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectBody;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.ToCountDeParser;
import jef.database.query.SqlExpression;

public class SelectToCountWrapper extends PlainSelect{
	private SubSelect inner;
	private boolean isDistinct;
	
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
		inner=new SubSelect();
		Union newUnion=new Union(union);
		newUnion.setOrderBy(null);
		inner.setSelectBody(newUnion);
		inner.setAlias("t__cnt"); //为了兼容Derby，故给表一个别名
		this.fromItem=inner;
	}
	
	public SelectToCountWrapper(PlainSelect select,DatabaseDialect profile){
		isDistinct=select.getDistinct()!=null;
		if(select.isGroupBy()){
			Function count=new Function();
			count.setName("count");
			count.setAllColumns(true);
			SelectExpressionItem countItem=new SelectExpressionItem();
			countItem.setExpression(count);
			countItem.setAlias("count");
			this.selectItems=Arrays.<SelectItem>asList(countItem);
			
			//将原来的Select部分套在SubSelect内。
			inner=new SubSelect();
			PlainSelect newSelect=new PlainSelect(select);
			newSelect.setOrderBy(null);
			List<SelectItem> ns=new ArrayList<SelectItem>();
			for(Expression exp: newSelect.getGroupByColumnReferences()){
				SelectExpressionItem item=new SelectExpressionItem();
				item.setExpression(exp);
				ns.add(item);
			}
			newSelect.setSelectItems(ns);
			inner.setSelectBody(newSelect);
			inner.setAlias("t__cnt");//为了兼容Derby，故给表一个别名
			this.fromItem=inner;
		}else{
			this.distinct=null; //特殊处理1 distinct去除
			SelectItem convert=getSelectItem(select,profile);
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
	
	private SelectItem getSelectItem(PlainSelect select,DatabaseDialect profile) {
		SelectExpressionItem result=new SelectExpressionItem();
		StringBuilder sb=new StringBuilder(64);
		ToCountDeParser.rewriteSelectItem(sb, select, profile);
		result.setExpression(new SqlExpression(sb.toString()));
		return result;
	}

	public SelectBody getInnerSelectBody(){
		if(inner!=null){
			return inner.getSelectBody();
		}
		return null;
	}

	public boolean isDistinct() {
		return isDistinct;
	}
}
