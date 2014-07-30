/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.database.jsqlparser.statement.select;

/**
 * A top clause in the form [TOP row_count] 
 */
public class Top {

    private long rowCount;

    private boolean rowCountJdbcParameter = false;

    public long getRowCount() {
        return rowCount;
    }

    public void setRowCount(long l) {
        rowCount = l;
    }

    public boolean isRowCountJdbcParameter() {
        return rowCountJdbcParameter;
    }

    public void setRowCountJdbcParameter(boolean b) {
        rowCountJdbcParameter = b;
    }

    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }
    
    public StringBuilder appendTo(StringBuilder sb) {
    	sb.append("TOP ");
    	sb.append(rowCountJdbcParameter?"?":String.valueOf(rowCount));
    	return sb;
    }
}
