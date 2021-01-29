package jef.database;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import jef.codegen.support.NotModified;
import jef.database.DataObject;

/**
 * @author luhao5
 * @desc 备份文件
 */
@NotModified
@Entity
@Table(name = "backup_doctest1")
public class BackupDocEntity extends DataObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9158878542933164245L;

	/**
	 * 备份文件id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "i_doc_id", nullable = false, precision = 10, columnDefinition = "number(10)")
	private int docId;

	/**
	 * 备份文件名称
	 */
	@Column(name = "c_doc_name", nullable = false, length = 255, columnDefinition = "varchar(255)")
	private String docName;

	/**
	 * 备份文件大小
	 */
	@Column(name = "i_doc_size", precision = 10, columnDefinition = "number(10)")
	private Integer docSize;

	/**
	 * 备份路径
	 */
	@Column(name = "c_path", nullable = false, length = 255, columnDefinition = "varchar(255)")
	private String path;

	/**
	 * 备份开始时间
	 */
	@Column(name = "d_begin_time", nullable = false, columnDefinition = "timestamp")
	private Date beginTime;

	/**
	 * 备份完成时间
	 */
	@Column(name = "d_finish_time", columnDefinition = "timestamp")
	private Date finishTime;

	/**
	 * 备份文件类型，0-全局备份文件，1-组件备份文件
	 */
	@Column(name = "i_type", nullable = false, precision = 5, columnDefinition = "number(5)")
	private int type;
	
	/**
	 * 备份方式，0-自动备份，1-手动备份
	 */
	@Column(name = "i_way", nullable = false, precision = 5, columnDefinition = "number(5)")
	private int backupWay;
	
	/**
	 * 备份规则id
	 */
	@Column(name = "i_rule_id", precision = 10, columnDefinition = "number(10)")
	private Integer ruleId;
	
	/**
	 * 备份组件所在机器
	 */
	@Column(name = "c_agent_no", length = 255, columnDefinition = "varchar(255)")
	private String agentNo;

	/**
	 * 组件ID
	 */
	@Column(name = "c_component_id", length = 255, columnDefinition = "varchar(255)")
	private String componentId;

//	@OneToOne(targetEntity = ComponentEntity.class)
//	@JoinColumn(name = "componentId", referencedColumnName = "componentId")
//	private ComponentEntity componentEntity;
	
	/**
	 * 备份进度
	 */
	@Column(name = "i_progress_num", precision = 5, columnDefinition = "number(5)")
	private int progressNum;
	
	/**
	 * 备份执行结果，记录失败、异常详情
	 */
	@Column(name = "c_action_result_detail", length = 255, columnDefinition = "varchar(255)")
	private String actionResultDetail;
	
	/**
	 * 是否异常:0-无异常;-1-存在异常
	 */
	@Column(name = "i_exception_status", nullable = false, precision = 5, columnDefinition = "number(5)")
	private int exceptionStatus;
	
	/**
	 * 备份任务是否删除:0-不删除;-1-删除
	 */
	@Column(name = "i_task_status", nullable = false, precision = 5, columnDefinition = "number(5)")
	private int taskStatus;
	
	/**
	 * 备份作业状态：2-成功;1-执行中；0-等待执行；-1-失败
	 */
	@Column(name = "i_status", nullable = false, precision = 5, columnDefinition = "number(5)")
	private int status;
	
	/**
	 * 备份文件保留状态: 0-保留;-1-不保留，自动备份时会清理掉
	 */
	@Column(name = "i_retain_status", nullable = false, precision = 5, columnDefinition = "number(5)")
	private int retainStatus = -1;

//	private User authUser = null;

	public int getDocId() {
		return docId;
	}

	public void setDocId(int docId) {
		this.docId = docId;
	}

	public String getDocName() {
		return docName;
	}

	public void setDocName(String docName) {
		this.docName = docName;
	}

	public Integer getDocSize() {
		return docSize;
	}

	public void setDocSize(Integer docSize) {
		this.docSize = docSize;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Date getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}

	public Date getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
	public int getBackupWay() {
		return backupWay;
	}

	public void setBackupWay(int backupWay) {
		this.backupWay = backupWay;
	}
	
	public Integer getRuleId() {
		return ruleId;
	}

	public void setRuleId(Integer ruleId) {
		this.ruleId = ruleId;
	}
	
	public String getAgentNo() {
		return agentNo;
	}

	public void setAgentNo(String agentNo) {
		this.agentNo = agentNo;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public int getProgressNum() {
		return progressNum;
	}

	public void setProgressNum(int progressNum) {
		this.progressNum = progressNum;
	}
	
	public String getActionResultDetail() {
		return actionResultDetail;
	}

	public void setActionResultDetail(String actionResultDetail) {
		this.actionResultDetail = actionResultDetail;
	}
	
	public int getExceptionStatus() {
		return exceptionStatus;
	}

	public void setExceptionStatus(int exceptionStatus) {
		this.exceptionStatus = exceptionStatus;
	}
	
	public int getTaskStatus() {
		return taskStatus;
	}

	public void setTaskStatus(int taskStatus) {
		this.taskStatus = taskStatus;
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	public int getRetainStatus() {
		return retainStatus;
	}

	public void setRetainStatus(int retainStatus) {
		this.retainStatus = retainStatus;
	}
	
	public enum Field implements jef.database.Field {
		docId, docName, docSize, path, beginTime, finishTime, type, backupWay, ruleId, agentNo, componentId, progressNum, actionResultDetail, exceptionStatus, taskStatus, status, retainStatus
	}
	
}