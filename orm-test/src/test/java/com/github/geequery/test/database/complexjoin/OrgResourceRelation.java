package com.github.geequery.test.database.complexjoin;

import com.github.geequery.orm.annotation.Comment;
import jef.database.DataObject;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/*
*@auther shixiafeng  
*@create 2017-10-25
*/
@Entity
@Table(name = "uc_org_resource_relation")
@Comment("组织资源关系表")
public class OrgResourceRelation extends DataObject {

    private static final long serialVersionUID = -783843068093830617L;

    @Id
    @Column(name = "org_id", length = 32)
    @Comment("组织ID")
    private String orgId;

    @Id
    @Column(name = "resource_id", length = 32)
    @Comment("资源id")
    private String resourceId;

    /**
     * 保留,暂时无用
     */
    @Comment("关系类型")
    @Column(name = "type")
    private String type = "";

    public enum Field implements jef.database.Field {
        orgId, resourceId, type
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
