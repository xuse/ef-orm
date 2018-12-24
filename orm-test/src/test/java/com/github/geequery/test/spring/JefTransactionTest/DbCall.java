package com.github.geequery.test.spring.JefTransactionTest;

import jef.database.Session;

public interface DbCall {
	void call(Session em);
}
