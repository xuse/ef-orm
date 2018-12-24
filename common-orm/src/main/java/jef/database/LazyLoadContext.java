package jef.database;

import java.sql.SQLException;
import java.util.BitSet;

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
	private BitSet loaded;
	//load过的次数
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
			loaded=new BitSet(processor.size());
		}else{
			if(loaded.get(id)){
				return -1;
			}	
		}
		return id;
	}
	
	@Override
	public void markProcessed(String field) {
		int id=processor.getTaskId(field);
		if(id==-1)return;
		if(loaded==null){
			loaded=new BitSet(processor.size());
		}
		loaded.set(id,true);
	}

	public boolean process(Object dataObject, int id) throws SQLException {
		if(!loaded.get(id)){
			processor.doTask(dataObject,id);
			loaded.set(id,true);
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
