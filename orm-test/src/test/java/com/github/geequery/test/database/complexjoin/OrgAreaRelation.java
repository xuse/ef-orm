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
@Table(name = "uc_org_area_relation")
@Comment("组织区域表")
public class OrgAreaRelation extends DataObject {

    private static final long serialVersionUID = 9109848964514169032L;

    @Id
    @Column(name = "org_id",length = 32)
    @Comment("组织ID")
    private String orgId;

    @Id
    @Column(name = "area_id",length = 32)
    @Comment("区域id")
    private String areaId;

    /**
     * 保留,暂时无用
     */
    @Comment("关系类型")
    @Column(name = "type")
    private String type = "";

    public OrgAreaRelation() {
    }

    public enum Field implements jef.database.Field {
        orgId, areaId, type
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
