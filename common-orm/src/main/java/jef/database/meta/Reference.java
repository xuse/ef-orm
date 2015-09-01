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

import jef.database.annotation.JoinType;
import jef.database.query.Query;
import jef.database.query.ReadOnlyQuery;
import jef.database.query.ReferenceType;
import jef.tools.Assert;

import com.google.common.base.Objects;

/**
 * 关于多表操作的三个概念设计
 * jef.database.meta.JoinKey类：描述两个实体间的一组关系。两个实体间的关系可能由多个jef.database.meta.JoinKey构成
 * 
 * Reference:描述两个实体的关系，这个关系除了包含多个JoinKey意外，还包含了A或者B中引用对方的那个字段名
 * @author Jiyi
 *
 */
public class Reference{
	/**
	 * 链接目标关系，定义了关联的操作方式,
	 * 详细参见{@link ReferenceType}
	 */
	private ReferenceType type;
	/**
	 * 发起表
	 */
	private ITableMetadata fromType;
	/**
	 * 要链接到一个目标的表（类）。
	 */
	private ITableMetadata targetType;
	
	/**
	 * 连接路径 
	 */
	private JoinPath hint;
	
	/**
	 * 产生一个反向的Reference关系
	 * @param path
	 * @param r
	 * @return
	 */
	public static Reference createRevse(JoinPath path,Reference r){
		Reference ref=new Reference(r.getTargetType(),r.getType(),r.getThisType());
		ref.setHint(path);
		return ref;
	}
	
	/**
	 * 构造
	 * @param target  引用表
	 * @param refType 引用类型
	 * @param source 被引用表
	 */
	public Reference(ITableMetadata target, ReferenceType refType, ITableMetadata source) {
		this.type=refType;
		this.fromType=source;
		this.targetType=target;
		Assert.notNull(type);
		Assert.notNull(fromType);
		Assert.notNull(targetType);
	}
	/**
	 * 空构造
	 * 必须有。会通过反射调用
	 */
	@SuppressWarnings("unused")
	private Reference(){};
	/**
	 * 返回引用类型
	 * @return 引用类型 
	 */
	public ReferenceType getType() {
		return type;
	}
	/**
	 * 设置引用类型
	 * @param type 引用类型
	 */
	public void setType(ReferenceType type) {
		this.type = type;
	}
	/**
	 * 返回被引用表
	 * @return
	 */
	public ITableMetadata getTargetType() {
		return targetType;
	}
	/**
	 * 设置被引用表 
	 * @param targetType
	 */
	public void setTargetType(ITableMetadata targetType) {
		this.targetType = targetType;
	}
	/**
	 * 返回引用表 
	 * @return
	 */
	public ITableMetadata getThisType() {
		return fromType;
	}
	public String toString(){
		StringBuilder sb=new StringBuilder();
		if(fromType!=null)
			sb.append("Join:").append(fromType.getThisType().getSimpleName()).append("->");
		if(targetType!=null)
			sb.append(targetType.getThisType().getSimpleName());
		if(this.hint!=null){
			sb.append(", CustomPath: "+hint);
			sb.append("\n");
		}
		return sb.toString();
	}
	

	//当没有配置Join路径时，通过自动查找的方式来寻找Join路径。
	//这是一种为了减少配置工作量而不得不做的容错处理。（不太推荐依赖这种容错机制）
	private JoinPath findPath(ITableMetadata thisType, ITableMetadata targetType) {
		//先在当前表中查询已定义的可用引用关系。
		for(Reference ref:thisType.getRefFieldsByRef().keySet()){
			if(ref==this){
				continue;
			}
			if(ref.getThisType()==thisType && ref.getTargetType()==targetType && ref.getHint()!=null){
				return ref.getHint();
			}
		}
		//再到目标表中查询可用的反向引用关系。
		for(Reference ref:targetType.getRefFieldsByRef().keySet()){
			if(ref.getThisType()==targetType && ref.getTargetType()==thisType && ref.getHint()!=null){
				return ref.getHint().flip();
			}
		}
		throw new IllegalArgumentException("Can not find the join path from "+thisType.getSimpleName()+" to "+ targetType.getSimpleName());
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Reference))return false;
		Reference o=(Reference)obj;
		if(!Objects.equal(this.fromType, o.fromType))return false;
		if(!Objects.equal(this.targetType, o.targetType))return false;
		if(this.type!=o.type)return false;
		if(!Objects.equal(this.hint, o.hint))return false;
		return true;
	}
	
	@Override
	public int hashCode() {
		int hashCode=this.fromType.hashCode()+this.targetType.hashCode()+this.type.hashCode();
		return hashCode;
	}

	public JoinPath getHint() {
		return hint;
	}

	/**
	 * 返回到目标实体表的路径
	 */
	public JoinPath toJoinPath(){
		Assert.notNull(this.targetType,"No join target found.");
		if(this.hint!=null )return hint;
		this.hint=findPath(fromType, targetType);
		return hint;
	}

	public void setHint(JoinPath hint) {
		Query<?> leftq=ReadOnlyQuery.getEmptyQuery(this.fromType);
		Query<?> rightq=ReadOnlyQuery.getEmptyQuery(this.targetType);
		JoinPath path=hint.accept(leftq, rightq);
		Assert.notNull(path);
		this.hint = hint;
	}
	
	/**
	 * 返回关联配置的Join类型。
	 * 默认类型为左连接
	 * @return Join类型。
	 * @see JoinType
	 */
	public JoinType getJoinType(){
		JoinPath path=toJoinPath();
		if(path==null){
			return JoinType.LEFT;
		}
		return path.getType();
	}
	
	/**
	 * 得到使用这个引用的全部引用字段
	 * @return
	 */
	public List<AbstractRefField> getAllRefFields(){
		return fromType.getRefFieldsByRef().get(this); 
	}
	
	private List<Reference> reverse;
	
	/**
	 * 得到目前对象对当前对象的反向引用关系。这个方法必须在所有级联关系初始化后调用，一般在第一次查询操作的时候才能调用。
	 * @return
	 */
	public List<Reference> getExistReverseReference(){
		if(null==reverse){
			reverse=new ArrayList<Reference>();
			for(Reference rr:getTargetType().getRefFieldsByRef().keySet()){
				if(isReverse(rr)){
					//出现多个反向关联，由于JoinDesc的限定条件存在，正向关联被分化，当反向关联查找时，会出现重复的关联。
					//这种情况下，如果正向关联是多个的
					if(!reverse.isEmpty()){
						throw new IllegalArgumentException();
					}
					reverse.add(rr);
				}
			}
		}
		return reverse;
	}
	
	
	private boolean isReverse(Reference ref){
		if(this.fromType!=ref.targetType || this.targetType!=ref.fromType || type.reverse()!=ref.type){
			return false;
		}
		if(hint.getJoinKeys().length!=ref.hint.getJoinKeys().length){
			return false;
		}
		for(JoinKey key:hint.getJoinKeys()){
			if(!hasReverse(key,ref.hint.getJoinKeys())){
				return false;
			}
		}
		return true;
	}

	private boolean hasReverse(JoinKey key, JoinKey[] joinKeys) {
		String left=key.getField().name();
		String right=key.getRightAsField().name();
		for(JoinKey k:joinKeys){
			if(right.equals(k.getField().name()) && left.equals(k.getRightAsField().name())){
				return true;
			}
		}
		return false;
	}
}
