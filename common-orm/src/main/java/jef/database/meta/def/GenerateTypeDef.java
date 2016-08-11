package jef.database.meta.def;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import jef.database.annotation.DateGenerateType;
import jef.tools.StringUtils;

/**
 * 
 * @author publicxtgxrj10
 * 
 */
public class GenerateTypeDef {
	private GenerationType geType;
	private DateGenerateType dateGenerate;

	public GenerateTypeDef(GenerationType strategy) {
		this.geType = strategy;
	}

	public GenerateTypeDef(String generator) {
		this.dateGenerate = getDateGenerateType(generator.toLowerCase());
	}

	private static DateGenerateType getDateGenerateType(String generated) {
		if (generated != null) {
			if ("created".equals(generated)) {
				return DateGenerateType.created;
			} else if ("modified".equals(generated)) {
				return DateGenerateType.modified;
			} else if ("created-sys".equals(generated) || "created_sys".equals(generated)) {
				return DateGenerateType.created_sys;
			} else if ("modified-sys".equals(generated) || "modified_sys".equals(generated)) {
				return DateGenerateType.modified_sys;
			} else if (generated.length() == 0) {
				return DateGenerateType.created;
			}
			throw new IllegalArgumentException("Unknown date generator [" + generated + "]");
		}
		return null;
	}

	public GenerationType getGeType() {
		return geType;
	}

	public void setGeType(GenerationType geType) {
		this.geType = geType;
	}

	public DateGenerateType getDateGenerate() {
		return dateGenerate;
	}

	public void setDateGenerate(DateGenerateType dateGenerate) {
		this.dateGenerate = dateGenerate;
	}

	public boolean isDateTime() {
		return dateGenerate != null;
	}

	public boolean isKeyGeneration() {
		return geType != null;
	}

	public static GenerateTypeDef create(GeneratedValue gv) {
		if (gv == null)
			return null;
		String generator = gv.generator();
		if (StringUtils.isEmpty(generator)) {
			return new GenerateTypeDef(gv.strategy());
		} else {
			return new GenerateTypeDef(generator);
		}
	}
}
