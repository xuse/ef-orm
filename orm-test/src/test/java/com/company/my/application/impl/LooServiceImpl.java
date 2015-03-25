package com.company.my.application.impl;

import org.springframework.stereotype.Service;

import com.company.my.application.LooService;

@Service
public class LooServiceImpl implements LooService{

	@Override
	public String test() {
		return "aaaa";
	}

}
