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
package jef.database.jsqlparser.expression;



/**
 * A basic class for binary expressions, that is expressions having a left member and a right member
 * which are in turn expressions. 
 */
public abstract class BinaryExpression implements Expression,Ignorable {
	public Expression rewrite;
	
	public enum Prior{
		LEFT,RIGHT
	}
	
    public Prior getPrior() {
		return prior;
	}
    
    /**
     * 交换左右参数
     */
    public void swap(){
    	Expression tmp=leftExpression;
    	leftExpression=rightExpression;
    	rightExpression=tmp;
    	if(prior==Prior.LEFT){
    		prior=Prior.RIGHT;
    	}else if(prior==Prior.RIGHT){
    		prior=Prior.LEFT;
    	}
    }

	public void setPrior(Prior prior) {
		this.prior = prior;
	}

	protected Expression leftExpression;

	protected Expression rightExpression;

    private boolean not = false;
    
    private Prior prior; 
    
    //变量绑定值是否为空
    private ThreadLocal<Boolean> isEmpty = new ThreadLocal<Boolean>(); 

    public boolean isEmpty() {
    	Boolean b=isEmpty.get();
    	if(b!=null)return b;
    	boolean leftIsEmpty=false;
    	boolean rightIsEmpty=false;
    	if(leftExpression instanceof Ignorable){
    		leftIsEmpty=((Ignorable) leftExpression).isEmpty();
    	}
    	if(rightExpression instanceof Ignorable){
    		rightIsEmpty=((Ignorable) rightExpression).isEmpty();
    	}
		return leftIsEmpty && rightIsEmpty;
	}

	public void setEmpty(Boolean isEmpty) {
		this.isEmpty.set(isEmpty);
	}

	public BinaryExpression() {
		isEmpty.set(null);
    }

    public final Expression getLeftExpression() {
        return leftExpression;
    }

    public final Expression getRightExpression() {
        return rightExpression;
    }

    public final void setLeftExpression(Expression expression) {
        leftExpression = expression;
    }

    public void setRightExpression(Expression expression) {
        rightExpression = expression;
    }

    public void setNot() {
        not = true;
    }

    public boolean isNot() {
        return not;
    }

    public String toString() {
    	StringBuilder sb=new StringBuilder();
    	appendTo(sb);
    	return sb.toString();
    }

    public void appendTo(StringBuilder sb) {
    	if(rewrite!=null){
    		rewrite.appendTo(sb);
        	return;
    	}
    	Expression  _leftExpression = this.leftExpression;
    	if(isEmptyExpress(_leftExpression)){
    		_leftExpression=null;
    	}
    	Expression  _rightExpression = this.rightExpression;
    	if(isEmptyExpress(_rightExpression)){
    		_rightExpression=null;
    	}
    	
    	if(_leftExpression==null && _rightExpression==null){
    		return;
    	}
    	if(not){
    		sb.append("NOT ");
    	}
    	if(prior==Prior.LEFT){
    		sb.append("PRIOR ");
    	}
    	if(_leftExpression!=null){
    		_leftExpression.appendTo(sb);
    		sb.append(' ');
    	}
    	if(_leftExpression!=null && _rightExpression!=null){
    		sb.append(getStringExpression());
    		if(prior==Prior.RIGHT){
        		sb.append(" PRIOR");
        	}
    	}
    	
    	if(_rightExpression!=null){
    		sb.append(' ');
    		_rightExpression.appendTo(sb);
    	}
	}

	static boolean isEmptyExpress(Expression e) {
		if(e instanceof Parenthesis){
			e=((Parenthesis) e).getExpression();
		}
		if(e instanceof Ignorable){
			return (((Ignorable) e).isEmpty());
		}
		return false;
	}

	public abstract String getStringExpression();
}
