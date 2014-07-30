package jef.database;

import java.sql.SQLException;

/**
 * 在每个需要延迟加载的对象中存放一个。
 * 用于记录已经加载的字段和尚未加载的字段。
 * @author jiyi
 *
 */
final class LazyLoadContext implements ILazyLoadContext {
	//策略
	private LazyLoadProcessor processor;
	//已经load过的数据
	private boolean[] loaded;
	private int executed;
	
	public LazyLoadContext(LazyLoadProcessor processor2) {
		this.processor=processor2;
		executed=0;
	}

	/**
	 * 返回需要执行的加载任务ID（对应LazyLoadProcessor中的Task序号）。
	 * 返回-1表示无任务需要执行
	 */
	public int needLoad(String field){
		int id=processor.getTaskId(field);
		if(id==-1)return id;
		if(loaded==null){
			loaded=new boolean[processor.size()];
		}else{
			if(loaded[id]){
				return -1;
			}	
		}
		return id;
	}

	public boolean process(DataObject dataObject, int id) throws SQLException {
		if(!loaded[id]){
			processor.doTask(dataObject,id);
			loaded[id]=true;
			executed++;
		}
		return executed>=processor.size();
	}

	public LazyLoadProcessor getProcessor() {
		return processor;
	}

	@Override
	public String toString() {
		return processor.toString();
	}
	
	
}
