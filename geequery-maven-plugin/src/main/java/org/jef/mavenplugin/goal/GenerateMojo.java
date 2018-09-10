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
package org.jef.mavenplugin.goal;

import java.io.File;
import java.sql.SQLException;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.github.geequery.codegen.EntityGenerator;
import com.github.geequery.codegen.MetaProvider.DbClientProvider;
import com.querydsl.sql.codegen.MetaDataExporter;
import com.querydsl.sql.codegen.support.NumericMapping;
import com.querydsl.sql.codegen.support.RenameMapping;
import com.querydsl.sql.codegen.support.TypeMapping;

import jef.database.DbClient;
import jef.database.DbClientBuilder;
import jef.database.DbUtils;

/**
 * @goal generate
 * @phase generate-sources
 */

public class GenerateMojo extends AbstractMojo {


	/**
	 * The server id in settings.xml to use as an alternative to jdbcUser and
	 * jdbcPassword
	 * 
	 * @parameter
	 */
	private String server;

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
	 * name prefix for querydsl-types (default: "Q")
	 * 
	 * @parameter default-value="Q"
	 */
	private String namePrefix;

	/**
	 * name suffix for querydsl-types (default: "")
	 * 
	 * @parameter default-value=""
	 */
	private String nameSuffix;

	/**
	 * name prefix for bean types (default: "")
	 * 
	 * @parameter default-value=""
	 */
	private String beanPrefix;

	/**
	 * name suffix for bean types (default: "")
	 * 
	 * @parameter default-value=""
	 */
	private String beanSuffix;

	/**
	 * package name for sources
	 * 
	 * @parameter
	 * @required
	 */
	private String packageName;

	/**
	 * schemaPattern a schema name pattern; must match the schema name as it is
	 * stored in the database; "" retrieves those without a schema; {@code null}
	 * means that the schema name should not be used to narrow the search
	 * (default: null)
	 *
	 * @parameter
	 */
	private String schemaPattern;

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
	 * target source folder to create the bean sources into
	 *
	 * @parameter
	 */
	private String beansTargetFolder;

	/**
	 * namingstrategy class to override (default: DefaultNamingStrategy)
	 *
	 * @parameter
	 */
	private String namingStrategyClass;

	/**
	 * name for bean serializer class
	 *
	 * @parameter
	 */
	private String beanSerializerClass;

	/**
	 * name for serializer class
	 *
	 * @parameter
	 */
	private String serializerClass;

	/**
	 * serialize beans as well
	 *
	 * @parameter default-value=false
	 */
	private boolean exportBeans;

	/**
	 * additional interfaces to be implemented by beans
	 *
	 * @parameter
	 */
	private String[] beanInterfaces;

	/**
	 * switch for {@code toString} addition
	 *
	 * @parameter default-value=false
	 */
	private boolean beanAddToString;

	/**
	 * switch for full constructor addition
	 *
	 * @parameter default-value=false
	 */
	private boolean beanAddFullConstructor;

	/**
	 * switch to print supertype content
	 *
	 * @parameter default-value=false
	 */
	private boolean beanPrintSupertype;

	/**
	 * wrap key properties into inner classes (default: false)
	 *
	 * @parameter default-value=false
	 */
	private boolean innerClassesForKeys;

	/**
	 * export validation annotations (default: false)
	 *
	 * @parameter default-value=false
	 */
	private boolean validationAnnotations;

	/**
	 * export column annotations (default: false)
	 *
	 * @parameter default-value=false
	 */
	private boolean columnAnnotations;

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
	 * switch for generating scala sources
	 *
	 * @parameter default-value=false
	 */
	private boolean createScalaSources;

	/**
	 * switch for using schema as suffix in package generation, full package
	 * name will be {@code ${packageName}.${schema}}
	 *
	 * @parameter default-value=false
	 */
	private boolean schemaToPackage;

	/**
	 * switch to normalize schema, table and column names to lowercase
	 *
	 * @parameter default-value=false
	 */
	private boolean lowerCase;

	/**
	 * switch to export tables
	 *
	 * @parameter default-value=true
	 */
	private boolean exportTables;

	/**
	 * switch to export views
	 *
	 * @parameter default-value=true
	 */
	private boolean exportViews;

	/**
	 * switch to export all types
	 *
	 * @parameter default-value=false
	 */
	private boolean exportAll;

