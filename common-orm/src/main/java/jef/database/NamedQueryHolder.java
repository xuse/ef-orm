package jef.database;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jef.common.log.LogUtil;
import jef.database.Condition.Operator;
import jef.database.query.Query;
import jef.database.support.RDBMS;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.JefConfiguration;
import jef.tools.StringUtils;
import jef.tools.XMLUtils;
import jef.tools.reflect.Enums;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

final class NamedQueryHolder {
	private Session parent;
	private Map<String, NQEntry> namedQueries;
	private Map<File, Long> loadedFiles = new HashMap<File, Long>();
	private long lastUpdate;// 记录上次更新文件的时间

	public NamedQueryHolder(Session parent) {
		this.parent = parent;
	}

	public NQEntry get(String name) {
		if (namedQueries == null) {
			initQueries();
			lastUpdate = System.currentTimeMillis();
		}
		if (ORMConfig.getInstance().isCheckUpdateForNamedQueries()) {// 允许动态修改SQL查询
			synchronized (this) {
				if (System.currentTimeMillis() - lastUpdate > 10000) {// 十秒内不更新修改
					checkUpdate(name);
					lastUpdate = System.currentTimeMillis();
				}
			}
		}
		return namedQueries.get(name);
	}

	private void put0(Map<String, NQEntry> namedQueries, NamedQueryConfig namedQueryConfig, RDBMS dialect, String source) {
		String name = namedQueryConfig.getName();
		NQEntry entry = namedQueries.get(name);
		if (entry == null) {
			namedQueries.put(name, new NQEntry(namedQueryConfig, dialect, source));
			return;
		}
		// 尝试替代原有的数据
		{
			NQEntry current = entry;
			while (current != null) {
				if (dialect == current.dialect) {
					// 告警
					String type = dialect == null ? "*" : dialect.name();
					LogUtil.warn("The Named-Query [{}] for {} in [{]}, was replaced by duplicate config from [{}]", name, type, current.getSource(), source);
					current.config = namedQueryConfig;
					return;
				}
				current = current.next;
			}
		}
		// 原有数据没有重复的，添加到链表
		NQEntry newElement = new NQEntry(namedQueryConfig, dialect, source);
		if (dialect == null) {
			newElement.next = entry;
			namedQueries.put(name, newElement);
		} else {
			NQEntry current = entry;
			while (current.next != null) {
				current = current.next;
			}
			current.next = newElement;
		}
	}

	// 检查文件更新
	public void checkUpdate(String name) {
		// 先通过文件日期检查更新
		for (Map.Entry<File, Long> e : loadedFiles.entrySet()) {
			File file = e.getKey();
			if (file.lastModified() > e.getValue()) {// 修改过了
				LogUtil.show("refresh named queries in file <" + file.toString() + ">");
				loadFile(namedQueries, file);
			}
		}
		// 尝试获取
		String tablename = JefConfiguration.get(DbCfg.DB_QUERY_TABLE_NAME);
		if (StringUtils.isNotEmpty(tablename)) {
			NQEntry e = null;
			if (StringUtils.isNotEmpty(name)) {
				e = namedQueries.get(name);
			}
			if (e == null) {// 全刷
				try {
					Query<NamedQueryConfig> q = QB.create(NamedQueryConfig.class);
					q.setCustomTableName(tablename);
					List<NamedQueryConfig> dbQueries = parent.select(q, null);
					for (NamedQueryConfig qc : dbQueries) {
						if (StringUtils.isEmpty(qc.getName())) {
							continue;
						}
						qc.stopUpdate();
						qc.setFromDb(true);
						RDBMS type = processName(qc);
						put0(namedQueries, qc, type, "database");
					}
				} catch (SQLException ex) {
					LogUtil.exception(ex);
				}
			} else { // 单刷
				NamedQueryConfig config = e.config;
				if (config.isFromDb()) {// 到数据库去载入
					NamedQueryConfig q = new NamedQueryConfig();
					q.getQuery().addCondition(NamedQueryConfig.Field.name, Operator.MATCH_START, name);
					try {
						for (NamedQueryConfig nq : parent.select(q)) {
							RDBMS type = processName(nq);
							put0(namedQueries, q, type, "database");
						}
					} catch (SQLException e1) {
						LogUtil.exception(e1);
					}
				}
			}
		}
	}

	/**
	 * 从名称中提取出RDBMS
	 * 
	 * @param q
	 * @return
	 */
	private RDBMS processName(NamedQueryConfig q) {
		String name = q.getName();
		Assert.notNull(name);
		int n = name.indexOf('#');
		if (n > -1) {
			String type = name.substring(n + 1).toLowerCase();
			RDBMS rtype = Enums.valueOf(RDBMS.class, type, "The Database type in namedquery [%s] is unknown.", name);
			name = name.substring(0, n);
			q.setName(name);
			return rtype;
		} else {
			return null;
		}
	}

	// 初始化全部查询
	private synchronized void initQueries() {
		if (namedQueries != null)
			return;
		Map<String, NQEntry> result = new ConcurrentHashMap<String, NQEntry>();
		boolean debugMode = ORMConfig.getInstance().isDebugMode();
		String filename=JefConfiguration.get(DbCfg.NAMED_QUERY_RESOURCE_NAME, "named-queries.xml");
		try {
			// Load from files
			for (URL queryFile : ArrayUtils.toIterable(this.getClass().getClassLoader().getResources(filename))) {
				if (queryFile == null)
					continue;
				if (debugMode) {
					LogUtil.show("loading named queries from file <" + queryFile.toString() + ">");
				}
				File file = IOUtils.urlToFile(queryFile);
				loadFile(result, file);
			}
		} catch (IOException e) {
			LogUtil.exception(e);
		}
		// Load from database
		String tablename = JefConfiguration.get(DbCfg.DB_QUERY_TABLE_NAME);
		try {
			if (StringUtils.isNotEmpty(tablename)) {
				if (debugMode) {
					LogUtil.show("loading named queries in table <" + tablename + ">");
				}
				Query<NamedQueryConfig> q = QB.create(NamedQueryConfig.class);
				q.setCustomTableName(tablename);
				List<NamedQueryConfig> dbQueries = parent.select(q, null);
				for (NamedQueryConfig qc : dbQueries) {
					if (StringUtils.isEmpty(qc.getName())) {
						continue;
					}
					qc.stopUpdate();
					qc.setFromDb(true);
					RDBMS type = processName(qc);
					put0(result, qc, type, "database");
				}
			}
		} catch (SQLException e) {
			LogUtil.exception(e);
		}
		this.namedQueries = result;
	}

	private synchronized void loadFile(Map<String, NQEntry> result, File file) {
		loadedFiles.put(file, file.lastModified());
		try {
			Document doc = XMLUtils.loadDocument(file);
			for (Element e : XMLUtils.childElements(doc.getDocumentElement(), "query")) {
				String name = XMLUtils.attrib(e, "name");
				String type = XMLUtils.attrib(e, "type");
				String sql = XMLUtils.nodeText(e);
				int size = StringUtils.toInt(XMLUtils.attrib(e, "fetch-size"), 0);
				NamedQueryConfig nq = new NamedQueryConfig(name, sql, type, size);
				nq.setTag(XMLUtils.attrib(e, "tag"));
				RDBMS dialect = processName(nq);
				put0(result, nq, dialect, file.getAbsolutePath());
			}
		} catch (SAXException e) {
			LogUtil.exception(e);
		} catch (IOException e) {
			LogUtil.exception(e);
		}
	}
}
