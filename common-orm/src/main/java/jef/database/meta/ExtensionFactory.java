package jef.database.meta;

import jef.database.query.Query;

public interface ExtensionFactory {
	ExtensionConfig valueOf(Query<?> q);
}
