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

import com.github.geequery.asm.ClassReader;

import jef.accelerator.asm.ASMUtils;
import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.Field;
import jef.database.SessionFactory;
import jef.database.annotation.EasyEntity;
import jef.database.dialect.ColumnType;
import jef.database.dialect.type.AutoIncrementMapping;
import jef.database.dialect.type.AutoIncrementMapping.GenerationResolution;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ColumnModification;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.meta.object.Column;
import jef.database.wrapper.executor.StatementExecutor;
import jef.tools.ArrayUtils;
import jef.tools.ClassScanner;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.resource.IResource;

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
	 * 是否创建不存在的表
	 */
	private boolean createTable = true;

	/**
	 * 是否修改存在的表
	 */
	private boolean alterTable = true;

	/**
	 * 当alterTable=true时，如果修改表时需要删除列，是否允许删除列
	 */
	private boolean allowDropColumn;

	/**
	 * 是否检查序列
	 */
	private boolean checkSequence = true;

	/**
	 * 是否检查索引
	 */
	private boolean checkIndex = true;

	/**
	 * 当创建新表后，是否同时初始化表中的数据
	 */
	private boolean initData = true;

	/**
	 * 扫描包
	 */
	private String[] packageNames = { "jef" };
	/**
	 * EMF
	 */
	private SessionFactory entityManagerFactory;
	/**
	 * DataInit
	 * 
	 * @return
	 */
	private DataInitializer dataInitializer;

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
		IResource[] classes = cs.scan(packageNames);

		// 循环所有扫描到的类
		for (IResource s : classes) {
			try {
				ClassReader cr = getClassInfo(cl, s);
				if (cr == null)// NOT found class
					continue;
				// 根据父类判断
				if (isEntiyClz(cl, parents, cr.getSuperName())) {
					Class<?> clz = loadClass(cl, ASMUtils.getJavaClassName(cr));
					if (clz != null) {
						registeEntity0(clz);
					}
				}
				;
			} catch (IOException e) {
				LogUtil.exception(e);
			}
		}
	}

	private boolean isEntiyClz(ClassLoader cl, String[] knownSuperNames, String superName) throws IOException {
		if ("java/lang/Object".equals(superName)) {
			return false;
		}
		if (ArrayUtils.contains(knownSuperNames, superName)) {// 是实体
			return true;
		}
		// 读取类
		ClassReader cr = getClassInfo(cl, superName);
		if (cr == null) {
			return false;
		}
		return isEntiyClz(cl, knownSuperNames, cr.getSuperName());
	}

	private ClassReader getClassInfo(ClassLoader cl, String s) throws IOException {
		URL url = cl.getResource(s.replace('.', '/') + ".class");
		if (url == null)
			return null;
		InputStream stream = url.openStream();
		if (stream == null) {
			LogUtil.error("The class content [" + s + "] not found!");
			return null;
		}
		return new ClassReader(IOUtils.toByteArray(stream));
	}

	private ClassReader getClassInfo(ClassLoader cl, IResource s) throws IOException {
		InputStream stream = s.getInputStream();
		if (stream == null) {
			LogUtil.error("The class content [" + s + "] not found!");
			return null;
		}
		return new ClassReader(IOUtils.toByteArray(stream));
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

	public boolean registeEntity(String... names) {
		if (names == null)
			return true;
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null) {
			cl = this.getClass().getClassLoader();
		}
		for (String name : names) {
			if (StringUtils.isEmpty(name)) {
				continue;
			}
			Class<?> c;
			try {
				c = cl.loadClass(name);
				registeEntity0(c);
			} catch (ClassNotFoundException e) {
				LogUtil.error("Class not found:" + e.getMessage());
			}
		}
		return true;
	}

	private void registeEntity0(Class<?> c) {
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
				boolean isCreated = doTableDDL(meta, create, refresh);
				if (dataInitializer.isEnable() && initData) {
					dataInitializer.initData(meta, isCreated);
				}
			}
		} catch (Exception e) {
			LogUtil.error("EntityScanner:[Failure]" + StringUtils.exceptionStack(e));
		}
	}

	/**
	 * 
	 * @param meta
	 *            表结构数据
	 * @param doCreateTask
	 *            是否创建
	 * @param refresh
	 *            是否更新
	 * @return 是否完成了表的新建
	 * @throws SQLException
	 */
	private boolean doTableDDL(ITableMetadata meta, final boolean doCreateTask, final boolean refresh) throws SQLException {
		// 不管是否存在，总之先创建一次
		DbClient client = entityManagerFactory.asDbClient();

		boolean newTable = false;
		if (doCreateTask) {
			newTable = client.createTable(meta) > 0;
		}
		boolean exists = newTable ? true : client.existsTable(meta.getTableName(true));
		if (!exists) {
			return false;
		}

		if (!newTable) {
			client.refreshTable(meta, new MetadataEventListener() {
				public void onTableFinished(ITableMetadata meta, String tablename) {
				}

				public boolean onTableCreate(ITableMetadata meta, String tablename) {
					return doCreateTask;
				}

				public boolean onSqlExecuteError(SQLException e, String tablename, String sql, List<String> sqls, int n) {
					LogUtil.error("[ALTER-TABLE]. SQL:[{}] ERROR.\nMessage:[{}]", sql, e.getMessage());
					return true;
				}

				public boolean onCompareColumns(String tablename, List<Column> columns, Map<Field, ColumnMapping> defined) {
					return refresh;
				}

				public boolean onColumnsCompared(String tablename, ITableMetadata meta, Map<String, ColumnType> insert, List<ColumnModification> changed,
						List<String> delete) {
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
		}
		// 检查Sequence
		if (checkSequence) {
			for (ColumnMapping f : meta.getColumns()) {
				if (f instanceof AutoIncrementMapping) {
					AutoIncrementMapping m = (AutoIncrementMapping) f;
					GenerationResolution gt = ((AutoIncrementMapping) f).getGenerationType(entityManagerFactory.asDbClient().getProfile(meta.getBindDsName()));
					if (gt == GenerationResolution.SEQUENCE || gt == GenerationResolution.TABLE) {
						entityManagerFactory.asDbClient().getSequenceManager().getSequence(m, meta.getBindDsName());
					}

				}
			}
		}
		return newTable;
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

	/**
	 * 设置数据库访问句柄
	 * 
	 * @param entityManagerFactory
	 *            数据库客户端
	 * @param useTable
	 *            是否使用数据库记录表
	 * @param charset
	 *            数据文件编码
	 */
	public void setEntityManagerFactory(SessionFactory entityManagerFactory, boolean useTable, String charset, String extName, String initRoot) {
		this.entityManagerFactory = entityManagerFactory;
		this.dataInitializer = new DataInitializer(entityManagerFactory.asDbClient(), useTable, charset, extName, initRoot);
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

	public void setInitData(boolean initData) {
		this.initData = initData;
	}

	public boolean isInitData() {
		return initData;
	}

	public boolean isCheckIndex() {
		return checkIndex;
	}

	public void setCheckIndex(boolean checkIndex) {
		this.checkIndex = checkIndex;
	}

	/**
	 * 完成类的注册和扫描。如果启用了数据初始化记录表，将会更新该表记录，下次启动不再初始化数据。
	 */
	public void finish() {
		if (dataInitializer != null && dataInitializer.isEnable()) {
			dataInitializer.finish();
		}
	}
}
