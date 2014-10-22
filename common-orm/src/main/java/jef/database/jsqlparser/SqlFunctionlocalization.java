package jef.database.jsqlparser;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.DbMetaData;
import jef.database.ORMConfig;
import jef.database.OperateTarget;
import jef.database.dialect.DatabaseDialect;
import jef.database.jsqlparser.expression.BinaryExpression;
import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Interval;
import jef.database.jsqlparser.expression.operators.arithmetic.Addition;
import jef.database.jsqlparser.expression.operators.arithmetic.Concat;
import jef.database.jsqlparser.expression.operators.relational.ExpressionList;
import jef.database.jsqlparser.statement.select.Limit;
import jef.database.jsqlparser.statement.select.StartWithExpression;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.VisitorAdapter;
import jef.database.meta.Feature;
import jef.database.meta.FunctionMapping;
import jef.database.query.function.SQLFunction;

/**
 * 将函数，字符串相加等逻辑修改为符合当前数据库的格式
 * 
 * @author jiyi
 * 
 */
public class SqlFunctionlocalization extends VisitorAdapter {
	private DatabaseDialect profile;
	private OperateTarget db;
	private boolean check;
	
	public StartWithExpression delayStartWith;
	public Limit               delayLimit;

	/**
	 * 构造
	 * @param dialect 数据库简要表
	 * @param db      用于进行UserFunction检查，如果传入null则不进行检查
	 */
	public SqlFunctionlocalization(DatabaseDialect dialect, OperateTarget db) {
		this.profile = dialect;
		this.db = db;
		this.check = ORMConfig.getInstance().isCheckSqlFunctions();
	}

	@Override
	public void visit(Concat concat) {
		super.visit(concat);// 先处理内层的。。。
		if(profile.has(Feature.CONCAT_IS_ADD)){
			concat.rewrite = new Addition(concat.getLeftExpression(),concat.getRightExpression());
		}else if (profile.notHas(Feature.SUPPORT_CONCAT)) {
			List<Expression> el = new ArrayList<Expression>();
			recursion(concat, el);
			Function func = new Function();
			func.setName("concat");
			func.setParameters(new ExpressionList(el));
			concat.rewrite = func;
		}
	}

	@Override
	public void visit(Function function) {
		super.visit(function);// 先处理内层的。。。

		String funName = function.getName().toLowerCase();
		FunctionMapping mapping = profile.getFunctions().get(funName);// 数据库有这个函数
		if (mapping == null) {
			jef.database.query.Func func = null;
			try {
				func = jef.database.query.Func.valueOf(funName);
			} catch (IllegalArgumentException e) {
			}
			;
			mapping = profile.getFunctionsByEnum().get(func);
			if (mapping == null) {
				if (check) {
					// 可能是用户自行创建的数据库函数
					try {
						checkUserFunction(funName);
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				} else {
					return;
				}
			}
		}
		mapping.rewrite(function);
	}

	private void checkUserFunction(String funName) throws SQLException {
		if (db == null) {
			throw new IllegalArgumentException("database " + profile.getName() + " doesn't support function: " + funName + ".");
		}
		DbMetaData meta = db.getMetaData();
		if (meta.checkedFunctions.contains(funName)) {
			return;
		}
		if (meta.existsFunction(null, funName)) {
			meta.checkedFunctions.add(funName);
		} else {
			throw new IllegalArgumentException("database " + profile.getName() + " doesn't support function: " + funName + ".");
		}
	}

	@Override
	public void visit(StartWithExpression startWithExpression) {
		if (profile.notHas(Feature.SUPPORT_CONNECT_BY)) {
			if (super.visitPath.size() <= 2) { // 距离statement最大为2 将递归条件保留下来，从而后续支持内存中 递归过滤
				delayStartWith = new StartWithExpression(startWithExpression.getStartExpression(), startWithExpression.getConnectExpression());
			} else {
				if (ORMConfig.getInstance().isAllowRemoveStartWith()) {
					String removed = startWithExpression.toString();
					LogUtil.warn("[" + removed + "] was removed from your SQL since current db doesn't support it.");
				} else {
					throw new IllegalArgumentException("The 'START WITH ... CONNECT BY ...' syntax, current db [" + profile.getName() + "] doesn't support!");
				}
			}
			startWithExpression.setStartExpression(null);
			startWithExpression.setConnectExpression(null);
			
		}
		super.visit(startWithExpression);
	}

	@Override
	public void visit(Limit limit) {
		if (profile.notHas(Feature.SUPPORT_LIMIT)) {
			if (super.visitPath.size() <= 2) { // 距离statement最大为2 将递归条件保留下来，从而后续支持内存中 递归过滤
				delayLimit=new Limit(limit);
				limit.clear();
			}
		}
		super.visit(limit);
	}
	
	public static void ensureUserFunction(FunctionMapping mapping, OperateTarget db) throws SQLException {
		DbMetaData meta = db.getMetaData();
		boolean flag = true;
		for (String name : mapping.requiresUserFunction()) {
			if (meta.checkedFunctions.contains(name)) {
				continue;
			}
			meta.checkedFunctions.add(name);
			if (!meta.existsFunction(null, name)) {
				flag = false;
				break;
			}
		}
		if (flag)
			return;
		SQLFunction sf = mapping.getFunction();
		URL url = sf.getClass().getResource(sf.getClass().getSimpleName() + ".sql");
		if (url == null) {
			// log.warn("Can't find user script file for user function "+ sf);
			throw new IllegalArgumentException("Can't find user script file for user function " + sf);
		}
		try {
			meta.executeScriptFile(url);
		} catch (SQLException ex) {
			throw ex;
		}
	}

	private void recursion(Concat concat, List<Expression> el) {
		Expression left = concat.getLeftExpression();
		if (left instanceof Concat) {
			recursion((Concat) left, el);
		} else {
			el.add(left);
		}
		Expression right = concat.getRightExpression();
		el.add(right);
	}

	/**
	 * 只有PG和MYSQL是支持interval语法的，但是两者的语法也有区别。 Oracle不支持Interval，但是支持以1为一天的小数运算，如
	 * 1/86400=1秒 1/1440=1分 1/24的小时。 oracle可以用整数运算来实现天数的加减。
	 * oracle提供了add_months来实现月数的加减, 可以用add_months 12 /-12来实现加减年份
	 */
	@Override
	public void visit(Interval interval) {
		super.visit(interval);
		Object parent = visitPath.pop();
		if (parent instanceof BinaryExpression) {
			profile.processIntervalExpression((BinaryExpression) parent, interval);
		} else if (parent instanceof ExpressionList) {
			Object func = visitPath.getFirst();
			if (func instanceof Function) {
				profile.processIntervalExpression((Function) func, interval);
			}
		}
		visitPath.push(parent);
	}

}
