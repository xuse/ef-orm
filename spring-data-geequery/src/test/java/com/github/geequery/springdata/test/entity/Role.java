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
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import jef.database.DataObject;
import jef.database.annotation.Indexed;

import com.github.geequery.orm.annotation.Comment;

/**
 * 角色表
 * @author shixiafeng
 */
@Entity
@Table(name = "uc_role_info")
@Comment("角色信息表")
public class Role extends DataObject {
    /**
     *
     */
    private static final long serialVersionUID = 1L;


    @Id
    @Column(length = 32)
    @Comment("角色ID")
    private String id;

    @Column(name = "role_code", length = 32)
    @Comment("角色编码")
    private String roleCode;

    @Column(name = "role_name")
    @Comment("角色名称")
    private String roleName;


    @Comment("角色类型")
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private RoleType type;

    public enum RoleType {
        /**
         * 系统角色
         */
        System,
        /**
         * 自定义角色
         */
        Custom
    }

    @Column(name = "authorization")
    @Comment("可授权范围")
    private String authorization;

    @Column(length = 512)
    @Lob
    @Comment("备注")
    private String desc;

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

    @ManyToMany(cascade = CascadeType.REFRESH)
    @JoinTable(name = "uc_account_role_relation", joinColumns =
            {@JoinColumn(name = "role_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "account_name", referencedColumnName = "accountName")})
    private List<Account> accounts;

    public enum Field implements jef.database.Field {
        id, roleCode, roleName, type, desc, authorization,
        //额外属性
        ownerOrg, createBy, created, modifyBy, modified, dataValid
    }

    public Role() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
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
