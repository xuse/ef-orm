package jef.database.exception;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jef.common.SimpleMap;
import jef.tools.StringUtils;

/**
 * 当多数据库提交时，如果某出现某些库提交成功，某些库提交失败，那么抛出这个异常。
 * @author jiyi
 *
 */
public class InconsistentCommitException extends SQLException{
	private static final long serialVersionUID = 3095533496988117255L;

	private  List<String> succeed;
	
	private SimpleMap<String,SQLException> exceptions;
	
	private int total;

	public InconsistentCommitException(List<String> successed2, SimpleMap<String, SQLException> errors,int total) {
		this.succeed=successed2;
		this.exceptions=errors;
		this.total=total;
	}

	@Override
	public String getMessage() {
		Entry<String, SQLException> error = exceptions.getEntries().get(0);
		String message = StringUtils.concat("Error while commit datasource [", error.getKey(), "], and this is the ", String.valueOf(succeed.size() + 1), "th commit of ", String.valueOf(total),
				", there must be some data consistency problem, please check it.");
		return message;
		
	}
	
	/**
	 * 获得成功提交了的数据源名称
	 * @return 成功提交了的数据源名称
	 */
	public Collection<String> getSucceedCommits(){
		return succeed;
	}
	
	/**
	 * 获得提交失败的数据源名称
	 * @return 提交失败的数据源名称
	 */
	public Collection<String> getFailureCommits(){
		return Collections.unmodifiableCollection(exceptions.keySet());
	}
	
	/**
	 * 得到出现的异常
	 * @return 出现的异常
	 */
	public Collection<SQLException> getExceptions(){
		return Collections.unmodifiableCollection(exceptions.values());
	}
	
	/**
	 * 得到出现提交异常的数据源名称和异常对象。
	 * @return 出现提交异常的数据源名称和异常对象。
	 */
	public Map<String,SQLException> getFailures(){
		return Collections.unmodifiableMap(exceptions);
	}
	
	/**
	 * 得到成功提交的数量
	 * @return 成功提交的数量
	 */
	public int getSucceedCount(){
		return succeed.size();
	}
	
	/**
	 * 得到未成功提交的连接数量
	 * @return 未成功提交的连接数量
	 */
	public int getUncommitCount(){
		return total-succeed.size();
	}
}
