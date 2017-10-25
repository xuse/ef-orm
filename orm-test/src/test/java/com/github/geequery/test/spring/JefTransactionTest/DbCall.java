package com.github.geequery.test.spring.JefTransactionTest;

import jef.database.jpa.JefEntityManager;

public interface DbCall {
	void call(JefEntityManager em);
}
