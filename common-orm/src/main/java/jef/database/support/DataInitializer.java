package jef.database.support;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jef.database.DbClient;
import jef.database.IQueryableEntity;
import jef.database.meta.ITableMetadata;
import jef.tools.csvreader.Codecs;
import jef.tools.csvreader.CsvReader;
import jef.tools.reflect.Property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataInitializer {
    private DbClient session;
    private Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public DataInitializer(DbClient session) {
        this.session = session;
    }

    private List<IQueryableEntity> readData(ITableMetadata meta, URL url) throws UnsupportedEncodingException, IOException {
        CsvReader reader = new CsvReader(new InputStreamReader(url.openStream(), "UTF-8"));
        try {
            // 根据Header分析Property
            List<Property> props = new ArrayList<Property>();
            if (reader.readHeaders()) {
                for (String header : reader.getHeaders()) {
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

    public int initData(ITableMetadata meta, URL url) {
        try {
            int count = 0;
            for (IQueryableEntity e : readData(meta, url)) {
                try {
                    session.insert(e);
                    count++;
                } catch (SQLException e1) {
                    log.error("Insert error:{}", e, e1);
                }
            }
            return count;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    

    public int mergeData(ITableMetadata meta, URL url) {
        try {
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
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

    }

    public static void main(String[] args) throws IOException {
        CsvReader reader = new CsvReader(new InputStreamReader(new FileInputStream(
                "G:/Git/ef-orm/orm-code-generator/src/test/resources/com.github.geequery.codegen.entity.ProjectUser.csv"), "UTF-8"));

        ;

        System.out.println(reader.getHeaders());

        reader.close();
    }
}
