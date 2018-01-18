package jef;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jef.common.log.LogUtil;
import jef.database.QB;
import jef.database.query.Query;
import jef.orm.onetable.model.TestEntity;
import jef.tools.DateUtils;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class MyCatTest {
	
	@Test
	public void fdfd(){
		Query<TestEntity> query = QB.create(TestEntity.class);
		java.util.Date d = DateUtils.getDate(2012, 5, 1);
		query.addCondition(QB.between(TestEntity.Field.dateField, DateUtils.dayBegin(d), DateUtils.dayEnd(d)));
		JSONObject o=(JSONObject) JSON.toJSON(query.getInstance());
		System.out.println(o);
	}

    /**
     * @throws Exception
     */
    @Test
    public void myCatTest() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:8066/TESTDB", "root", "123456");
        conn.setAutoCommit(true);
        conn.setAutoCommit(false);
        System.out.println(conn.getMetaData().getDatabaseProductName());

        executeQueryPrint(conn, "select * from wp_users", null);

        Integer max=executeQueryGet(conn,"select max(ID) from wp_users",null, Integer.class);
        System.out.println("最大序号:"+max);
        executeUpdate(
                conn,
                "insert into wp_users (ID,login_name,password,nicename,email,url,registered,activation_key,status,display_name,spam,deleted) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                Arrays.<Object> asList(max+1, "JIYI", "pass", "nick", "jiyi@gmail.com", "", DateUtils.getSqlDate(2017, 1, 1), "", 1, "ddd", 0, 0));
        
//        executeUpdate(
//                conn,
//                "update wp_users set password = ? where id = ?;"
//                + "update wp_users set password = ? where id = ?;"
//                + "update wp_users set password = ? where id = ?;"
//                + "update wp_users set password = ? where id = ?;"
//                + "update wp_users set password = ? where id = ?;",
//                Arrays.<Object> asList("p1",3,"p2",6,"p3",9,"p4",6,"p5",12));
        executeBatch(
                conn,
                "update wp_users set password = ? where id = ?;",5,         
                Arrays.<Object> asList("p1",3,"p2",6,"p3",9,"p4",6,"p5",12));
        
//        executeUpdate(
//                conn,
//                "update wp_users set password = ? where id = ?;",
//                Arrays.<Object> asList("p1",3));
//        executeUpdate(
//                conn,
//                "update wp_users set password = ? where id = ?;",
//                Arrays.<Object> asList("p2",6));
//        executeUpdate(
//                conn,
//                "update wp_users set password = ? where id = ?;",
//                Arrays.<Object> asList("p3",9));
//        executeUpdate(
//                conn,
//                "update wp_users set password = ? where id = ?;",
//                Arrays.<Object> asList("p4",15));
//        executeUpdate(
//                conn,
//                "update wp_users set password = ? where id = ?;",
//                Arrays.<Object> asList("p5",12));
        conn.commit();
        conn.setAutoCommit(true);
        conn.setAutoCommit(false);
        
        conn.setAutoCommit(true);
        conn.close();
        
        System.out.println("finish");

    }

    private void executeBatch(Connection conn, String string, int time, List<Object> args) throws SQLException {
        PreparedStatement st = conn.prepareStatement(string);
        int index=0;
        int eachArgNum=args.size()/time;
        if(eachArgNum * time != args.size()){
            throw new RuntimeException("参数个数不对！");
        }
        for(int i=0;i<time ;i++){
            for(int j=0;j<eachArgNum; j++){
                st.setObject(j + 1, args.get(index++));
            }
            st.addBatch();
        }
        int[] rs = st.executeBatch();
        LogUtil.show("batch result:" + Arrays.toString(rs));
    }

    private int executeUpdate(Connection conn, String string, List<Object> args) throws SQLException {
        PreparedStatement st = conn.prepareStatement(string);
        try {
            if (args != null && !args.isEmpty()) {
                for (int i = 0; i < args.size(); i++) {
                    st.setObject(i + 1, args.get(i));
                }
            }
            int rs = st.executeUpdate();
            LogUtil.show("result:" + rs);
            return rs;
        } finally {
            st.close();
        }
    }

    private void executeQueryPrint(Connection conn, String string, List<Object> args) throws SQLException {
        PreparedStatement st = conn.prepareStatement(string);
        try {
            if (args != null && !args.isEmpty()) {
                for (int i = 0; i < args.size(); i++) {
                    st.setObject(i + 1, args.get(i));
                }
            }
            ResultSet rs = st.executeQuery();
            try {
                LogUtil.show(rs);
            } finally {
                rs.close();
            }
        } finally {
            st.close();
        }
    }
    
    
    private <T> T executeQueryGet(Connection conn, String string, List<Object> args,Class<T> getType) throws SQLException {
        PreparedStatement st = conn.prepareStatement(string);
        try {
            if (args != null && !args.isEmpty()) {
                for (int i = 0; i < args.size(); i++) {
                    st.setObject(i + 1, args.get(i));
                }
            }
            ResultSet rs = st.executeQuery();
            try {
                if(!rs.next()){
                    return null;
                }
                return rs.getObject(1, getType);
            } finally {
                rs.close();
            }
        } finally {
            st.close();
        }
    }

}
