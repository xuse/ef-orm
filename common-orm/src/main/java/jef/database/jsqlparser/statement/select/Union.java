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
package jef.database.jsqlparser.statement.select;

import java.util.List;

/**
 * A UNION statement
 */
public class Union implements SelectBody {

    private List<PlainSelect> plainSelects;

    private OrderBy orderBy;

    private Limit limit;

    private boolean distinct;

    private boolean all;
    
    public Union(){
    }
    
    //浅拷贝构造
    public Union(Union union){
    	this.plainSelects=union.plainSelects;
    	this.distinct=union.distinct;
    	this.all=union.all;
    	this.orderBy=union.orderBy;
    	this.limit=union.limit;
    }

    public void accept(SelectVisitor selectVisitor) {
        selectVisitor.visit(this);
    }

   

    /**
	 * the list of {@link PlainSelect}s in this UNION
	 * @return the list of {@link PlainSelect}s
	 */
    public List<PlainSelect> getPlainSelects() {
        return plainSelects;
    }


    public void setPlainSelects(List<PlainSelect> list) {
        plainSelects = list;
    }

    public Limit getLimit() {
        return limit;
    }

    public void setLimit(Limit limit) {
        this.limit = limit;
    }

    /**
	 * This is not 100% right; every UNION should have their own All/Distinct clause...
	 */
    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    /**
	 * This is not 100% right; every UNION should have their own All/Distinct clause...
	 */
    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        return appendTo(sb).toString();
    }

	public StringBuilder appendTo(StringBuilder sb) {
        String allDistinct="";
        if (isAll()) {
            allDistinct = "ALL ";
        } else if (isDistinct()) {
            allDistinct = "DISTINCT ";
        }
        
        boolean hasParentis=limit!=null;
        
        for (int i = 0; i < plainSelects.size(); i++) {
        	if(i>0){
        		sb.append("\n UNION ").append(allDistinct);
        	}
        	if(hasParentis){
        		plainSelects.get(i).appendTo(sb.append('(')).append(')');
        	}else{
        		plainSelects.get(i).appendTo(sb);
        	}
        }
        if(orderBy!=null){
        	orderBy.appendTo(sb);
        }
        if(limit != null){
        	sb.append(limit.toString());
        }
        return sb;
	}

	public OrderBy getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}
}
