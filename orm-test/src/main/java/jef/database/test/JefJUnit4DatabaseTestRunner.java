package jef.database.test;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbUtils;
import jef.database.datasource.MapDataSourceLookup;
import jef.database.datasource.RoutingDataSource;
import jef.database.datasource.SimpleDataSource;
import jef.database.meta.MetaHolder;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.io.Charsets;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.FieldEx;

/**
 * jef的单元测试工具
 * 
 * @author jiyi
 * 
 */
public class JefJUnit4DatabaseTestRunner extends BlockJUnit4ClassRunner {
	final Map<String, Object> connections = new HashMap<String, Object>();
	private boolean isRouting;
	private DbClient routingDbClient;
	private Properties pro = new Properties();

	static class DbConnectionHolder {
		DbConnectionHolder(DataSource ds, DbClient db) {
			this.datasource = ds;
			this.db = db;
		}

		DataSource datasource;
		DbClient db;

	}

	private void initContext(DataSourceContext annotation) {
		if (annotation == null) {
			throw new IllegalArgumentException("Please assign a @DataSourceContext on this class.");
		}
		this.isRouting = annotation.routing();
		Reader reader = IOUtils.getReader(this.getClass().getClassLoader().getResource("junit4jef.properties"),Charsets.UTF8);
		if (reader != null) {
			try {
				pro.load(reader);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (DataSource ds : annotation.value()) {
			String url = ds.url();
			if (url.isEmpty())
				continue;
			url = apply(url);
			if (url.isEmpty()) {
				LogUtil.warn("The case {} with datasource {} was not config.", this.getName(), ds.url());
				continue;
			}
			connections.put(ds.name(), ds);
		}
	}

	@Override
	protected List<FrameworkMethod> getChildren() {
		List<FrameworkMethod> methods = super.getChildren();
		if (isRouting || connections.isEmpty()) {
			return methods;
		}
		List<FrameworkMethod> result = new ArrayList<FrameworkMethod>();
		for (String s : connections.keySet()) {
			for (FrameworkMethod me : methods) {
				IgnoreOn at = me.getMethod().getAnnotation(IgnoreOn.class);
				if (at == null || isNotIgnore(at, s)) {
					result.add(new DbFrameworkMethod(s, me.getMethod()));
				}
			}
		}
		return result;
	}

	private boolean isNotIgnore(IgnoreOn at, String s) {
		if (at.allButExcept().length == 0) {
			return !ArrayUtils.contains(at.value(), s);
		} else {
			return ArrayUtils.contains(at.allButExcept(), s);
		}
	}

	@Override
	public void filter(final Filter raw) throws NoTestsRemainException {
		super.filter(new Filter() {
			@Override
			public boolean shouldRun(Description description) {
				//String testDisplay = StringUtils.substringBefore(description.getDisplayName(), " ");
				//String last=StringUtils.substringAfter(description.getDisplayName(), "(");
//				if (testDisplay != description.getDisplayName()) {
//					description = Description.createTestDescription(description.getTestClass(), testDisplay);
//				}
				boolean flag=raw.shouldRun(description);
			//	System.out.println(description + "--" + flag);
				return flag;
			}

			@Override
			public String describe() {
				return raw.describe();
			}
		});
	}

	@Override
	protected Statement methodInvoker(FrameworkMethod method, Object test) {
		boolean isNew = false;
		boolean inject = false;
		if (method instanceof DbFrameworkMethod) {
			DbFrameworkMethod dbCase = (DbFrameworkMethod) method;
			String dbType = dbCase.getDbType();
			Object obj = connections.get(dbType);
			if (obj == null) {
				throw new IllegalArgumentException("Database " + dbType + " is unknown.");
			}
			DbConnectionHolder holder;
			if (obj instanceof DataSource) {
				holder = createDbClient((DataSource) obj);// CreateDbClient
				isNew = true;
			} else {
				holder = (DbConnectionHolder) obj;
				if (holder.db == null) {
					holder = createDbClient(holder.datasource);// CreateDbClient
				}
			}
			inject(test, holder.db, holder.datasource.field());
			inject=true;
		} else if (isRouting) {
			if (routingDbClient == null) {
				MapDataSourceLookup lookup = new MapDataSourceLookup();
				for (Entry<String, Object> entry : connections.entrySet()) {
					if (entry.getValue() instanceof DataSource) {
						DataSource ds = (DataSource) entry.getValue();
						lookup.put(ds.name(), new SimpleDataSource(ds.url(),ds.user(),ds.password()));
					}
				}
				RoutingDataSource rds = new RoutingDataSource(lookup);
				routingDbClient = new DbClient(rds);
				isNew = true;
			}
			inject(test, routingDbClient, "");
			inject=true;
		}
		if (isNew) {
			List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(DatabaseInit.class);
			try {
				for (FrameworkMethod m : methods) {
					printMethod(m, method);
					m.getMethod().invoke(test);
				}
			} catch (Exception e) {
				throw DbUtils.toRuntimeException(e);
			}
		}
		printMethod(method, null);
		if(inject){
			return super.methodInvoker(method, test);
		}else{
			System.err.println("数据库未配置，跳过测试："+super.describeChild(method).getDisplayName());
			return new ExpectException(null, NullPointerException.class);
		}
	}

	private void printMethod(FrameworkMethod m, FrameworkMethod parentMethod) {
		String name = m.getMethod().getDeclaringClass().getName() + "." + m.getName();
		if (parentMethod instanceof DbFrameworkMethod) {
			name = name + "@" + ((DbFrameworkMethod) parentMethod).dbType;
		} else if (m instanceof DbFrameworkMethod) {
			name = name + "@" + ((DbFrameworkMethod) m).dbType;
		}
		System.out.println("======================== " + name + " ==========================");
	}

	private void inject(Object test, DbClient db, String field) {
		if (StringUtils.isEmpty(field)) {
			field = "db";
		}
		FieldEx f = BeanUtils.getField(test.getClass(), field);
		if (f == null) {
			throw new IllegalStateException("The class " + test.getClass() + " must have a field named '" + field + "'.");
		}
		try {
			f.set(test, db);
		} catch (Exception e) {
			throw new IllegalStateException("Cann't inject DbClient into '" + field + "' on " + test);
		}
	}

	private DbConnectionHolder createDbClient(DataSource ds) {
		MetaHolder.clear();
		DbClient db = new DbClientBuilder(apply(ds.url()), apply(ds.user()), apply(ds.password())).build();
		DbConnectionHolder holder = new DbConnectionHolder(ds, db);
		connections.put(ds.name(), holder);
		return holder;
	}

	private String apply(String string) {
		if (pro.isEmpty()) {
			return string;
		}
		return StringUtils.convertProperty(string, pro);
	}

	@Override
	protected String testName(FrameworkMethod method) {
		if (method instanceof DbFrameworkMethod) {
			return method.toString();
		} else {
			return super.testName(method);
		}
	}

	@Override
	public void run(RunNotifier notifier) {
		notifier.addListener(new RunListener() {
			public void testRunFinished(Result result) throws Exception {
				super.testRunFinished(result);
				if (routingDbClient != null) {
					close(routingDbClient, "");
					routingDbClient = null;
				}
				for (Object obj : connections.values()) {
					if (obj instanceof DbConnectionHolder) {
						DbConnectionHolder holder = (DbConnectionHolder) obj;
						close(holder.db, holder.datasource.field());
						holder.db = null;
					}
				}
			}

			@Override
			public void testFailure(Failure failure) throws Exception {
				super.testFailure(failure);
				System.err.println("!!!Failure!!!");
				failure.getException().printStackTrace();
			}
		});
		super.run(notifier);
	}

	private void close(DbClient db, String field) {
		List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(DatabaseDestroy.class);
		if (!methods.isEmpty()) {
			try {
				Object test = super.createTest();
				inject(test, db, field);
				for (FrameworkMethod m : methods) {
					m.getMethod().invoke(test);
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		try {
			db.close();
		} catch (Exception e) {
		}
		;
	}

	static class DbFrameworkMethod extends FrameworkMethod {
		public DbFrameworkMethod(String type, Method method) {
			super(method);
			this.dbType = type;
		}

		String dbType;

		public final String getDbType() {
			return dbType;
		}

		public final void setDbType(String dbType) {
			this.dbType = dbType;
		}

		@Override
		public String toString() {
			return getMethod().getName() + " @" + dbType;
		}
	}

	public JefJUnit4DatabaseTestRunner(Class<?> klass) throws InitializationError {
		super(klass);
		initContext(klass.getAnnotation(DataSourceContext.class));
	}

}
