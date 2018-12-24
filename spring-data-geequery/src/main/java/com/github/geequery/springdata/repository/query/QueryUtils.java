package com.github.geequery.springdata.repository.query;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.github.geequery.springdata.annotation.IgnoreIf;

import jef.database.Session;
import jef.database.SessionFactory;

public class QueryUtils {
    static boolean isIgnore(IgnoreIf ignoreIf, Object obj) {
        switch (ignoreIf.value()) {
        case Empty:
            return obj == null || String.valueOf(obj).length() == 0;
        case Negative:
            if (obj instanceof Number) {
                return ((Number) obj).longValue() < 0;
            } else {
                throw new IllegalArgumentException("can not calcuate is 'NEGATIVE' on parameter which is not a number.");
            }
        case Null:
            return obj == null;
        case Zero:
            if (obj instanceof Number) {
                return ((Number) obj).longValue() == 0;
            } else {
                throw new IllegalArgumentException("can not calcuate is 'IS_ZERO' on parameter which is not a number.");
            }
        case ZeroOrNagative:
            if (obj instanceof Number) {
                return ((Number) obj).longValue() <= 0;
            } else {
                throw new IllegalArgumentException("can not calcuate is 'IS_ZERO_OR_NEGATIVE' on parameter which is not a number.");
            }
        default:
            throw new IllegalArgumentException("Unknown ignoreIf type:" + ignoreIf.value());
        }
    }

    public static boolean hasNamedParameter(String query) {
        return StringUtils.hasText(query) && NAMED_PARAMETER.matcher(query).find();
    }

    /**
     * 获得EntityManager
     * 
     * @return
     */
    public static final Session getEntityManager(SessionFactory jefEmf) {
        return jefEmf.getSession();
    }

    private static final String IDENTIFIER = "[\\p{Lu}\\P{InBASIC_LATIN}\\p{Alnum}._$]+";
    private static final Pattern NAMED_PARAMETER = Pattern.compile(":" + IDENTIFIER + "|\\#" + IDENTIFIER, CASE_INSENSITIVE);
}
