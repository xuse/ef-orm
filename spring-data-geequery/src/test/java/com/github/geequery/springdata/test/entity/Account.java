package com.github.geequery.springdata.test.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import jef.database.DataObject;
import jef.database.annotation.Indexed;

import com.github.geequery.orm.annotation.Comment;

@Table(name = "uc_account_info")
@Entity
@Comment("账号信息表")
public class Account extends DataObject {

    private static final long serialVersionUID = -491389720180169475L;

    @Id
    @Comment("账号名")
    @Column(name = "account_name", length = 32)
    private String accountName;

    @Comment("密码")
    @Column
    private String password;

    @Comment("2:账户锁定,1:账户启用")
    @Column(name = "status")
    @Enumerated(EnumType.ORDINAL)
    private AccountStatus status;

    @Comment("true:需要强制修改密码,false:不需要强制修改密码")
    @Column(name = "modify_pwd")
    private boolean modifyPwd;

    @Column(name = "org_id", length = 32)
    @Comment("组织id")
    private String orgId;

    @Comment("使用人(对应用户表)")
    @Column(name = "user_id", length = 32)
    @Indexed
    private String userId;

    //公共字段
    @Comment("创建人员")
    @Column(name = "create_by", length = 32)
    private String createBy;

    @Comment("创建时间")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Comment("修改人员")
    @Column(name = "modify_by", length = 32)
    private String modifyBy;

    @Comment("修改时间")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modified;

    @Comment("所属组织")
    @Indexed
    @Column(name = "owner_org", length = 32)
    private String ownerOrg;

    @Comment("true：有效 false：无效（逻辑删除）;默认为有效")
    @Indexed
    @Column(name = "data_valid", columnDefinition = " boolean DEFAULT true ")
    private Boolean dataValid = Boolean.TRUE;

    @Comment("片区")
    @Column
    private String division;

    @Comment("头像")
    @Column
    @Lob
    private byte[] icon;

    @ManyToMany
    @JoinTable(name = "uc_account_role_relation", joinColumns = {
            @JoinColumn(name = "account_name", referencedColumnName = "accountName")}, inverseJoinColumns = {
            @JoinColumn(name = "role_id", referencedColumnName = "id")})
    private List<Role> roles;

    @OneToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "createBy", referencedColumnName = "accountName")
    private Account createUser;

    public enum Field implements jef.database.Field {
        accountName, password, status, orgId, userId, division, icon, modifyPwd,
        //额外属性
        ownerOrg, createBy, created, modifyBy, modified, dataValid
    }

    public boolean isModifyPwd() {
        return modifyPwd;
    }

    public void setModifyPwd(boolean modifyPwd) {
        this.modifyPwd = modifyPwd;
    }

    public Account getCreateUser() {
        return createUser;
    }

    public void setCreateUser(Account createUser) {
        this.createUser = createUser;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }


    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getModifyBy() {
        return modifyBy;
    }

    public void setModifyBy(String modifyBy) {
        this.modifyBy = modifyBy;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getOwnerOrg() {
        return ownerOrg;
    }

    public void setOwnerOrg(String ownerOrg) {
        this.ownerOrg = ownerOrg;
    }

    public Boolean getDataValid() {
        return dataValid;
    }

    public void setDataValid(Boolean dataValid) {
        this.dataValid = dataValid;
    }
}
