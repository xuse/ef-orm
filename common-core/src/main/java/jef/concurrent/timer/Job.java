/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.concurrent.timer;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

import jef.common.DateSpan;
import jef.common.log.LogUtil;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

/**
 * 由于Trigger方法中涉及重新安排任务的时间，造成需要存取TimerTask的state属性，
 * 为此不得不将JDK的Timer和TimerTask抄了一份到这里来。
 * @author jiyi
 */
public abstract class Job extends TimerTask implements Serializable {
	
	/**
	 * 描述一次执行的情况
	 * @author Administrator
	 *
	 */
	public static class ExecuteInfo implements Serializable{
		private static final long serialVersionUID = -346868877252111768L;
		private boolean force;
		private DateSpan firetime;
		private String exception;
		public ExecuteInfo(DateSpan loadObject) {
			this.firetime=loadObject;
		}
		/**
		 * 获得上次运行的执行时间
		 * @return
		 */
		public DateSpan getFiretime() {
			return firetime;
		}
		
		public String getException() {
			return exception;
		}
		public boolean isForce() {
			return force;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7587521178032575229L;
	protected ExecuteInfo lastFire;//上次执行记录
	/**
	 * 如果是调用trigger方法强制执行的，此标志会设置为true;
	 */
	protected boolean isForceFire=false;
	protected Date currentStartTime;
	private String name;
	protected Timer timer;
	private JobState state=JobState.NOT_SCHEDULED;
	/**
	 * 延迟时间（第一次运行前）单位ms
	 * @return
	 */
	protected abstract long getDelay();
	/**
	 * 得到间隔时间,单位ms
	 * 如果返回0，表示此任务只运行一次。
	 */
	protected abstract long getPeriod();
	/**
	 * 执行的内容
	 */
	protected abstract void execute();
	/**
	 * 返回名称
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * 设置名称
	 * @param name
	 */
	public void setName(String name) {
		Assert.equals(JobState.NOT_SCHEDULED, state, "The trigger's cann't be renamed after SCHEDULED.");
		this.name = name;
		initTime(name);
	}
	
	public enum JobState{
		NOT_SCHEDULED,//未安排，未运行
		RUNNING,//正在运行
		SCHEDULED,//已安排
	}
	
	//季怡关于Java的Timer 和 TimerTask的代码阅读要点
	//1 一个Timer中可以安排多个Task，但每个Timer是一个线程运行，所以一个Task长时间运行会堵塞整个Timer，造成后续任务无法运行。
	//因此设计还是让每个Job一个Timer(拥有独立的计时线程)
	//2 Timertask中的scheduledExecutionTime可以获得上次运行时间（开始时间而非任务结束时间,实际上是根据已经安排的下次时间倒推计算的）
	//3 timerTask的Cancel方法可以取消任务或者安排。（返回True即取消有效，否则返回False）
	//4 要想重新安排任务，必须先Cancel这一个任务。
	//5 scheduleAtFixedRate和普通schedule区别在于其会保证在一定时间内的运行次数。但是还在Timer的同一个单线程中运行，
	//一旦进度延后其会尝试连续运行多次来追赶安排的进度。(个人感觉意义不大)
	
	private final void initTime(String name) {
		if(stateSaved()){
			String fileName=this.getClass().getSimpleName()+"_"+name+".last";
			File lastStatus=new File(fileName);
			if(lastStatus.exists()){
				Object obj=IOUtils.loadObject(lastStatus);
				if(obj instanceof DateSpan){
					lastFire=new ExecuteInfo((DateSpan)obj);	
				}else if(obj instanceof ExecuteInfo){
					lastFire=(ExecuteInfo)obj;
				}
			}	
		}
	}
	/**
	 * 表示此类是否具有时间持久化特性。
	 * @return
	 */
	protected boolean stateSaved() {
		return true;
	}
	/**
	 * 计时器开始运作
	 */
	public final void startTimer(){
		if(timer==null){
			timer=new Timer(true);
			timer.start();
			long d=getDelay();
			if(lastFire!=null){//如果上次运行时间能够获得
				long wait=getPeriod()-(System.currentTimeMillis()-lastFire.getFiretime().getStart().getTime());
				if(wait>d)d=wait;
			}
			timer.schedule(this, d,getPeriod());
			state=JobState.SCHEDULED;
		}
	}
	
	/**
	 * 停止计时器,包括计时器线程等所有内容
	 */
	public final void stopTimer(){
		if(timer!=null){
			this.cancel();
			timer.cancel();
			timer=null;
			state=JobState.NOT_SCHEDULED;	
		}
	}
	/**
	 * 如果当前Job正在运行，得到Job的开始时间。否则返回null
	 * @return
	 */
	public Date getCurrentStartTime() {
		return currentStartTime;
	}
	/**
	 * 获得状态
	 */
	public JobState getState(){
		return state;
	}
	
	/**
	 * 立即触发任务，相应的下次运行时间会重新调整
	 */
	public void trigger(){
		if(timer==null){
			LogUtil.show(this.getName()+" no timer, will execute in current thread directly.");
			isForceFire=true;
			this.run();
			isForceFire=false;
		}else{
			this.cancel();
			timer.purge();//移除该任务
			synchronized(lock) {//强行回复状态（修补XX膜？）
				super.state=TimerTask.VIRGIN;
			}
			isForceFire=true;
			timer.schedule(this, 200, getPeriod());
			isForceFire=false;
		}
	}
	
	/**
	 * 在计时器线程中被调用，当有多个线程调用此方法时会被阻塞
	 */
	public synchronized void run() {
		state=JobState.RUNNING;
		currentStartTime=new Date();
		String exception=null;
		try{
			execute();	
		}catch(Throwable t){
			exception = StringUtils.exceptionSummary(t);
		}
		//将上次运行时间保存到本地
		if(stateSaved()){
			String fileName=this.getClass().getSimpleName()+"_"+getName()+".last";
			File statusFile=new File(fileName);
			lastFire=new ExecuteInfo(new DateSpan(currentStartTime,new Date()));
			lastFire.force=isForceFire;
			lastFire.exception=exception;
			IOUtils.saveObject(lastFire, statusFile);//每次运行结束时，将时间持久化到磁盘上，下次构造时自动读取
		}
		currentStartTime=null;
		state=JobState.SCHEDULED;
	}
	
	
	/**
	 * 得到上次运行的时间段，没有运行结束过返回null
	 * @return
	 */
	public DateSpan getLastFire() {
		return lastFire.getFiretime();
	}
	
	/**
	 * 得到上次运行的详细信息
	 * @return
	 */
	public ExecuteInfo getLastFireInfo() {
		return lastFire;
	}
}
