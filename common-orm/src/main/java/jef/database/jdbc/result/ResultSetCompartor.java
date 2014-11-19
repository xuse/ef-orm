package jef.database.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

import javax.persistence.PersistenceException;

import jef.database.wrapper.clause.InMemoryOrderBy;

/**
 * 结果集比较器
 * @author jiyi
 *
 */
final class ResultSetCompartor implements Comparator<ResultSet> {
	private InMemoryOrderBy orders;

	public ResultSetCompartor(InMemoryOrderBy order) {
		this.orders=order;
	}

	@Override
	public int compare(ResultSet value, ResultSet value2) {
		int len = orders.size();
		int[] orderFields = orders.getOrderFields();
		boolean[] orderAscendent = orders.getOrderAsc();
		int retVal = 0;
		for (int i = 0; i < len; i++) {
			try {
				retVal = compares(value.getObject(orderFields[i]), value2.getObject(orderFields[i]));
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
			if(retVal!=0){
				return orderAscendent[i]?retVal:-retVal; 
			}
		}
		return -1;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int compares(Object object, Object object2) {
		if (object == object2)
			return 0;
		if (object == null)
			return 1;
		if (object2 == null)
			return -1;
		return ((Comparable) object).compareTo(object2);
	}
}
