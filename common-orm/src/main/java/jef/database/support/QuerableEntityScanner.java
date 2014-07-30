package jef.database.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jef.accelerator.asm.Attribute;
import jef.accelerator.asm.ClassReader;
import jef.accelerator.asm.ClassVisitor;
import jef.codegen.EnhanceTaskASM;
import jef.common.log.LogUtil;
import jef.common.wrapper.Holder;
import jef.database.Field;
import jef.database.IQueryableEntity;
import jef.database.annotation.EasyEntity;
import jef.database.dialect.ColumnType;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.Column;
import jef.database.meta.ColumnModification;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.tools.ArrayUtils;
import jef.tools.ClassScanner;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.reflect.ClassLoaderUtil;
import jef.tools.reflect.UnsafeUtils;

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
	// 选项
	private boolean scanSubPackage = true;

	// 刷新表的时候不要删除列
	private boolean notDropColumn = true;

	//
	private String[] packageNames = { "jef" };

	private JefEntityManagerFactory entityManagerFactory;

	private boolean checkEnhance;

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

	public boolean isCheckEnhance() {
		return checkEnhance;
	}

	public void setCheckEnhance(boolean checkEnhance) {
		this.checkEnhance = checkEnhance;
	}

	public void setScanSubPackage(boolean scanSubPackage) {
		this.scanSubPackage = scanSubPackage;
	}

	public void doScan() {
		String[] parents = getClassNames();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = QuerableEntityScanner.class.getClassLoader();

		ClassLoader jefCl = getJefClassLoader(cl);
		if (jefCl != null) {
			checkEnhance = false;
		}

		// 开始
		ClassScanner cs = new ClassScanner();
		Set<String> classes = cs.scan(packageNames);

		// 如果要增强，那么记录已经加载过的类，以防增强实现再加载一遍。父加载器不再判断
		Set<String> clzz = new HashSet<String>();
		if (checkEnhance) {
			for (Class<?> clz : ClassLoaderUtil.getClassesLoadedBy(cl)) {
				clzz.add(clz.getName());
			}
		}
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
				stream.close();

				// 根据父类判断
				String superName = cr.getSuperName();
				if (!ArrayUtils.contains(parents, superName)) {// 是实体
					continue;
				}
				
				//加载或初始化
				Class<?> clz;
				if (!checkEnhance || clzz.contains(s)) {
					clz=loadClass(cl,s);
				} else {
					clz = checkAndRuntimeEnhance(cr, s, url, cl);
				}
				if(clz!=null){
					registeEntity(clz);
				}
			} catch (IOException e) {
				LogUtil.exception(e);
			}
		}
	}

	private ClassLoader getJefClassLoader(ClassLoader threadContextLoader) {
		ClassLoader currentLoader = threadContextLoader;
		while (currentLoader != null) {
			if ("jef.database.JefClassLoader".equals(currentLoader.getClass().getName())) {
				return currentLoader;
			}
			currentLoader = currentLoader.getParent();
		}
		return null;
	}

	private Class<?> checkAndRuntimeEnhance(ClassReader cr, String s, URL url, ClassLoader cl) {
		final Holder<Boolean> checkd = new Holder<Boolean>(false);
		cr.accept(new ClassVisitor() {
			public void visitAttribute(Attribute attr) {
				if ("jefd".equals(attr.type)) {
					checkd.set(true);
				}
				super.visitAttribute(attr);
			}
		}, ClassReader.SKIP_CODE);
		//已经增强过的代码，直接加载返回
		if(checkd.get()){
			return loadClass(cl, s); 
		}
		//尝试动态增强加载(有风险)
		try {
			return enhanceClassAndLoader(cr, cl, s);
		} catch (IOException e) {
			return null;
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

	private Class<?> enhanceClassAndLoader(ClassReader cr, ClassLoader cl, String clzName) throws IOException {
		
		EnhanceTaskASM enhancer = new EnhanceTaskASM(null,null);
		
		InputStream fieldClz = cl.getResourceAsStream(clzName.replace('.', '/').concat("$Field.class"));
		if(fieldClz==null)return loadClass(cl, clzName);
		
		
		List<String> fields = enhancer.parseEnumFields(IOUtils.toByteArray(fieldClz));
		byte[] result = enhancer.enhanceClass(cr, fields);

		if (result != null && result.length > 0) {
			try {
				//强制在当前上下文线程ClassLoader加载会引起很多古怪的问题，这样做有相当的风险
				Class<?> clz = UnsafeUtils.defineClass(clzName, result, 0, result.length, cl);
				LogUtil.info("Class [{}] was not enhanced, runtime enhance successful.", clzName);
				dynamicEnhanced.add(clzName);
				return clz;
			} catch (Throwable t) {
			}
		}
		return loadClass(cl, clzName);
	}

	private void registeEntity(Class<?> c) {
		try {
			ITableMetadata meta = MetaHolder.getMeta(c.asSubclass(IQueryableEntity.class));//用initMeta变为强制初始化。getMeta更优雅一点
			if (meta != null) {
				LogUtil.info("EntityScanner:" + meta.getTableName(true) + " was mapping to " + c.getName());
			} else {
				LogUtil.info("EntityScanner:" + c.getName() + " was not mapping to any table.");
			}
			EasyEntity ee=c.getAnnotation(EasyEntity.class);
			final boolean create=(ee==null || ee.create());
			final boolean refresh=(ee==null || ee.refresh()); 
			
			if (entityManagerFactory != null && (create || refresh)) {
				entityManagerFactory.getDefault().refreshTable(meta, new MetadataEventListener() {
					public void onTableFinished(ITableMetadata meta, String tablename) {
					}

					public boolean onTableCreate(ITableMetadata meta, String tablename) {
						return create;
					}

					public boolean onSqlExecuteError(SQLException e, String tablename, String sql, List<String> sqls, int n) {
						return false;
					}

					public boolean onCompareColumns(String tablename, List<Column> columns, Map<Field, ColumnType> defined) {
						return refresh;
					}

					public boolean onColumnsCompared(String tablename, ITableMetadata meta, Map<String, ColumnType> insert, List<ColumnModification> changed, List<String> delete) {
						if (notDropColumn) {
							delete.clear();
						}
						return true;
					}

					public void onAlterSqlFinished(String tablename, String sql, List<String> sqls, int n, long cost) {
					}

					public boolean beforeTableRefresh(ITableMetadata meta, String table) {
						return true;
					}

					public void beforeAlterTable(String tablename, ITableMetadata meta, Connection conn, List<String> sql) {
					}
				});
			}
		} catch (Throwable e) {
			LogUtil.error("EntityScanner:[Failure]" + StringUtils.exceptionStack(e));
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

	public boolean isNotDropColumn() {
		return notDropColumn;
	}

	public void setNotDropColumn(boolean notDropColumn) {
		this.notDropColumn = notDropColumn;
	}

	public void setEntityManagerFactory(JefEntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}
}
