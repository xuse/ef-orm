package jef.database.pooltest;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import jef.database.DbClient;
import jef.database.DbUtils;
import jef.database.Transaction;

public class DbPoolTest {
    
    static ConcurrentHashMap<String,String> set = new ConcurrentHashMap<String,String>();
    
    static final String obj = new String();
    
    static PrintStream out;
    
    public static void main(String[] args) throws Exception {
        
        try{
            File file = new File("d:\\temp\\pool.time");
            if(file.exists()){
                boolean isd = file.delete();
                System.out.println(isd);
            }
            out = new PrintStream(file){
//                @Override
//                public void println(String s) {
//                }
            };
        }catch(Exception e){
            out = System.out;
        }
        
        final boolean useDbClient = true;
        
        final DataSource dataSource;
        final DbClient client;//getDbClient();
        
        if (useDbClient) {
            client = getDbClient();
          //  client.executeSql("create table Y2K(X varchar(8))");
            dataSource = null;
        }else{
//            dataSource  = getDataSource();
            client = null;
        }
        
        for (int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        if (useDbClient) {
                            selectY2KCount(client);
                        } else {
                            selectY2KCount(dataSource);
                        }
                    }
                }
            }).start();
        }
    }

    private static void selectY2KCount(DbClient client){
        long t = System.currentTimeMillis();
        selectY2K(client);
        long t2 = System.currentTimeMillis();
        out.println(Thread.currentThread().getName() + " : " +(t2-t) + " : " + set.size());
    }
    
    private static void selectY2KCount(DataSource dataSource){
        long t = System.currentTimeMillis();
        selectY2K(dataSource);
        long t2 = System.currentTimeMillis();
        out.println(Thread.currentThread().getName() + " : " +(t2-t) + " : " + set.size());
    }
    
    private static void selectY2K(DataSource dataSource) {
        Connection c = null;
        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            Field f = c.getClass().getDeclaredField("inner");
            f.setAccessible(true);
            Object obj = f.get(c);

            set.put(obj.toString(), DbPoolTest.obj);

            Random random = new Random();
            int r = random.nextInt(350);

            PreparedStatement pst = c.prepareStatement("select * from y2k");
           
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                // out.printf("%s,%s,%s \n", rs.getObject(1),
                // rs.getObject(2),rs.getObject(3));
            }

            // Thread.sleep(700-r);
            if (set.size() > 1000) {
                set.clear();
            }

            pst = c.prepareStatement("select * from y2k");
            while (rs.next()) {
                // out.printf("%s,%s,%s \n", rs.getObject(1),
                // rs.getObject(2),rs.getObject(3));
            }
            
            boolean ret = pst.execute();
            c.commit();
        } catch (Exception e) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
        } finally {
            try {
                if (c != null)
                    c.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 
     */
//    public static DataSource getDataSource() throws Exception {
//        ComboPooledDataSource dataSource = new ComboPooledDataSource(true);
//        dataSource.setDriverClass("com.mysql.jdbc.Driver");
//        dataSource.setJdbcUrl("jdbc:mysql://localhost:3307/test");
//        dataSource.setUser("root");
//        dataSource.setPassword("");
//        dataSource.setInitialPoolSize(1);
//        dataSource.setMinPoolSize(5);
//        dataSource.setMaxPoolSize(100);
//        dataSource.setMaxIdleTime(25000);
//        dataSource.setAcquireIncrement(1);
//        dataSource.setAcquireRetryAttempts(30);
//        dataSource.setAcquireRetryDelay(1000);
//        dataSource.setTestConnectionOnCheckin(true);
//        // dataSource.setAutomaticTestTable(null);
//        dataSource.setIdleConnectionTestPeriod(18000);
//        dataSource.setCheckoutTimeout(800 * 20);
//        return dataSource;
//    }

    /**
     * 
     */
    public static DbClient getDbClient() throws Exception {
        DbClient dbClient = new DbClient(DbUtils.createSimpleDataSource("jdbc:derby:./db;create=true", "root", "root"));
        return dbClient;
    }
    
    private static void selectY2K(DbClient dbClient) {
        Transaction tra =null;
        try {
            tra = dbClient.startTransaction();
            Random random = new Random();
            int r = random.nextInt(350);

            tra.getResultSet("select * from y2k",0);
            // Thread.sleep(100);
            if (set.size() > 1000) {
                set.clear();
            }

            tra.getResultSet("select * from y2k",0);

            // tra.commitAndClose();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                tra.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            if (tra != null) {
                try {
                    tra.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
}
