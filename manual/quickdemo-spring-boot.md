### Step1

创建一个空白的Maven项目。修改pom.xml如下。其中数据库驱动、数据库连接信息请修改以适配你自己的数据库。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>org.sample</groupId>
	<artifactId>sample</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<properties>
		<spring-boot.version>2.0.4.RELEASE</spring-boot.version>
		<geequery.version>1.12.4.RELEASE</geequery.version>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<maven.compiler.source>1.8</maven.compiler.source>
		<maven.compiler.target>1.8</maven.compiler.target>
		<maven.compiler.testSource>1.8</maven.compiler.testSource>
		<maven.compiler.testTarget>1.8</maven.compiler.testTarget>
	</properties>
	<dependencies>
		<dependency>
			<groupId>com.github.geequery</groupId>
			<artifactId>geequery-spring-boot-starter</artifactId>
			<version>${geequery.version}</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-jdbc</artifactId>
			<version>${spring-boot.version}</version>
		</dependency>
		<dependency>
			<groupId>joda-time</groupId>
			<artifactId>joda-time</artifactId>
			<version>2.9.9</version>
		</dependency>
		<!-- test dependencies -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
			<version>${spring-boot.version}</version>
		</dependency>
		<dependency>
			<groupId>com.github.geequery</groupId>
			<artifactId>geequery-spring-boot-starter-test</artifactId>
			<version>${geequery.version}</version>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<version>1.4.197</version>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>junit</groupId>
			<artifactId>junit</artifactId>
			<version>4.12</version>
			<scope>test</scope>
		</dependency>
		<!-- 请替换为你自己的数据库驱动 -->
		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
			<version>5.1.47</version>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>com.github.geequery</groupId>
				<artifactId>geequery-maven-plugin</artifactId>
				<version>1.12.3.RELEASE</version>
				<executions>
					<execution>
						<goals><goal>enhance</goal></goals>
					</execution>
				</executions>
				<configuration>
                    <!-- 这里请替换为你自己的数据库连接地址与账户密码 -->
					<jdbcUrl>jdbc:mysql://host:3306/test</jdbcUrl>
					<jdbcUser>myuser</jdbcUser>
					<jdbcPassword>mypassword</jdbcPassword>
                    <targetFolder>${project.basedir}/src/main/java</targetFolder>
					<packageName>com.mycompany.demo.entity</packageName>
                    <commentAnnotations>true</commentAnnotations>
                    
                    <exportRepos>true</exportRepos>
                    <exportRepoTests>true</exportRepoTests>
					                    
					<exportDataFrom>com.mycompany.demo.entity</exportDataFrom>
                    <resourceFolder>src/main/resources/data</resourceFolder>
            		<applicationPackage>com.mycompany.application</applicationPackage>
					<applicationName>MyGeeQueryInSpringBoot</applicationName>
				</configuration>
				<!-- 请替换为你自己的数据库驱动 -->
                <dependencies>
					<dependency>
						<groupId>mysql</groupId>
						<artifactId>mysql-connector-java</artifactId>
						<version>5.1.47</version>
					</dependency>
				</dependencies>
			</plugin>
		</plugins>
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
											<goal>export-data</goal>
											<goal>enhance</goal>
										</goals>
									</pluginExecutionFilter>
									<action><ignore /></action>
								</pluginExecution>
							</pluginExecutions>
						</lifecycleMappingMetadata>
					</configuration>
				</plugin>
			</plugins>
		</pluginManagement>
	</build>
</project>
```

### Step 2

执行命令

```
mvn geequery:generate
```

该命令会从数据库中导出数据表并生成entity 和 repository类，还会为每个repository生成空白的单元测试类。

### Step 3

执行命令

```
mvn geequery:springboot-init
```

该命令可以生成——

* 默认的Spring-boot Application启动类（main方法）

* application.properties，并配置好了数据库连接和一些常用参数

* SpringBoot Application的测试类（空白）。

在本例中，您可以——
1. 查看并调节位于application.properties，支持配置JDBC数据源、连接池（默认使用hikariCP）、slf4j日志（默认使用logback）、以及GeeQuery的各类参数。
2. 运行com.mycompany.application.MyGeeQueryInSpringBoot，这是一个连接了数据库的应用程序，可以随时增加数据库增删改查等各种逻辑。
3. 运行位于测试目录下的类MyGeeQueryInSpringBootTest以及各个Repository 的单元测试。其中Repository 的单元测试会自动使用H2在内存中模拟。

