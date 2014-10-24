package jef.database.meta;

import jef.database.query.Query;

public interface ExtensionConfigFactory {
	ExtensionConfig valueOf(Query<?> q);
	ExtensionConfig valueOf(String value);
}
