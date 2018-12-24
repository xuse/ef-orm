package com.github.geequery.spring.support;

import jef.database.datasource.PasswordDecryptor;

public class MyPasswordDecryptor implements PasswordDecryptor{
	public String decrypt(String raw) {
		System.out.println("Input password :" +raw+" return password 'admin'");
		return "admin";
	}

}
