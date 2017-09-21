package jef.database.support;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.DbClient;
import jef.database.DbMetaData;
import jef.database.DbUtils;
import jef.database.IQueryableEntity;
import jef.database.QB;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.jre5support.ProcessUtil;
import jef.tools.StringUtils;
import jef.tools.csvreader.Codecs;
import jef.tools.csvreader.CsvReader;
import jef.tools.reflect.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataInitializer {
    private DbClient session;
    private volatile boolean enable = true;
    private Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private boolean useTable;

    private int tableInit;
    private int recordInit;

    public DataInitializer(DbClient session, boolean useTable) {
        this.session = session;
        DbMetaData meta = session.getMetaData(null);
        try {
            if (useTable) {
                meta.createTable(MetaHolder.getMeta(AllowDataInitialize.class));
                checkEnableFlag();
            } else {
                // 即便没有打开useDataInitTable的开关，只要检测到这张表存在，就认为开关已经打开。
                if (meta.existTable("allow_data_initialize")) {
                    session.refreshTable(AllowDataInitialize.class);
                    useTable = true;
                    checkEnableFlag();
                }
            }
        } catch (SQLException e) {
            throw DbUtils.toRuntimeException(e);
        }
        this.useTable = useTable;
    }

    private void checkEnableFlag() throws SQLException {
        AllowDataInitialize record = session.load(QB.create(AllowDataInitialize.class), false);
        if (record == null) {
            createRecord();
        } else {
            enable = record.isDoInit();
        }
        LogUtil.info("Table [allow_data_initialize] was found, DataInit flag = {}.", enable);
    }

    private void createRecord() throws SQLException {
        AllowDataInitialize record = new AllowDataInitialize();
        record.setDoInit(true);
        session.insert(record);
        enable = true;
    }

    private List<IQueryableEntity> readData(ITableMetadata meta, URL url) throws UnsupportedEncodingException, IOException {
        CsvReader reader = new CsvReader(new InputStreamReader(url.openStream(), "UTF-8"));
        try {
            // 根据Header分析Property
            List<Property> props = new ArrayList<Property>();
            if (reader.readHeaders()) {
                for (String header : reader.getHeaders()) {
                    if (header.charAt(0) == '[') {
                        header = header.substring(1, header.length() - 1);
                    }
                    jef.database.Field field = meta.getField(header);
                    if (field == null) {
                        throw new IllegalArgumentException(String.format("The field [%s] in CSV file doesn't exsts in the entity [%s] metadata.", header,
                                meta.getName()));
                    }
                    props.add(meta.getColumnDef(field).getFieldAccessor());
                }
            }
            // 根据预先分析好的Property读取属性
            List<IQueryableEntity> result = new ArrayList<IQueryableEntity>();
            while (reader.readRecord()) {
                IQueryableEntity obj = meta.newInstance();
                obj.stopUpdate();
                for (int i = 0; i < props.size(); i++) {
                    Property prop = props.get(i);
                    prop.set(obj, Codecs.fromString(reader.get(i), prop.getGenericType()));
                }
                obj.startUpdate();
                result.add(obj);
            }
            return result;
        } finally {
            reader.close();
        }
    }

    private int initData0(ITableMetadata meta, URL url) throws IOException {
        int count = 0;
        List<IQueryableEntity> data = readData(meta, url);
        for (int i = 0; i < data.size(); i += 500) {
            int batchIndex = Math.min(i + 500, data.size());
            try {
                session.batchInsert(data.subList(i, batchIndex));
                count += (batchIndex - i);
            } catch (SQLIntegrityConstraintViolationException e1) {
                // 主键冲突，改为逐条插入
                count += insertOnebyone(data.subList(i, batchIndex));
            } catch (SQLException e1) {
                throw DbUtils.toRuntimeException(e1);
            }
        }
        return count;
    }

    private int insertOnebyone(List<IQueryableEntity> data) {
        int count = 0;
        for (IQueryableEntity e : data) {
            try {
                session.insert(e);
                count++;
            } catch (SQLException e1) {
                log.error("Insert error:{}", e, e1);
            }
        }
        return count;
    }

    private int mergeData0(ITableMetadata meta, URL url) throws IOException {
        int count = 0;
        for (IQueryableEntity e : readData(meta, url)) {
            try {
                session.merge(e);
                count++;
            } catch (SQLException e1) {
                log.error("Insert error:{}", e, e1);
            }
        }
        return count;
    }

    // public static void main(String[] args) throws IOException {
    // CsvReader reader = new CsvReader(new InputStreamReader(new
    // FileInputStream(
    // "G:/Git/ef-orm/orm-code-generator/src/test/resources/com.github.geequery.codegen.entity.ProjectUser.csv"),
    // "UTF-8"));
    //
    // ;
    //
    // System.out.println(reader.getHeaders());
    //
    // reader.close();
    // }

    public final boolean isEnable() {
        return enable;
    }

    /**
     * 对外暴露。初始化制定表的数据
     * 
     * @param meta
     *            表结构元数据
     * @param isNew
     *            表是否刚刚创建
     */
    public final void initData(ITableMetadata meta, boolean isNew) {
        URL url = meta.getThisType().getResource("/" + meta.getThisType().getName() + ".csv");
        if (url != null) {
            if (isNew) {
                LogUtil.info("DataInitializer：table [{}] was created just now, begin insert data into database.", meta.getTableName(false));
            } else {
                LogUtil.info("DataInitializer：table [{}] already exists, begin merge data into database.", meta.getTableName(false));
            }
            try {
                if (isNew) {
                    recordInit += initData0(meta, url);
                } else {
                    recordInit += mergeData0(meta, url);
                }
                tableInit++;
            } catch (RuntimeException e) {
                ex = e;
                throw e;
            } catch (IOException e) {
                ex = e;
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * 记录初始化任务结果
     */
    private void recordResult(Exception ex) {
        if (recordInit > 0) {
            try {
                AllowDataInitialize record = session.load(QB.create(AllowDataInitialize.class), false);
                record.getQuery().setAllRecordsCondition();
                record.prepareUpdate(AllowDataInitialize.Field.doInit, false);
                record.prepareUpdate(AllowDataInitialize.Field.lastDataInitTime, new Date());
                record.prepareUpdate(AllowDataInitialize.Field.lastDataInitUser,
                        ProcessUtil.getPid() + "@" + ProcessUtil.getHostname() + "(" + ProcessUtil.getLocalIp() + ") OS:" + ProcessUtil.getOSName());
                if (ex == null) {
                    record.prepareUpdate(AllowDataInitialize.Field.lastDataInitResult, "success. Tables init = " + tableInit + ", records = " + recordInit);
                } else {
                    record.prepareUpdate(AllowDataInitialize.Field.lastDataInitResult, StringUtils.truncate(ex.toString(), 300));
                }
                session.update(record);
            } catch (SQLException e) {
                log.error("Record DataInitilizer Table failure! please check.", e);
            }
        }
    }

    private Exception ex;

    public void finish() {
        if (useTable) {
            recordResult(ex);
        }
    }

}
