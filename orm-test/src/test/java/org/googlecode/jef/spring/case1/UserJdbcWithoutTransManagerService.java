package org.googlecode.jef.spring.case1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service("service1")
public class UserJdbcWithoutTransManagerService {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void addScore(String userName, int toAdd) {
		String sql = "UPDATE t_user u SET u.score=u.score+? WHERE user= ?";
		jdbcTemplate.update(sql, toAdd, userName);
	}
}