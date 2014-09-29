package jef.database.dynamic;

import java.awt.Container;
import java.awt.GridLayout;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import jef.database.DbUtils;
import jef.database.Field;
import jef.database.dialect.ColumnType;
import jef.database.meta.Column;
import jef.database.meta.ColumnChange;
import jef.database.meta.ColumnModification;
import jef.database.meta.ITableMetadata;
import jef.database.support.MetadataEventListener;
import jef.database.support.executor.StatementExecutor;
import jef.tools.ThreadUtils;

public class ProgressSample implements MetadataEventListener{
	JProgressBar progressBar;
	JFrame theFrame;
	private static final int scale=5;
	
	public static void main(String[] args) {
		ProgressSample t=new ProgressSample();
		t.startBar();
		for(int i=0;i<101;i++){
			ThreadUtils.doSleep(5*scale);
			t.progressBar.setValue(i);
		}
		ThreadUtils.doSleep(10*scale);
		t.theFrame.dispose();
	}

	public void startBar() {
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true); // 显示百分比字符
		progressBar.setIndeterminate(false); // 不确定的进度条
		theFrame= new JFrame("Progress Bars");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container contentPane = theFrame.getContentPane();
		contentPane.setLayout(new GridLayout(1, 1));
		contentPane.add(progressBar);
		theFrame.setLocationByPlatform(true);
		theFrame.setSize(800, 90);
		theFrame.setVisible(true);
	}

	public boolean beforeTableRefresh(ITableMetadata meta, String table) {
		startBar();
		progressBar.setString("正在变更表"+table);
		ThreadUtils.doSleep(30*scale);
		return true;
	}

	public boolean onTableCreate(ITableMetadata meta, String tablename) {
		progressBar.setValue(30);
		progressBar.setString("正在创建表"+tablename);
		ThreadUtils.doSleep(30*scale);
		return true;
	}
	
	public boolean onCompareColumns(String tablename, List<Column> columns, Map<Field, ColumnType> defined) {
		progressBar.setValue(5);
		progressBar.setString("正在比较表"+tablename+"的结构...");
		ThreadUtils.doSleep(30*scale);
		return true;
	}

	public boolean onColumnsCompared(String tablename, ITableMetadata meta, Map<String, ColumnType> insert, List<ColumnModification> changed, List<String> delete) {
		progressBar.setValue(15);
		progressBar.setString("将在表"+tablename+"上新增"+insert.size()+"个列，删除"+delete.size()+"个列，修改"+changed.size()+"个列。");
		System.out.println("将在表"+tablename+"上新增"+insert.size()+"个列，删除"+delete.size()+"个列，修改"+changed.size()+"个列。");
		//详细打印出每个列修改了什么。
		for(ColumnModification modification:changed){
			System.out.print("列"+ modification.getColumnName()+"上修改: ");
			for(ColumnChange change: modification.getChanges()){
				switch (change.getType()) {
				case CHG_DATATYPE:
					System.out.print("数据类型从"+change.getFrom()+" -> "+change.getTo()+" ");
					break;
				case CHG_DEFAULT:
					System.out.print("修改缺省值为"+change.getTo()+" ");
					break;
				case CHG_DROP_DEFAULT:
					System.out.print("取消缺省值 ");
					break;
				case CHG_TO_NOT_NULL:
					System.out.print("不允许为空 ");
					break;
				case CHG_TO_NULL:
					System.out.print("允许为空 ");
					break;
				default:
					break;
				}
			}
			System.out.println();
		}
		ThreadUtils.doSleep(30*scale);
		return true;
	}

	public void beforeAlterTable(String tablename, ITableMetadata meta, StatementExecutor conn, List<String> sql) {
		Statement st=null;
		ResultSet rs=null;
		int count=0;
		try{
			rs=conn.executeQuery("select count(*) from "+tablename);
			if(rs.next()){
				count=rs.getInt(1);
			}
		}catch(SQLException e){
			throw new IllegalArgumentException();
		}finally{
			DbUtils.close(rs);
			DbUtils.close(st);
		}
		progressBar.setValue(20);
		progressBar.setString("将在表上执行"+sql.size()+"条SQL，这将影响表中的"+count+"条记录。");
		ThreadUtils.doSleep(50*scale);
	}


	public void onAlterSqlFinished(String tablename, String sql,  List<String> sqls,int n, long cost) {
		int progress=80/sqls.size();
		progressBar.setValue(progressBar.getValue()+progress);
		progressBar.setString("第"+(n+1)+"条SQL执行完成，耗时"+cost+"毫秒。\n"+sql);
		ThreadUtils.doSleep(50*scale);
	}
	
	public void onTableFinished(ITableMetadata meta, String tablename) {
		ThreadUtils.doSleep(15*scale);
		progressBar.setValue(100);
		progressBar.setString("表"+tablename+"变更完成。");
		ThreadUtils.doSleep(50*scale);
		theFrame.dispose();
	}

	public boolean onSqlExecuteError(SQLException e, String tablename, String sql, List<String> sqls, int n) {
		ThreadUtils.doSleep(15*scale);
		progressBar.setValue(100);
		progressBar.setString("表"+tablename+"变更失败！"+sql);
		ThreadUtils.doSleep(50*scale);
		theFrame.dispose();
		return false;
	}
}