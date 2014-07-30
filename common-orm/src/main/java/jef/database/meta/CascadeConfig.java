package jef.database.meta;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;

import jef.database.annotation.Cascade;
import jef.database.query.ReferenceType;

public class CascadeConfig {
	 Cascade asMap;
	 JoinPath path;
	 FetchType fetch = FetchType.EAGER;
	 CascadeType[] cascade;
	 ReferenceType refType;
}