	/**
	 * switch to export primary keys
	 *
	 * @parameter default-value=true
	 */
	private boolean exportPrimaryKeys;

	/**
	 * switch to export foreign keys
	 *
	 * @parameter default-value=true
	 */
	private boolean exportForeignKeys;

	/**
	 * switch to export direct foreign keys
	 *
	 * @parameter default-value=true
	 */
	private boolean exportDirectForeignKeys;

	/**
	 * switch to export inverse foreign keys
	 *
	 * @parameter default-value=true
	 */
	private boolean exportInverseForeignKeys;

	/**
	 * override default column order (default: alphabetical)
	 *
	 * @parameter
	 */
	private String columnComparatorClass;

	/**
	 * switch to enable spatial type support
	 *
	 * @parameter default-value=false
	 */
	private boolean spatial;

	/**
	 * Comma-separated list of table types to export (allowable values will
	 * depend on JDBC driver). Allows for arbitrary set of types to be exported,
	 * e.g.: "TABLE, MATERIALIZED VIEW". The exportTables and exportViews
	 * parameters will be ignored if this parameter is set. (default: none)
	 *
	 * @parameter
	 */
	private String tableTypesToExport;

	/**
	 * java import added to generated query classes: com.bar for package
	 * (without .* notation) com.bar.Foo for class
	 *
	 * @parameter
	 */
	private String[] imports;

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
		db = new DbClientBuilder(this.jdbcUrl, this.jdbcUser, this.jdbcPassword, 2).setEnhanceScanPackages(false).build();
		g.setProvider(new DbClientProvider(db));
		g.setProfile(db.getProfile(null));
		g.addExcludePatter(".*_\\d+$"); // 防止出现分表
		g.addExcludePatter("AAA"); // 排除表
		g.setMaxTables(999);
		g.setSrcFolder(new File(targetFolder));
		g.setBasePackage(this.packageName);
		try {
			g.generateSchema();
		} catch (SQLException e) {
			throw DbUtils.toRuntimeException(e);
		}finally {
			db.shutdown();
		}
	}

	protected boolean isForTest() {
		return false;
	}


	public void setServer(String server) {
		this.server = server;
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

	public void setNamePrefix(String namePrefix) {
		this.namePrefix = namePrefix;
	}

	public void setNameSuffix(String nameSuffix) {
		this.nameSuffix = nameSuffix;
	}

	public void setBeanInterfaces(String[] beanInterfaces) {
		this.beanInterfaces = beanInterfaces;
	}

	public void setBeanPrefix(String beanPrefix) {
		this.beanPrefix = beanPrefix;
	}

	public void setBeanSuffix(String beanSuffix) {
		this.beanSuffix = beanSuffix;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public void setSchemaPattern(String schemaPattern) {
		this.schemaPattern = schemaPattern;
	}

	public void setTableNamePattern(String tableNamePattern) {
		this.tableNamePattern = tableNamePattern;
	}

	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}

	public void setNamingStrategyClass(String namingStrategyClass) {
		this.namingStrategyClass = namingStrategyClass;
	}

	public void setBeanSerializerClass(String beanSerializerClass) {
		this.beanSerializerClass = beanSerializerClass;
	}

	public void setSerializerClass(String serializerClass) {
		this.serializerClass = serializerClass;
	}

	public void setExportBeans(boolean exportBeans) {
		this.exportBeans = exportBeans;
	}

	public void setInnerClassesForKeys(boolean innerClassesForKeys) {
		this.innerClassesForKeys = innerClassesForKeys;
	}

	public void setValidationAnnotations(boolean validationAnnotations) {
		this.validationAnnotations = validationAnnotations;
	}

	public void setColumnAnnotations(boolean columnAnnotations) {
		this.columnAnnotations = columnAnnotations;
	}

	public void setCustomTypes(String[] customTypes) {
		this.customTypes = customTypes;
	}

	public void setCreateScalaSources(boolean createScalaSources) {
		this.createScalaSources = createScalaSources;
	}

	public void setSchemaToPackage(boolean schemaToPackage) {
		this.schemaToPackage = schemaToPackage;
	}

	public void setLowerCase(boolean lowerCase) {
		this.lowerCase = lowerCase;
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

	public void setImports(String[] imports) {
		this.imports = imports;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	private static String emptyIfSetToBlank(String value) {
		boolean setToBlank = value == null || value.equalsIgnoreCase("BLANK");
		return setToBlank ? "" : value;
	}
}
