package jef.database;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 该对象描述一个lazyload策略，多个对象公用，因此不保存状态
 * @author jiyi
 *
 */
public final class LazyLoadProcessor {
	private WeakReference<Session> session;
	private DbClient parent;
	private final List<LazyLoadTask> tasks=new ArrayList<LazyLoadTask>();
	private final Map<String,Integer> onFields=new HashMap<String,Integer>(8,0.6f);
	
	public LazyLoadProcessor(LazyLoadTask task,Session session) {
		this.session=new WeakReference<Session>(session);
		this.parent=session.getNoTransactionSession();
		this.register(task);
	}
	public LazyLoadProcessor(List<LazyLoadTask> tasks,Session session) {
		this.session=new WeakReference<Session>(session);
		this.parent=session.getNoTransactionSession();
		for(LazyLoadTask t:tasks){
			this.register(t);
		}
	}
	public void doTask(Object obj,int id) throws SQLException{
		Session s = null;
		if(session!=null){
			s=session.get();
			if(s==null){
				session.clear();
				session=null;
			}else if(!s.isOpen()){
				session.clear();
				session=null;
				s=null;
			}
		}
		if(s==null)s=parent;
		tasks.get(id).process(s, obj);
	}
	
	public void register(LazyLoadTask vsManyLoadTask) {
		tasks.add(vsManyLoadTask);
		Integer id=tasks.size()-1;//该任务的序号
		for(String fieldName: vsManyLoadTask.getEffectFields()){
			onFields.put(fieldName,id);	
		}
	}
	public int getTaskId(String field) {
		Integer id=onFields.get(field);
		return id==null?-1:id.intValue();
	}
	public int size() {
		return tasks.size();
	}
	public List<LazyLoadTask> getTasks() {
		return tasks;
	}
	public Map<String,Integer> getOnFields(){
		return onFields;
	}
	@Override
	public String toString() {
		StringBuilder sb=new StringBuilder();
		for(Map.Entry<String,Integer> entry:onFields.entrySet()){
			sb.append('[').append(entry.getKey()).append('=').append(tasks.get(entry.getValue()));
			sb.append(']');
		}
		return sb.toString();
	}
}
