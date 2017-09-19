package jef.codegen;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jef.database.DbUtils;
import jef.database.datasource.SimpleDataSource;

import com.querydsl.sql.codegen.MetaDataExporter;

/**
 * 测试QueryDSL的代码生成功能
 * @author jiyi
 *
 */
public class Exporter {
    public static void main(String[] args) throws SQLException {
        DataSource ds=new SimpleDataSource("jdbc:derby:./db;create=true", null, null);
        Connection conn = null;
        try {
            conn = ds.getConnection();
            MetaDataExporter exporter = new MetaDataExporter();
            exporter.setPackageName("org.springframework.data.jdbc.query.generated");
            exporter.setTargetFolder(new File("build/generated-sources/java"));
            exporter.export(conn.getMetaData());
        } finally {
            DbUtils.closeConnection(conn);
        }
    }
}
