package jef.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.database.annotation.JoinDescription;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jsqlparser.statement.select.OrderBy;
import jef.database.jsqlparser.statement.select.OrderByElement;
import jef.database.meta.AbstractRefField;
import jef.database.meta.ISelectProvider;
import jef.database.meta.ITableMetadata;
import jef.database.meta.JoinPath;
import jef.database.meta.Reference;
import jef.database.meta.ReferenceField;
import jef.database.query.JoinElement;
import jef.database.query.OrderField;
import jef.database.query.Query;
import jef.database.query.QueryBuilder;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanWrapper;

final class VsManyLoadTask implements LazyLoadTask {
	private ReverseReferenceProcessor reverse;
	private Query<?> query;
	private JoinElement finalQuery;
	private QueryOption option;
	private JoinPath rs;
	private Map<Reference, List<Condition>> filters;
	private List<Condition> currentFilter;
	private ITableMetadata targetTableMeta;
	private List<AbstractRefField> refs;
	private List<OrderField> orders;

	/**
	 * 
	 * @param entry
	 *            模型中的静态条件
	 * @param filters
	 *            人工的动态过滤条件
	 */
	public VsManyLoadTask(Map.Entry<Reference, List<AbstractRefField>> entry,  Map<Reference, List<Condition>> filters) {
		this.filters = filters;
		this.refs = entry.getValue(); // 要装填的字段

		Reference ref = entry.getKey(); // 引用关系
		this.currentFilter=filters.get(ref);
		query = QueryBuilder.create(ref.getTargetType());
		rs = ref.toJoinPath();
		if (rs == null) {
			LogUtil.error("No join key found: " + ref);
		}
		this.reverse = CascadeUtil.getReverseProcessor(ref);
		targetTableMeta = query.getMeta();

		// 将预制的两个条件加入
		if (rs.getDescription() != null) {
			JoinDescription desc = rs.getDescription();
			if (desc.maxRows() > 0) {
				query.setMaxResult(desc.maxRows());
			}

			orders = new ArrayList<OrderField>();
			if (StringUtils.isNotEmpty(rs.getOrderBy())) {
				OrderBy order = DbUtils.parseOrderBy(rs.getOrderBy());
				for (OrderByElement ele : order.getOrderByElements()) {
					ColumnMapping<?> field = targetTableMeta.findField(ele.getExpression().toString());
					if (field != null) {
						orders.add(new OrderField(field.field(), ele.isAsc()));
					}
				}
			}
		}
		// 计算查询目标的引用关系
		finalQuery = query;
		option = QueryOption.createFrom(query);
		if(reverse!=null)
			option.skipReference=reverse.refs;
		if (!targetTableMeta.getRefFieldsByName().isEmpty()) {
			finalQuery = DbUtils.toReferenceJoinQuery(query, reverse == null ? null : reverse.refs); // 去除引用关系后将其转为Join，用这种方式进行的查询不查询反向的多对1关系
		}
	}

	public void process(Session db, Object obj) throws SQLException {
		if(!db.isOpen()){
			throw new SQLException("try to load field "+refs.get(0).getName()+" but the session was already closed!");
		}
		BeanWrapper bean = BeanWrapper.wrap(obj);
		if (DbUtils.appendRefCondition(bean, rs, query, currentFilter) == false)
			return;
		if(orders!=null){
			for(OrderField f:orders){
				query.addOrderBy(f.isAsc(), f.getField());
			}
		}
		@SuppressWarnings("unchecked")
		List<? extends IQueryableEntity> subs = db.innerSelect(finalQuery, null, filters, option);		
		
		for (ISelectProvider reff : refs) { // 根据配置装填到对象中去
			AbstractRefField refield = (AbstractRefField) reff;

			if (refield.isSingleColumn()) {// 引用字段填充
				Class<?> container = refield.getSourceContainerType();
				Object value = db.rProcessor.collectValueToContainer(subs, container, ((ReferenceField) refield).getTargetField().fieldName());
				bean.setPropertyValue(refield.getSourceField(), value);
			} else { // 全引用填充
				Class<?> container = refield.getSourceContainerType();
				Object value;
				if (targetTableMeta.getContainerType() == container) {
					value = subs.isEmpty() ? null : subs.get(0);
				} else {
					value = DbUtils.toProperContainerType(subs, container,targetTableMeta.getContainerType(),refield.getAsMap());
				}
				bean.setPropertyValue(refield.getSourceField(), value);
			}
		}
		// 反相关系
		if (reverse != null) {
			reverse.process(bean.getWrapped(), subs);
		}
	}

	public Collection<String> getEffectFields() {
		String[] str = new String[refs.size()];
		for (int i = 0; i < refs.size(); i++) {
			ISelectProvider p = refs.get(i);
			str[i] = p.getName();
		}
		return Arrays.asList(str);
	}
}
