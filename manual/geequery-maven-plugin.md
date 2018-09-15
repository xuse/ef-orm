# GeeQuery Maven插件

### 1. 概述

插件可以完成以下功能

| Goal        | 用途                                                         |
| ----------- | ------------------------------------------------------------ |
| enhance     | 在编译后的（process-classes阶段），对编译后的实体类进行增强。（核心功能） |
| generate    | 根据配置的数据库链接，逆向生成实体的java源文件。（可选）     |
| export-data | 扫描包中的实体，将其对应在数据库中的表数据以csv格式导出，用于初始化数据。（可选） |
| spring-demo | 将当前工程修改为一个使用spring-boot的geequery工程            |

本文档针对 v 1.12.4.RELEASE以上版本。

插件引用

```xml
<plugin>
	<groupId>com.github.geequery</groupId>
	<artifactId>geequery-maven-plugin</artifactId>
   	<version>${geequery.version}</version>
	<executions>
		<execution>
			<goals>
				<goal>enhance</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
        <!--配置参数，详见后文-->
	</configuration>
	<dependencies>
	</dependencies>
</plugin>
```

### 2. enhance

按上节的配置，增加了`<goal>enhance</goal>`后，在maven构建过程中会自动调用本插件对类文件进行增强。使用本功能无必须配置的参数。

注意：本功能会在编译后对当前工程target/class下的类文件增强，不会对位于jar包中的类文件生效，因此必须配置在定义了实体的模块上。

也可以直接使用命令`mvn geequery:enhance`调用，但开发期间已经提供了动态增强机制，参见手册《Chapter-2   入门 Getting started》，所以插件的功能主要还是用于编译构建过程。

### 3. generate

此功能用于从数据库中根据表结构导出实体 。使用配置如下

```xml
<configuration>
	<jdbcUrl>jdbc:mysql://myhost:3306/test</jdbcUrl>
	<jdbcUser>my_user</jdbcUser>
	<jdbcPassword>my_password</jdbcPassword>
	<packageName>com.github.geequery.demo.entity</packageName>
	<targetFolder>${project.basedir}/target/generated-sources</targetFolder>
    <exportRepos>true</exportRepos>
</configuration>
```

配置说明

| 配置名                         | 说明                                                         | 必须 |
| ------------------------------ | ------------------------------------------------------------ | ---- |
| jdbcUrl                        | 数据库URL                                                    | Y    |
| jdbcUser                       | 数据库用户                                                   | Y    |
| jdbcPassword                   | 数据库密码                                                   | Y    |
| skip                           | true/false，为true时忽略当前任务                             | N    |
| **导出实体相关**               |                                                              |      |
| packageName                    | 导出实体的包名                                               | Y    |
| targetFolder                   | 导出路径（不含包路径）                                       | Y    |
| tableNamePattern               | 正则表达式，用于过滤表名（不配置则导出当前schema下全部表）   | N    |
| commentAnnotations             | true/false，为true时会将数据库中的表和列注释以@Comment注解形式添加到实体。使用实体建表时可以携带这些注释信息。 | N    |
| initializeData                 | true/false，为true时会在实体上增加@InitializeData注解        | N    |
| **Spring-data repository相关** |                                                              |      |
| exportRepos                    | true/false，true时导出实体同时还会生成Spring-data的Repository | N    |
| repositoryPackage              | 导出Spring-data的Repository的包名，如不指定会在entity平级创建一个repos的包，将生成代码放入该包。 | N    |
| repositorySuffix               | 导出Spring-data的Repository类的后缀。默认为实体名+Repository | N    |
| exportRepoTests                | true/false，true时导出Spring-data的Repository同时生成单元测试 | N    |
| testFolder                     | 单元测试所在路径，默认src/test/java                          | N    |

配置完成后，可以手工调用——

```
mvn geequery:generate
```

也可以将其放入maven的generate-sources周期，

```xml
<executions>
	<execution>
		<goals>
			<goal>generate</goal>
			<goal>enhance</goal>
		</goals>
	</execution>
</executions>
```

放入后会在每次Maven编译前自动执行。

> 关于生成文件的覆盖问题。如果发生文件已经存在，会检查文件中的@NotModified注解。如果该注解存在，视为文件未被用户更改过，会进行覆盖。因此如果用户不希望文件被覆盖，请去除类上的该注解。

### 4. export-data

此功能可以将指定表中的数据导出到本地资源文件夹，这些数据是csv格式的，可以用Excel打开自行编辑。

本操作不是从数据中罗列表进行数据导出，而是先指定一个包，根据包内的实体，连接到对应的表进行导出。该功能主要配合GeeQuery中的数据初始化功能使用。请参阅手册 14.2. 数据初始化。

export-data支持的配置项如下

| 配置名                       | 说明                                                        | 必须 |
| ---------------------------- | ----------------------------------------------------------- | ---- |
| jdbcUrl                      | 数据库URL                                                   | Y    |
| jdbcUser                     | 数据库用户                                                  | Y    |
| jdbcPassword                 | 数据库密码                                                  | Y    |
| skip                         | true/false，true时忽略当前任务                              | N    |
| resourceFolder               | 导出路径，默认位于src\main\resources                        | Y    |
| exportDataFromPackage        | 导出该包下类文件对应的表。                                  | Y    |
| withInitializeDataAnnotation | 设置为true时，仅当类上具有@InitializeData注解的时才会导出。 | N    |
| maxResult                    | 单张表最多导出行数。默认5000行。                            | N    |

配置完成后，可以手工调用该任务

```
mvn geequery:export-data
```

### 5. spring-demo

该功能用于快速搭建一个GeeQuery的Spring-data+spring-boot的示例。其中会用到geequery-spring-boot-starter等Spring-boot模块。

功能包括——

* 生成可运行的SpringApplication类，生成默认的application.properties配置
* 生成Spring-Application的单元测试。

上述功能可以帮助对GeeQuery的Spring-boot集成不够熟悉的同学，快速搭建一个可运行的数据库服务程序。同时在application.properties中包含了常用的若干项geequery配置信息。

| 配置名             | 说明                                                   | 必须 |
| ------------------ | ------------------------------------------------------ | ---- |
| skip               | true/false，true时忽略当前任务                         | N    |
| testFolder         | 单元测试所在路径，默认src/test/java                    | N    |
| applicationPackage | SpringApplication所在包，默认com.mycompany.application | N    |
| applicationName    | SpringApplication类名，默认MyGeeQueryApplication       | N    |

可以手工调用该任务

```
mvn geequery:springboot-init
```

### 6. 关于m2e lifecycle报错

Eclipse的maven lifecycle检查特别严格。所以如果出现Eclipse的Maven报错。可以通过增加以下配置解决。

```xml
<build>
   ...
<pluginManagement>
	<plugins>
		<plugin>
			<groupId>org.eclipse.m2e</groupId>
			<artifactId>lifecycle-mapping</artifactId>
			<version>1.0.0</version>
			<configuration>
				<lifecycleMappingMetadata>
					<pluginExecutions>
						<pluginExecution>
							<pluginExecutionFilter>
								<groupId>com.github.geequery</groupId>
								<artifactId>geequery-maven-plugin</artifactId>
								<versionRange>[1.0,)</versionRange>
								<goals>
									<goal>generate</goal>
									<goal>enhance</goal>
								</goals>
							</pluginExecutionFilter>
							<action>
								<ignore />
							</action>
						</pluginExecution>
					</pluginExecutions>
				</lifecycleMappingMetadata>
			</configuration>
		</plugin>
	</plugins>
</pluginManagement>
```

