package com.github.geequery.test.database.boss;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import jef.database.Condition.Operator;
import jef.database.DbClient;
import jef.database.QB;
import jef.database.query.Query;
import jef.database.query.QueryBuilder;
import jef.database.query.Selects;

import org.junit.Test;
import org.springframework.dao.DataAccessResourceFailureException;

public class Case {
	DbClient db=new DbClient();

	@Test
	public void test1() {
		groupSaleIncome(new Date(),new Date());
		

	}

	public List<StatisticSaleIncomeGenDto> groupSaleIncome(Date start, Date end) {
		Query<SaleOrder> query = QB.create(SaleOrder.class);
		query.addCondition(SaleOrder.Field.isDelete, 1);
		query.addCondition(SaleOrder.Field.isTrial, 1);
		query.addCondition(SaleOrder.Field.status, Operator.NOT_EQUALS, 1);
		query.addCondition(SaleOrder.Field.isStatistic, 1);
		query.addCondition(SaleOrder.Field.updateTime, Operator.GREAT_EQUALS, start);
		query.addCondition(SaleOrder.Field.updateTime, Operator.LESS, end);

		Selects selects = QueryBuilder.selectFrom(query);
		selects.column(SaleOrder.Field.partnerId).group().as("partnerId");
		
		
		selects.column(SaleOrder.Field.devIncome).expression("? * dev_discount/100").sum().as("deviceIncome");
		selects.sqlExpression("dev_income * dev_discount/100").sum().as("deviceIncome");
		
//		BeanUtils.setFieldValue(selects.sqlExpression("SUM(DEV_INCOME*DEV_DISCOUNT/100)").as("deviceIncome"), "projection", 12);
//		BeanUtils.setFieldValue(selects.sqlExpression("SUM(SERV_INCOME*SERV_DISCOUNT/100)").as("servIncome"),"projection",12);
		
//		selects.sqlExpression("SUM(DEV_INCOME*DEV_DISCOUNT/100)").as("deviceIncome")
//		selects.sqlExpression("SUM(SERV_INCOME*SERV_DISCOUNT/100)").as("servIncome")
		try {
			return db.selectAs(query, StatisticSaleIncomeGenDto.class);
		} catch (SQLException e) {
			throw new DataAccessResourceFailureException("统计分析分组查询异常", e);
		}

	}
}
