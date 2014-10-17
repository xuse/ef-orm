package jef.database.jsqlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.LongValue;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.statement.select.Limit;
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
	
	private static final ExpressionList EXP_1=new ExpressionList(LongValue.L1);
	private static final List<SelectItem> SELECT_1=Arrays.<SelectItem>asList(new SelectExpressionItem(LongValue.L1,"l"));
	
	private SubSelect inner;
	private boolean isDistinct;
	private Limit removedLimit;
	
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
		List<Expression> distinctToGroupBy=null;
		if(isDistinct && select.getSelectItems().size()>1){//Distinct多个列
			distinctToGroupBy=distinctItemToGroupItem(select);
		}
		if(select.isGroupBy() || distinctToGroupBy!=null){
			Function count=new Function();
			count.setName("count");
			count.setParameters(EXP_1);
			SelectExpressionItem countItem=new SelectExpressionItem();
			countItem.setExpression(count);
			countItem.setAlias("count");
			this.selectItems=Arrays.<SelectItem>asList(countItem);
			
			//将原来的Select部分套在SubSelect内。
			PlainSelect innerSelect=new PlainSelect(select);
			if(distinctToGroupBy!=null){
				innerSelect.setDistinct(null);
				innerSelect.setGroupByColumnReferences(distinctToGroupBy);
			}
			innerSelect.setOrderBy(null);
//			List<SelectItem> ns=new ArrayList<SelectItem>();
//			for(Expression exp: innerSelect.getGroupByColumnReferences()){
//				SelectExpressionItem item=new SelectExpressionItem();
//				item.setExpression(exp);
//				ns.add(item);
//			}
			innerSelect.setSelectItems(SELECT_1);
			inner=new SubSelect();
			inner.setSelectBody(innerSelect);
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
			removedLimit=select.getLimit();
			//this.orderByElements=select.getOrderByElements(); //Order不要	
		}
	}
	
	private List<Expression> distinctItemToGroupItem(PlainSelect select) {
		List<Expression> result=new ArrayList<Expression>();
		for(SelectItem item:select.getSelectItems()){
			if(item.isAllColumns()){
				continue;
			}
			result.add(item.getAsSelectExpression().getExpression());//将
		}
		return result;
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

	public Limit getRemovedLimit() {
		return removedLimit;
	}
}
