package jef.database.meta;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import jef.database.annotation.Cascade;
import jef.database.query.ReferenceType;

public class CascadeConfig {
	static final CascadeType[] ALL = new CascadeType[] { CascadeType.ALL };
	
	private Cascade asMap;
	private FetchType fetch = FetchType.EAGER;
	private CascadeType[] cascade = ALL;
	private ReferenceType refType;
	
	JoinPath path;

	public CascadeConfig(Cascade cascade, OneToOne ref) {
		asMap = cascade;
		this.refType=ReferenceType.ONE_TO_ONE;
		if (ref != null) {
			if(ref.cascade().length>0)
				this.cascade = ref.cascade();
			this.fetch = ref.fetch();
		}
	}

	public CascadeConfig(Cascade cascade, OneToMany ref) {
		asMap = cascade;
		this.refType=ReferenceType.ONE_TO_MANY;
		if (ref != null) {
			if(ref.cascade().length>0)
				this.cascade = ref.cascade();// 由于EF-ORM中的级联操作都是显式操作，因此当不指定时可以默认用ALL计算
			this.fetch = ref.fetch();
		}else{
			fetch = FetchType.LAZY;
		}
	}

	public CascadeConfig(Cascade cascade, ManyToOne ref) {
		asMap = cascade;
		this.refType=ReferenceType.MANY_TO_ONE;
		if (ref != null) {
			if(ref.cascade().length>0)
				this.cascade = ref.cascade();
			this.fetch = ref.fetch();
		}
	}

	public CascadeConfig(Cascade cascade, ManyToMany ref) {
		asMap = cascade;
		this.refType=ReferenceType.MANY_TO_MANY;
		if (ref != null) {
			if(ref.cascade().length>0)
				this.cascade = ref.cascade();
			this.fetch = ref.fetch();
		}else{
			fetch = FetchType.LAZY;
		}
	}

	public Cascade getAsMap() {
		return asMap;
	}

	public FetchType getFetch() {
		return fetch;
	}

	public CascadeType[] getCascade() {
		return cascade;
	}

	public ReferenceType getRefType() {
		return refType;
	}
}
