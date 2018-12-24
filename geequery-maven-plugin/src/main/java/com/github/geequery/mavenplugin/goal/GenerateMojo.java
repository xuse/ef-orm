/*
 * Copyright 2015, The Querydsl Team (http://www.querydsl.com/team)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.geequery.mavenplugin.goal;

import java.io.File;
import java.sql.SQLException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.github.geequery.codegen.EntityGenerator;
import com.github.geequery.codegen.MetaProvider.DbClientProvider;
import com.github.geequery.codegen.Option;
import com.querydsl.sql.codegen.support.NumericMapping;
import com.querydsl.sql.codegen.support.RenameMapping;
import com.querydsl.sql.codegen.support.TypeMapping;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbUtils;
import jef.tools.ArrayUtils;
import jef.tools.StringUtils;

/**
 * @goal generate
 * @phase generate-sources
 */

public class GenerateMojo extends AbstractMojo {
	/**
	 * JDBC driver class name
	 * 
	 * @parameter required=true
	 */
	private String jdbcDriver;

	/**
	 * JDBC connection url
	 * 
	 * @parameter required=true
	 */
	private String jdbcUrl;

	/**
	 * JDBC connection username
	 * 
	 * @parameter
	 */
	private String jdbcUser;

	/**
	 * JDBC connection password
	 * 
	 * @parameter
	 */
	private String jdbcPassword;

	/**
	 * name suffix for bean types (default: "")
	 * 
	 * @parameter default-value="Repository"
	 */
	private String repositorySuffix;

	/**
	 * package name for sources
	 * 
	 * @parameter
	 * @required
	 */
	private String packageName;

	/**
	 * tableNamePattern a table name pattern; must match the table name as it is
	 * stored in the database (default: null)
	 *
	 * @parameter
	 */
	private String tableNamePattern;

	/**
	 * target source folder to create the sources into (e.g.
	 * target/generated-sources/java)
	 *
	 * @parameter
	 * @required
	 */
	private String targetFolder;
	
	/**
	 * @parameter default-value="src/test/java" required=true
	 */
	private String testFolder;

	/**
	 * 生成仓库
	 *
	 * @parameter default-value=false
	 */
	private boolean exportRepos;
	
	/**
	 * 
	 * 生成仓库的单元测试
	 * @parameter default-value=false
	 */
	private boolean exportRepoTests;
	
	
	/**
	 * serialize beans as well
	 *
	 * @parameter default-value=false
	 */
	private boolean initializeData;
	

	/**
	 * Repository Package name
	 * 
	 * @parameter
	 */
	private String repositoryPackage;

	/**
	 * export column annotations (default: false)
	 *
	 * @parameter default-value=false
	 */
	private boolean commentAnnotations;

	/**
	 * custom type classnames to use
	 *
	 * @parameter
	 */
	private String[] customTypes;

	/**
	 * custom type mappings to use
	 *
	 * @parameter
	 */
	private TypeMapping[] typeMappings;

	/**
	 * custom numeric mappings
	 *
	 * @parameter
	 */
	private NumericMapping[] numericMappings;

	/**
	 * custom rename mappings
	 *
	 * @parameter
	 */
	private RenameMapping[] renameMappings;

	/**
	 * switch to export Indexes
	 *
	 * @parameter default-value=true
	 */
	private boolean exportIndexes;

	/**
	 * Whether to skip the exporting execution
	 *
	 * @parameter default-value=false property="maven.querydsl.skip"
	 */
	private boolean skip;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			return;
		}
		// <jdbcDriver>org.apache.derby.jdbc.EmbeddedDriver</jdbcDriver>
		// <jdbcUrl>jdbc:derby:target/demoDB;create=true</jdbcUrl>
		// <packageName>com.myproject.domain</packageName>
		// <targetFolder>${project.basedir}/target/generated-sources/java</targetFolder>
		EntityGenerator g = new EntityGenerator();
		DbClient db;
		db = new DbClientBuilder(this.jdbcUrl, this.jdbcUser, this.jdbcPassword).setEnhanceScanPackages(false).build();
		g.setProvider(new DbClientProvider(db));
		g.setProfile(db.getProfile(null));
		if (StringUtils.isNotEmpty(tableNamePattern)) {
			g.setIncludePattern(tableNamePattern);
		} else {
			g.addExcludePatter(".*_\\d+$"); // 防止出现分表
			g.addExcludePatter("AAA"); // 排除表
		}
		g.setInitializeDataAll(initializeData);
		g.setMaxTables(999);
		g.setSrcFolder(new File(targetFolder));
		g.setBasePackage(this.packageName);
		Option[] options = new Option[] { Option.generateEntity };
		if (!commentAnnotations) {
			options = ArrayUtils.addElement(options, Option.ignoreCommentAnnotation);
		}
		try {
			if (exportRepos) {
				if (StringUtils.isNotEmpty(this.repositoryPackage)) {
					g.setReposPackageName(repositoryPackage);
				}
				if (StringUtils.isNotEmpty(this.repositorySuffix)) {
					g.setReposSuffix(repositorySuffix);
				}
				options = ArrayUtils.addElement(options, Option.generateRepos);
				if(exportRepoTests) {
					options = ArrayUtils.addElement(options, Option.generateRepoTestCase);
					g.setTestFolder(new File(testFolder));
				}
			}
			g.generateSchema(options);
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		} finally {
			db.shutdown();
		}
	}

	protected boolean isForTest() {
		return false;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public void setJdbcUser(String jdbcUser) {
		this.jdbcUser = jdbcUser;
	}

	public void setJdbcPassword(String jdbcPassword) {
		this.jdbcPassword = jdbcPassword;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setTableNamePattern(String tableNamePattern) {
		this.tableNamePattern = tableNamePattern;
	}

	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}

	public void setCustomTypes(String[] customTypes) {
		this.customTypes = customTypes;
	}

	public void setTypeMappings(TypeMapping[] typeMappings) {
		this.typeMappings = typeMappings;
	}

	public void setNumericMappings(NumericMapping[] numericMappings) {
		this.numericMappings = numericMappings;
	}

	public void setRenameMappings(RenameMapping[] renameMappings) {
		this.renameMappings = renameMappings;
	}

	public void setRepositorySuffix(String repositorySuffix) {
		this.repositorySuffix = repositorySuffix;
	}

	public void setExportRepos(boolean exportRepos) {
		this.exportRepos = exportRepos;
	}

	public void setRepositoryPackage(String repositoryPackage) {
		this.repositoryPackage = repositoryPackage;
	}

	public void setCommentAnnotations(boolean commentAnnotations) {
		this.commentAnnotations = commentAnnotations;
	}

	public void setExportIndexes(boolean exportIndexes) {
		this.exportIndexes = exportIndexes;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	public void setInitializeData(boolean initializeData) {
		this.initializeData = initializeData;
	}

	private static String emptyIfSetToBlank(String value) {
		boolean setToBlank = value == null || value.equalsIgnoreCase("BLANK");
		return setToBlank ? "" : value;
	}
}
