package jef.database.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.GenerationType;

import jef.accelerator.asm.ClassReader;
import jef.common.log.LogUtil;
import jef.database.Field;
import jef.database.annotation.EasyEntity;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.MappingType;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.Column;
import jef.database.meta.ColumnModification;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.support.executor.StatementExecutor;
import jef.tools.ArrayUtils;
import jef.tools.ClassScanner;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

/**
 * 自动扫描工具，在构造时可以根据构造方法，自动的将继承DataObject的类检查出来，并载入
 * 
 * @author Administrator
 * 
 */
public class QuerableEntityScanner {

	public static final Set<String> dynamicEnhanced = new HashSet<String>();

	// implClasses
	private String[] implClasses = new String[] { "jef.database.DataObject" };
	/**
	 * 是否扫描子包
	 */
	private boolean scanSubPackage = true;
	/**
	 * 是否允许删除列
	 */
	private boolean allowDropColumn;

	private boolean createTable = true;

	private boolean alterTable = true;
	
	private boolean checkSequence=true;

	/**
	 * 扫描包
	 */
	private String[] packageNames = { "jef" };
	/**
	 * EMF
	 */
	private JefEntityManagerFactory entityManagerFactory;

	public String[] getPackageNames() {
		return packageNames;
	}

	public void setPackageNames(String packageNames) {
		this.packageNames = packageNames.split(",");
	}

	public boolean isScanSubPackage() {
		return scanSubPackage;
	}

	public String[] getImplClasses() {
		return implClasses;
	}

	/**
	 * 设置多个DataObject类
	 * 
	 * @param implClasses
	 */
	public void setImplClasses(String[] implClasses) {
		this.implClasses = implClasses;
	}

	@SuppressWarnings("rawtypes")
	public void setImplClasses(Class... implClasses) {
		String[] result = new String[implClasses.length];
		for (int i = 0; i < implClasses.length; i++) {
			result[i] = implClasses[i].getName();
		}
		this.implClasses = result;
	}

	public void setScanSubPackage(boolean scanSubPackage) {
		this.scanSubPackage = scanSubPackage;
	}

	public void doScan() {
		String[] parents = getClassNames();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = QuerableEntityScanner.class.getClassLoader();

		// 开始
		ClassScanner cs = new ClassScanner();
		Set<String> classes = cs.scan(packageNames);

		// 循环所有扫描到的类
		for (String s : classes) {
			try {
				// 读取类
				URL url = cl.getResource(s.replace('.', '/') + ".class");
				if (url == null)
					continue;
				InputStream stream = url.openStream();
				if (stream == null) {
					LogUtil.error("The class content [" + s + "] not found!");
					continue;
				}
				ClassReader cr = new ClassReader(stream);
				IOUtils.closeQuietly(stream);

				// 根据父类判断
				String superName = cr.getSuperName();
				if (!ArrayUtils.contains(parents, superName)) {// 是实体
					continue;
				}

				// 加载或初始化
				Class<?> clz = loadClass(cl, s);
				if (clz != null) {
					registeEntity(clz);
				}
			} catch (IOException e) {
				LogUtil.exception(e);
			}
		}
	}

	private Class<?> loadClass(ClassLoader cl, String s) {
		try {
			Class<?> c = cl.loadClass(s);
			return c;
		} catch (ClassNotFoundException e) {
			LogUtil.error("Class not found:" + e.getMessage());
			return null;
		}
	}

	public boolean registeEntity(String name) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = this.getClass().getClassLoader();
		}
		try {
			Class<?> c = cl.loadClass(name);
			registeEntity(c);
			return true;
		} catch (Exception e) {
			LogUtil.exception(e);
			return false;
		}
	}

	private void registeEntity(Class<?> c) {
		try {
			ITableMetadata meta = MetaHolder.getMeta(c);// 用initMeta变为强制初始化。getMeta更优雅一点
			if (meta != null) {
				LogUtil.info("Table [" + meta.getTableName(true) + "] <--> [" + c.getName() + "]");
			} else {
				LogUtil.error("Entity [" + c.getName() + "] was not mapping to any table.");
			}
			EasyEntity ee = c.getAnnotation(EasyEntity.class);
			final boolean create = createTable && (ee == null || ee.create());
			final boolean refresh = alterTable && (ee == null || ee.refresh());
			if (entityManagerFactory != null && (create || refresh)) {
				doTableDDL(meta,create,refresh);
			}
		} catch (Throwable e) {
			LogUtil.error("EntityScanner:[Failure]" + StringUtils.exceptionStack(e));
		}
	}

	private void doTableDDL(ITableMetadata meta,final boolean create, final boolean refresh) throws SQLException {
		entityManagerFactory.getDefault().refreshTable(meta, new MetadataEventListener() {
			public void onTableFinished(ITableMetadata meta, String tablename) {
			}

			public boolean onTableCreate(ITableMetadata meta, String tablename) {
				return create;
			}

			public boolean onSqlExecuteError(SQLException e, String tablename, String sql, List<String> sqls, int n) {
				LogUtil.error("[ALTER-TABLE]. SQL:[{}] ERROR.\nMessage:[{}]", sql, e.getMessage());
				return true;
			}

			public boolean onCompareColumns(String tablename, List<Column> columns, Map<Field, ColumnType> defined) {
				return refresh;
			}

			public boolean onColumnsCompared(String tablename, ITableMetadata meta, Map<String, ColumnType> insert, List<ColumnModification> changed, List<String> delete) {
				if (!allowDropColumn) {
					delete.clear();
				}
				return true;
			}

			public void onAlterSqlFinished(String tablename, String sql, List<String> sqls, int n, long cost) {
			}

			public boolean beforeTableRefresh(ITableMetadata meta, String table) {
				return true;
			}

			public void beforeAlterTable(String tablename, ITableMetadata meta, StatementExecutor conn, List<String> sql) {
			}
		});
		if(checkSequence){
			for(MappingType<?> f:meta.getMetaFields()){
				if(f instanceof AutoIncrementMapping){
					AutoIncrementMapping<?> m=(AutoIncrementMapping<?>)f;
					GenerationType gt=((AutoIncrementMapping<?>) f).getGenerationType(entityManagerFactory.getDefault().getProfile(meta.getBindDsName()));
					if(gt==GenerationType.SEQUENCE || gt==GenerationType.TABLE){
						entityManagerFactory.getDefault().getSequenceManager().getSequence(m, meta.getBindDsName());					
					}
					
				}
			}
		}
	}

	private String[] getClassNames() {
		List<String> clzs = new ArrayList<String>();
		for (int i = 0; i < implClasses.length; i++) {
			String s = implClasses[i];
			s = StringUtils.trimToNull(s);
			if (s == null)
				continue;
			clzs.add(s.replace('.', '/'));
		}
		return clzs.toArray(new String[clzs.size()]);
	}

	public boolean isAllowDropColumn() {
		return allowDropColumn;
	}

	public void setAllowDropColumn(boolean allowDropColumn) {
		this.allowDropColumn = allowDropColumn;
	}

	public void setEntityManagerFactory(JefEntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	public boolean isCreateTable() {
		return createTable;
	}

	public void setCreateTable(boolean createTable) {
		this.createTable = createTable;
	}

	public boolean isAlterTable() {
		return alterTable;
	}

	public void setAlterTable(boolean alterTable) {
		this.alterTable = alterTable;
	}

	public boolean isCheckSequence() {
		return checkSequence;
	}

	public void setCheckSequence(boolean checkSequence) {
		this.checkSequence = checkSequence;
	}
}
