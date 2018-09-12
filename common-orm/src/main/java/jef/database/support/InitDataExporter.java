package jef.database.support;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.geequery.orm.annotation.InitializeData;

import jef.accelerator.asm.ASMUtils;
import jef.accelerator.asm.AnnotationVisitor;
import jef.accelerator.asm.ClassReader;
import jef.accelerator.asm.ClassVisitor;
import jef.accelerator.asm.Opcodes;
import jef.common.log.LogUtil;
import jef.database.DbCfg;
import jef.database.DbClient;
import jef.database.QB;
import jef.database.annotation.EasyEntity;
import jef.database.dialect.type.ColumnMapping;
import jef.database.jpa.JefEntityManagerFactory;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.tools.ClassScanner;
import jef.tools.JefConfiguration;
import jef.tools.csvreader.Codecs;
import jef.tools.csvreader.CsvWriter;
import jef.tools.reflect.Property;
import jef.tools.resource.IResource;

/**
 * 将表中的数据导出成为CSV格式。。
 * 
 * @author jiyi
 *
 */
public class InitDataExporter {
	private final Logger logger = LoggerFactory.getLogger(InitDataExporter.class);
	private DbClient session;
	private boolean deleteEmpty;
	private File target = new File(System.getProperty("user.dir"));
	private String extension = "." + JefConfiguration.get(DbCfg.INIT_DATA_EXTENSION, "txt");
	private String charset = "UTF-8";
	private int maxResults = 5000;

	private final List<URL> classRoot = new ArrayList<>();

	/**
	 * @param emf
	 *            EntityManagerFactory 数据库客户端
	 */
	public InitDataExporter(EntityManagerFactory emf) {
		if (emf instanceof JefEntityManagerFactory) {
			this.session = ((JefEntityManagerFactory) emf).getDefault();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * @param session
	 *            数据库客户端
	 */
	public InitDataExporter(DbClient session) {
		this.session = session;
	}

	/**
	 * 
	 * @param session
	 *            数据库客户端
	 * @param target
	 *            生成的资源文件路径
	 */
	public InitDataExporter(DbClient session, File target) {
		this.session = session;
		this.target = target;
	}

	/**
	 * 导出制定的实体类数据
	 * 
	 * @param clz
	 */
	public void export(@SuppressWarnings("rawtypes") Class clz) {
		try {
			export0(clz);
		} catch (SQLException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * 将数据库中的数据全部作为初始化数据导出到src/main/resources下。
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 * 
	 * @throws Exception
	 */
	public void exportPackage(String... packageName) throws ClassNotFoundException, IOException {
		// 只有第一个Classpath是要扫描的
		ClassLoader loader = this.getClass().getClassLoader();
		if (!classRoot.isEmpty()) {
			loader = new URLClassLoader(classRoot.toArray(new URL[classRoot.size()]), this.getClass().getClassLoader());
			Thread.currentThread().setContextClassLoader(loader);
		}
		URL url = classRoot.isEmpty() ? null : classRoot.get(0);
		IResource[] entities = ClassScanner.listClassNameInPackage(url, packageName, false);
		LogUtil.info("Scanned {}, found {}", Arrays.toString(packageName), entities.length);
		for (IResource clz : entities) {
			ClassReader cl = new ClassReader(clz.getInputStream(), true);
			ClassAnnotationExtracter ae = new ClassAnnotationExtracter();
			cl.accept(ae, ClassReader.SKIP_CODE);
			if (ae.hasAnnotation(Entity.class) || ae.hasAnnotation(EasyEntity.class)) {
				Class<?> e;
				try {
					e = loader.loadClass(cl.getJavaClassName());
				} catch (ClassNotFoundException ex) {
					String path = null;
					if (loader instanceof URLClassLoader) {
						path = Arrays.toString(((URLClassLoader) loader).getURLs());
					}
					LogUtil.error("Not found {} in classloader {}", cl.getJavaClassName(), path);
					throw ex;
				}

				InitializeData data = e.getAnnotation(InitializeData.class);
				if (data != null)
					logger.info("Starting export data:{}", e.getName());
				export(e);
			}
		}
	}

	private void export0(@SuppressWarnings("rawtypes") Class clz) throws SQLException, IOException {
		ITableMetadata meta = MetaHolder.getMeta(clz);
		if (meta == null)
			return;
		if (!session.getMetaData(null).existTable(meta.getTableName(true))) {
			return;
		}
		File file = new File(target, meta.getThisType().getName() + extension);
		@SuppressWarnings("unchecked")
		Query<?> query = QB.create(clz);
		query.setCascade(false);
		query.setFetchSize(2000);
		query.setMaxResult(this.maxResults);
		List<?> o = session.select(query);
		if (o.isEmpty()) {
			if (deleteEmpty && file.exists()) {
				file.delete();
			}
			return;
		}

		CsvWriter cw = new CsvWriter(file, ',', charset);
		try {
			Collection<ColumnMapping> columns = meta.getColumns();
			for (ColumnMapping column : columns) {
				cw.write("[" + column.fieldName() + "]");
			}
			cw.endRecord();
			for (Object obj : o) {
				for (ColumnMapping column : columns) {
					Property accessor = column.getFieldAccessor();
					String data = Codecs.toString(accessor.get(obj), accessor.getGenericType());
					cw.write(data);
				}
				cw.endRecord();
			}
		} finally {
			cw.close();
		}
		logger.info("{} was updated.", file.getAbsolutePath());
	}

	static class ClassAnnotationExtracter extends ClassVisitor {
		private Set<String> annotations = new HashSet<String>();

		public ClassAnnotationExtracter() {
			super(Opcodes.ASM5);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			annotations.add(desc);
			return null;
		}

		public boolean hasAnnotation(Class<? extends Annotation> clzName) {
			return annotations.contains(ASMUtils.getDesc(clzName));
		}
	}

	public String getCharset() {
		return charset;
	}

	public void addClassRoot(URL classRoot) {
		this.classRoot.add(classRoot);
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}
}
