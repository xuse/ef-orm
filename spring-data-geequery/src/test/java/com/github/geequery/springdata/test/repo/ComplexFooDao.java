package com.github.geequery.springdata.test.repo;

import com.github.geequery.springdata.repository.GqRepository;
import com.github.geequery.springdata.test.entity.ComplexFoo;

public interface ComplexFooDao extends GqRepository<ComplexFoo, int[]>,CustomComplexDao{

}