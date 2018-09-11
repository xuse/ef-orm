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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal springboot-init
 * @phase generate-resources
 */

public class SpringbootMojo extends AbstractMojo {

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

	}

	protected boolean isForTest() {
		return false;
	}

	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	private static String emptyIfSetToBlank(String value) {
		boolean setToBlank = value == null || value.equalsIgnoreCase("BLANK");
		return setToBlank ? "" : value;
	}
}
