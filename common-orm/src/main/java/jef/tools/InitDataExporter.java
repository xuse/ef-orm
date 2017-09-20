package jef.tools;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import jef.database.DbClient;
import jef.database.QB;
import jef.database.dialect.type.ColumnMapping;
import jef.database.meta.ITableMetadata;
import jef.database.meta.MetaHolder;
import jef.database.query.Query;
import jef.tools.csvreader.Codecs;
import jef.tools.csvreader.CsvWriter;
import jef.tools.reflect.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 将表中的数据导出成为CSV。
 * @author jiyi
 *
 */
public class InitDataExporter {
    private final Logger logger=LoggerFactory.getLogger(jef.tools.InitDataExporter.class);
    private DbClient session;
    private boolean deleteEmpty;
    private File rootPath;

    /**
     * @param session 数据库客户端
     */
    public InitDataExporter(DbClient session) {
        this.session = session;
        this.rootPath = new File(System.getProperty("user.dir"));
    }

    /**
     * 
     * @param session 数据库客户端
     * @param target 生成的资源文件路径
     */
    public InitDataExporter(DbClient session, File target) {
        this.session = session;
        this.rootPath = target;
    }

    /**
     * 导出制定的实体类数据
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

    private void export0(@SuppressWarnings("rawtypes") Class clz) throws SQLException, IOException {
        ITableMetadata meta = MetaHolder.getMeta(clz);
        if (meta == null)
            return;

        File file = new File(rootPath, meta.getThisType().getName() + ".csv");
        @SuppressWarnings("unchecked")
        Query<?> query = QB.create(clz);
        query.setCascade(false);
        List<?> o = session.select(query);
        if (o.isEmpty()) {
            if (deleteEmpty && file.exists()) {
                file.delete();
            }
            return;
        }

        CsvWriter cw = new CsvWriter(file, ',', "UTF-8");
        try{
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
        }finally{
            cw.close();    
        }
        logger.info("{} was updated.",file.getAbsolutePath());
    }

}
