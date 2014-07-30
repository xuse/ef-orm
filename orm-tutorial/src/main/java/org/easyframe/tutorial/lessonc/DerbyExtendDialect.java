package org.easyframe.tutorial.lessonc;

import jef.database.dialect.DerbyDialect;
import jef.database.query.function.StandardSQLFunction;
import jef.database.query.function.TemplateFunction;

public class DerbyExtendDialect extends DerbyDialect{
	public DerbyExtendDialect() {
		super();
		registerNative(new StandardSQLFunction("atan2"));
		registerCompatible(null, new TemplateFunction("ifnull", "(CASE WHEN %1$s is null THEN %2$s ELSE %1$s END)"),"ifnull");
		
//		registerAlias("ifnull", "coalesce");

	}
}
