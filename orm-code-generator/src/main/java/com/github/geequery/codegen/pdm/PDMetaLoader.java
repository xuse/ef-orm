/* 
 * This file is part of Mosquito meta-loader.
 *
 * Mosquito meta-loader is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mosquito meta-loader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.geequery.codegen.pdm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.github.geequery.codegen.pdm.model.MetaColumn;
import com.github.geequery.codegen.pdm.model.MetaKey;
import com.github.geequery.codegen.pdm.model.MetaModel;
import com.github.geequery.codegen.pdm.model.MetaReference;
import com.github.geequery.codegen.pdm.model.MetaTable;
import jef.common.JefException;
import jef.common.log.LogUtil;
import jef.tools.IOUtils;
import jef.tools.XMLUtils.XmlFixedReader;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PDMetaLoader implements IMetaLoader {

	class ActualRefStack {
		private Stack<PDMReference> stk = new Stack<PDMReference>();

		public PDMReference peek() throws EmptyStackException {
			return (PDMReference) stk.peek();
		}

		public PDMReference pop() throws EmptyStackException {
			return (PDMReference) stk.pop();
		}

		public PDMReference push(PDMReference aActualRef) {
			return (PDMReference) stk.push(aActualRef);
		}
	}

	class ActualXEStack {
		private Stack<XmlElem> stk = new Stack<XmlElem>();

		public XmlElem peek() throws EmptyStackException {
			return (XmlElem) stk.peek();
		}

		public XmlElem pop() throws EmptyStackException {
			return (XmlElem) stk.pop();
		}

		public XmlElem push(XmlElem aXE) {
			return (XmlElem) stk.push(aXE);
		}

		public XmlElem underPeek() throws EmptyStackException {
			int len = stk.size();
			if (len < 2)
				throw new EmptyStackException();
			return (XmlElem) stk.elementAt(len - 2);
		}

	}

	/**
	 * Stores xoe when when it is declared \ <nmsp:Elem Id="aKey"/>
	 */

	class HT_ID_XOE {
		private Hashtable<String, XmlElem> htId_XOE;

		public HT_ID_XOE(int ainitCap) {
			htId_XOE = new Hashtable<String, XmlElem>(ainitCap);
		}

		/**
		 * @param aId
		 * @return xoe
		 */
		public XmlElem get(String aId) {
			return (XmlElem) htId_XOE.get(aId);
		}

		/**
		 * @param id
		 *            string (key)
		 * @param xe
		 *            (value)
		 * @return prev xe
		 */
		public XmlElem put(String aId, XmlElem aXOE) {
			return (XmlElem) htId_XOE.put(aId, aXOE);
		}
	}

	class PDMColumn extends PDMElement {

		protected String comment = "";

		protected String datatype = "";

		protected String defaultVal = "";

		protected PDMColumn fkParent;

		protected String length = "";

		protected String listOfVals = "";

		protected boolean mandatory = false;

		protected String parentTable = "";

		protected boolean partOfPK = false;

		protected String precision = "";

		protected MetaColumn getCreateMetaColumn() throws SAXException {
			MetaColumn retVal = null;
			if ((retVal = getMetaColumnByPDMColId(id)) == null) {
				// IT DOESN'T EXIST. Creating ...
				int len = 0, prec = 0;
				try {
					len = Integer.parseInt(this.length);
				} catch (NumberFormatException nfe) {
				}
				try {
					prec = Integer.parseInt(this.precision);
				} catch (NumberFormatException nfe) {
				}

				String dataTypeName = new String(datatype);
				int index = dataTypeName.indexOf("(");
				if (index != -1)
					dataTypeName = dataTypeName.substring(0, index);

				// String matchDT = datatype.
				String jClassName = dataTypeName.trim();
				// Fk MetaColumn parent is temporarly set to null
				// it will be resolved later
				MetaColumn fkMetaCol = null;

				retVal = new MetaColumn(this.code, this.name, this.parentTable, this.mandatory, fkMetaCol, jClassName, len, prec, comment);
				retVal.setDefaultVal(defaultVal);
				// list of values...
				if (!"".equals(listOfVals)) {
					String spEntries[] = listOfVals.split("\n");
					for (int i = 0; i < spEntries.length; i++) {
						String spValueLabel[] = spEntries[i].trim().split("\t");
						retVal.put(spValueLabel[0].trim(), spValueLabel[1].trim());
					}
				}
				try {
					htCreatedMetaColumns_ID_MC.put(id, retVal);
				} catch (Exception e) {
					// if (id == null || retVal == null)
					throw new SAXException("Column already exists! id = " + id + e.getMessage());
				}
			}
			return retVal;
		}
	}

	class PDMElement {

		protected String code = "";

		protected String comment = "";

		protected String id = "";

		protected String name = "";

	}

	class PDMKey extends PDMElement {
		ArrayList<String> alColumns = new ArrayList<String>();

		public void addCol(String aColId) {
			alColumns.add(aColId);
		}

		public MetaKey createMetaKey(Hashtable<String, PDMElement> aIdResources) throws SAXException {
			MetaKey retVal = new MetaKey(name, code);
			Iterator<String> iKeyCols = alColumns.iterator();
			while (iKeyCols.hasNext()) {
				String crntColId = iKeyCols.next();
				try {
					PDMColumn col = (PDMColumn) aIdResources.get(crntColId);
					retVal.addColumn(col.getCreateMetaColumn());
				} catch (Exception e) {
					LogUtil.error("Can't find MetaColumn to add " + "to MetaKey from id: " + crntColId + ". " + e.getMessage());
					throw new SAXException("PDML: " + e.getMessage());
				}
			}
			return retVal;
		}
	}

	class PDMModel extends PDMElement {
		ArrayList<String> alTableIds = new ArrayList<String>();

		protected void addTable(String aPDMTableId) throws SAXException {
			alTableIds.add(aPDMTableId);
		}

		protected MetaModel createMetaModel(Hashtable<String, PDMElement> aIdResources) throws SAXException {
			MetaModel retVal = new MetaModel(code, name, comment);
			Iterator<String> iIdTables = alTableIds.iterator();
			while (iIdTables.hasNext()) {
				String crntTableId = iIdTables.next();
				try {
					PDMTable pdmTable = (PDMTable) aIdResources.get(crntTableId);
					retVal.addTable(pdmTable.createMetaTable(aIdResources));
				} catch (Exception e) {
					LogUtil.exception(e);
					LogUtil.error("Can't create MetaTable from id: " + crntTableId + ". " + e.getMessage());
					throw new SAXException("PDML: " + e.getMessage());
				}
			}
			return retVal;
		}
	}

	class PDMReference extends PDMElement {
		private ArrayList<String> alRefJoins = new ArrayList<String>();

		String parentTableId = "";

		String sourceTableId = "";
		
		String cardinality = "";

		public void addRefJoin(String aRefJoin) {
			alRefJoins.add(aRefJoin);
		}

		public MetaReference createMetaReference(Hashtable<String, PDMElement> aIdResources) throws SAXException {
			MetaReference retVal = new MetaReference(name, code,cardinality);
			htCreatedRefs.put(id, retVal);
			/*
			 * try{ retVal.setParentTable((MetaTable)
			 * htCreatedMetaTables.get(parentTableId));
			 * retVal.setSourceTable((MetaTable)
			 * htCreatedMetaTables.get(sourceTableId)); }catch(Exception e) {
			 * logger.error("Can't find parent MetaTable to add " + "to
			 * MetaReference id: " + parentTableId + ". " + e.getMessage());
			 * throw new SAXException("PDML: " + e.getMessage()); } Iterator
			 * iJoins = alRefJoins.iterator(); while (iJoins.hasNext()) { String
			 * currRefJoinId = (String) iJoins.next(); ReferenceJoin currRefJoin
			 * = (ReferenceJoin) aIdResources.get(currRefJoinId); try {
			 * MetaColumn col = getMetaColumnByPDMColId(currRefJoin.colId);
			 * retVal.addColumn(col); } catch (Exception e) {
			 * logger.error("Can't find MetaColumn to add " + "to MetaReference
			 * from id: " + currRefJoin.colId + ". " + e.getMessage()); throw
			 * new SAXException("PDML: " + e.getMessage()); } }
			 */
			return retVal;
		}
	}

	class PDMShortcut extends PDMElement {
	}

	class PDMTable extends PDMElement {
		private ArrayList<String> alColumns = new ArrayList<String>();

		private ArrayList<String> alKeys = new ArrayList<String>();

		private ArrayList<String> alRefs = new ArrayList<String>();

		protected String comment = "";

		protected void addCol(String aColId) throws SAXException {
			alColumns.add(aColId);
		}

		public void addKey(String aKeyId) {
			alKeys.add(aKeyId);
		}

		public void addRef(String aRefId) {
			alRefs.add(aRefId);
		}

		protected MetaTable createMetaTable(Hashtable<String, PDMElement> aIdResources) throws SAXException {
			MetaTable retVal = new MetaTable(code, name, comment);
			htCreatedTables.put(id, retVal);
			htCreatedTablesByCode.put(code, retVal);
			// create/add MetaKeys
			Iterator<String> iKeys = alKeys.iterator();
			while (iKeys.hasNext()) {
				String crntPDMKeyId = (String) iKeys.next();
				try {
					PDMKey crntPDMKey = (PDMKey) aIdResources.get(crntPDMKeyId);
					retVal.addKey(crntPDMKey.createMetaKey(aIdResources));
				} catch (Exception e) {
					LogUtil.error("Can't create MetaKey from id: " + crntPDMKeyId + ". " + e.getMessage());
					throw new SAXException("PDML: " + e.getMessage());
				}
			}

			// create/add References
			Iterator<String> iRefs = alRefs.iterator();
			while (iRefs.hasNext()) {
				String currRefId = (String) iRefs.next();
				PDMReference currRef = (PDMReference) aIdResources.get(currRefId);
				try {
					retVal.addRef(currRef.createMetaReference(aIdResources));
				} catch (Exception e) {
					LogUtil.error("Can't create MetaReference from id: " + currRefId + ". " + e.getMessage());
					throw new SAXException("PDML: " + e.getMessage());
				}
			}

			// create/add MetaColumns
			Iterator<String> iIdColumns = alColumns.iterator();
			while (iIdColumns.hasNext()) {
				try {
					String crntColId = (String) iIdColumns.next();
					PDMColumn pdmCol = (PDMColumn) aIdResources.get(crntColId);
					retVal.addCol(pdmCol.getCreateMetaColumn());
				} catch (Exception e) {
				}
			}

			return retVal;
		}

		public Iterator<String> elements() {
			return alColumns.iterator();
		}

	}

	class ReferenceJoin extends PDMElement {

		String colId = "";

		String fkParentColId = "";

		public void join(Hashtable<String, PDMElement> aIdResources) throws SAXException {
			try {
				PDMColumn pdmCol = (PDMColumn) aIdResources.get(colId);
				PDMColumn pdmFkCol = (PDMColumn) aIdResources.get(fkParentColId);
				pdmCol.fkParent = pdmFkCol;
			} catch (Exception e) {
				LogUtil.error("ReferenceJoin: Columns(" + colId + ", " + fkParentColId);
				//throw new SAXException("PDML: id=" + colId + e.getMessage());
			}
		}
	}

	/**
	 * PD's <o:Object Attr="o123"> Object :=
	 * {Model|Table|Column|RefernceJoin|Key} Attr := {Id|Ref}
	 */
	class XmlElem {

		public final static String A_CODE = "a:Code";

		public final static String A_COMMENT = "a:Comment";

		public final static String A_DATATYPE = "a:DataType";

		public final static String A_DEFAULT = "a:DefaultValue";

		public final static String A_DESCRIPTION = "a:Description";

		public final static String A_LENGTH = "a:Length";

		public final static String A_LISTOFVALS = "a:ListOfValues";

		public final static String A_MANDATORY = "a:Mandatory";
		
		public final static String A_CARDINALITY = "a:Cardinality";

		public final static String A_NAME = "a:Name";

		public final static String A_PRECISION = "a:Precision";

		public final static String C_CHILD_TABLE = "c:ChildTable";

		public final static String C_COLUMNS = "c:Columns";

		public final static String C_KEY_COLUMNS = "c:Key.Columns";

		public final static String C_KEYS = "c:Keys";

		public final static String C_OBJECT_1 = "c:Object1";

		public final static String C_OBJECT_2 = "c:Object2";

		public final static String C_PARENT_TABLE = "c:ParentTable";

		public final static String C_REFERENCES = "c:References";
		
		public final static String C_TABLES = "c:Tables";

		public final static String O_COLUMN = "o:Column";

		public final static String O_KEY = "o:Key";

		public final static String O_MODEL = "o:Model";

		public final static String O_REFERECE_JOIN = "o:ReferenceJoin";

		public final static String O_REFERENCE = "o:Reference";

		public final static String O_SHORTCUT = "o:Shortcut";

		public final static String O_TABLE = "o:Table";

		protected String id = null;

		protected PDMElement pdmElement = null;

		protected String qName = null;

		public XmlElem(String aQName, String aId, PDMElement aPDMElement) {
			qName = aQName;
			id = aId;
			pdmElement = aPDMElement;
		}

		public boolean isACode() {
			return A_CODE.equals(qName);
		}

		public boolean isAComment() {
			return A_COMMENT.equals(qName);
		}

		public boolean isADataType() {
			return A_DATATYPE.equals(qName);
		}

		public boolean isADefaultVal() {
			return A_DEFAULT.equals(qName);
		}

		public boolean isADescription() {
			return A_DESCRIPTION.equals(qName);
		}

		public boolean isALength() {
			return A_LENGTH.equals(qName);
		}

		public boolean isAListOfVals() {
			return A_LISTOFVALS.equals(qName);
		}

		public boolean isAMandatory() {
			return A_MANDATORY.equals(qName);
		}

		public boolean isAName() {
			return A_NAME.equals(qName);
		}

		public boolean isAPrecision() {
			return A_PRECISION.equals(qName);
		}

		public boolean isCChildTable() {
			return C_CHILD_TABLE.equals(qName);
		}

		public boolean isCColumns() {
			return C_COLUMNS.equals(qName);
		}

		public boolean isCKeyColumns() {
			return C_KEY_COLUMNS.equals(qName);
		}

		public boolean isCKeys() {
			return C_KEYS.equals(qName);
		}

		public boolean isCObject1() {
			return C_OBJECT_1.equals(qName);
		}

		public boolean isCObject2() {
			return C_OBJECT_2.equals(qName);
		}

		public boolean isCReferences() {
			return C_REFERENCES.equals(qName);
		}

		public boolean isCTables() {
			return C_TABLES.equals(qName);
		}

		public boolean isOColumn() {
			return pdmElement instanceof PDMColumn;
		}

		public boolean isOKey() {
			return O_KEY.equals(qName);
		}

		public boolean isOModel() {
			return pdmElement instanceof PDMModel;
		}

		public boolean isOReference() {
			return O_REFERENCE.equals(qName);
		}

		public boolean isCParentTable() {
			return C_PARENT_TABLE.equals(qName);
		}

		public boolean isACardinality() {
			return A_CARDINALITY.equals(qName);
		}

		
		public boolean isOReferenceJoin() {
			return O_REFERECE_JOIN.equals(qName);
		}

		public boolean isOShortcut() {
			return O_SHORTCUT.equals(qName);
		}

		// public boolean isPackage() {
		// return objType == OT_PACKAGE;
		// }

		public boolean isOTable() {
			return pdmElement instanceof PDMTable;
		}

	}

	/**
	 * Handles SAX2 events
	 */

	class XmlHandlerDelegate extends DefaultHandler {
		PDMColumn actualColumn = null;

		PDMKey actualKey = null;

		PDMReference actualRef = null;

		ReferenceJoin actualRJ = null;

		PDMShortcut actualShortcut = null;

		PDMTable actualTable = null;

		XmlElem actualXE = null;

		HT_ID_XOE htId_XE = new HT_ID_XOE(INIT_HT_CAP);

		XmlElem prevActualXE = null;

		ActualRefStack stkActualRef = new ActualRefStack();

		ActualXEStack stkActualXE = new ActualXEStack();

		/**
		 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
		 */
		public void characters(char[] ch, int start, int length) throws SAXException {
			String sCh = (new String(ch, start, length));
			XmlElem actualXE = null;
			XmlElem prevActualXE = null;
			try {
				actualXE = stkActualXE.peek();
				prevActualXE = stkActualXE.underPeek();
			} catch (EmptyStackException ese) {
				return;
			}
			try {
				actualRef = stkActualRef.peek();
			} catch (EmptyStackException ese) {
			}

			if (prevActualXE == null || actualXE == null)
				return;

			// ok ...
			if (actualXE.isAName()) {
				// ________________________________________________________A_NAME
				if (prevActualXE.isOColumn()) {
					actualColumn.name += sCh;
				} else if (prevActualXE.isOKey()) {
					actualKey.name += sCh;
				} else if (prevActualXE.isOTable()) {
					actualTable.name += sCh;
				} else if (prevActualXE.isOModel()) {
					actualMModel.name += sCh;
				} else if (prevActualXE.isOReference()) {
					actualRef.name += sCh;
				}
			} else if (actualXE.isACode()) {
				// ________________________________________________________A_CODE
				if (prevActualXE.isOColumn()) {
					actualColumn.code += sCh;
				} else if (prevActualXE.isOKey()) {
					actualKey.code += sCh;
				} else if (prevActualXE.isOTable()) {
					actualTable.code += sCh;
				} else if (prevActualXE.isOModel()) {
					actualMModel.code += sCh;
				} else if (prevActualXE.isOReference()) {
					actualRef.code += sCh;
				} else if (prevActualXE.isOShortcut()) {
					actualShortcut.code += sCh;
				}
			} else if (actualXE.isADefaultVal()) {
				// _____________________________________________________A_DEFAULT
				if (prevActualXE.isOColumn()) {
					actualColumn.defaultVal += sCh;
				}
			} else if (actualXE.isADescription()) {
				// _________________________________________________A_DESCRIPTION
				if (prevActualXE.isOTable()) {
					actualTable.comment += sCh;
				} else if (prevActualXE.isOModel()) {
					actualMModel.comment += sCh;
				}
			} else if (actualXE.isAListOfVals()) {
				// __________________________________________________A_LISTOFVALS
				if (prevActualXE.isOColumn()) {
					actualColumn.listOfVals += sCh;
				}
			} else if (actualXE.isAMandatory()) {
				// ___________________________________________________A_MANDATORY
				if (prevActualXE.isOColumn()) {
					actualColumn.mandatory = ("1".equals(sCh));
				}
			} else if (actualXE.isAComment()) {
				// _____________________________________________________A_COMMENT
				if (prevActualXE.isOColumn()) {
					actualColumn.comment += sCh;
				} else if (prevActualXE.isOTable()) {
					actualTable.comment += sCh;
				} else if (prevActualXE.isOModel()) {
					actualMModel.comment += sCh;
				}
			} else if (actualXE.isADataType()) {
				// ____________________________________________________A_DATATYPE
				if (prevActualXE.isOColumn()) {
					actualColumn.datatype += sCh;
				}
			} else if (actualXE.isALength()) {
				// ______________________________________________________A_LENGTH
				if (prevActualXE.isOColumn()) {
					actualColumn.length += sCh;
				}
			} else if (actualXE.isAPrecision()) {
				// ___________________________________________________A_PRECISION
				if (prevActualXE.isOColumn()) {
					actualColumn.precision += sCh;
				}
			} else if (actualXE.isACardinality()) {
				this.actualRef.cardinality+= sCh;
				
			}
		}

		/**
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
		 *      java.lang.String, java.lang.String)
		 */
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals(XmlElem.A_CODE)) {
				// In case of shortcut
				if (actualShortcut != null) {
					htShortcuts.put(actualShortcut.id, actualShortcut);
				}
			} else if (qName.equals(XmlElem.O_COLUMN)) {
				actualColumn = null;
			} else if (qName.equals(XmlElem.C_KEY_COLUMNS)) {
			} else if (qName.equals(XmlElem.O_REFERECE_JOIN)) {
				actualRJ = null;
			} else if (qName.equals(XmlElem.O_REFERENCE)) {
				try {
					stkActualRef.pop();
				} catch (EmptyStackException e) {
					LogUtil.error("Invalid pdm file! Unbalanced Reference tags!");
					throw new SAXException("Invalid pdm file! Unbalanced Reference tags!");
				}
			} else if (qName.equals(XmlElem.O_SHORTCUT)) {
				actualShortcut = null;
			} else if (qName.equals(XmlElem.O_KEY)) {
				actualKey = null;
			} else if (qName.equals(XmlElem.C_COLUMNS)) {
			} else if (qName.equals(XmlElem.O_TABLE)) {
				actualTable = null;
			} else if (qName.equals(XmlElem.C_TABLES)) {
			} else if (qName.equals(XmlElem.O_MODEL)) {
			}
			try {
				stkActualXE.pop();
			} catch (EmptyStackException ese) {
				// do nothing
			}
		}

		/**
		 * @see org.xml.sax.ContentHandler#startDocument()
		 */
		public void startDocument() throws SAXException {
		}

		/**
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
		 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			try {
				prevActualXE = stkActualXE.peek();
			} catch (EmptyStackException e) {
				prevActualXE = null;
			}
			actualXE = null;
			String id = attributes.getValue("Id");
			String ref = attributes.getValue("Ref");

			// CASE ELEMENT

			if (qName.equals(XmlElem.A_NAME)) {
				// ________________________________________________________A_NAME
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_CODE)) {
				// ________________________________________________________A_CODE
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_DEFAULT)) {
				// _____________________________________________________A_DEFAULT
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_DESCRIPTION)) {
				// _________________________________________________A_DESCRIPTION
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_LISTOFVALS)) {
				// __________________________________________________A_LISTOFVALS
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_MANDATORY)) {
				// ___________________________________________________A_MANDATORY
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_COMMENT)) {
				// _____________________________________________________A_COMMENT
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_DATATYPE)) {
				// ____________________________________________________A_DATATYPE
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.A_LENGTH)) {
				// ______________________________________________________A_LENGTH
				actualXE = new XmlElem(qName, null, null);

			} else if (qName.equals(XmlElem.A_CARDINALITY)) {
				// ______________________________________________________A_LENGTH
				actualXE = new XmlElem(qName, null, null);
				
			} else if (qName.equals(XmlElem.A_PRECISION)) {
				// ___________________________________________________A_PRECISION
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.O_COLUMN)) {
				// ______________________________________________________O_COLUMN
				String colIdToAdd = null;
				if (id != null) {
					PDMColumn newCol = new PDMColumn();
					newCol.id = id;
					colIdToAdd = id;
					actualXE = new XmlElem(qName, id, newCol);
					htId_PDMObject.put(id, newCol);
					htId_XE.put(id, actualXE);
					actualColumn = newCol;
				} else if (ref != null) {
					colIdToAdd = ref;
				}
				if (colIdToAdd != null && prevActualXE != null) {
					XmlElem prevPrevActualXE = null;
					try {
						prevPrevActualXE = stkActualXE.underPeek();
					} catch (EmptyStackException ese) {
					}
					// Ok, detecting context of <o:column>
					if (prevActualXE.isCColumns()) {
						try {
							actualTable.addCol(colIdToAdd);
						} catch (NullPointerException npe) {
							LogUtil.error(npe.getMessage());
							throw new SAXException("PDML: Can't add column to " + "null table - actualTable == null");
						}
					} else if (prevActualXE.isCKeyColumns()) {
						if (actualKey == null) {
							LogUtil.error("Can't initialize actualKey - it is null!");
							throw new SAXException("PDML: Can't initialize actualKey - it is null!");
						}
						actualKey.addCol(colIdToAdd);
					} else if (prevActualXE.isCObject1()) {
						if (actualRJ == null || prevPrevActualXE == null || !prevPrevActualXE.isOReferenceJoin()) {
							LogUtil.error("actualRJ is null or this is not " + "a context of ReferenceJoin/Object1/Columns");
							throw new SAXException("PDML: actualRJ is null or this is not " + "a context of ReferenceJoin/Object1/Column");
						}
						actualRJ.fkParentColId = colIdToAdd;
					} else if (prevActualXE.isCObject2()) {
						if (actualRJ == null || prevPrevActualXE == null || !prevPrevActualXE.isOReferenceJoin()) {
							LogUtil.error("actualRJ is null or this is not " + "a context of ReferenceJoin/Object2/Column");
							throw new SAXException("PDML: actualRJ is null or this is not " + "a context of ReferenceJoin/Object2/Column");
						}
						actualRJ.colId = colIdToAdd;
					}
				}
			} else if (qName.equals(XmlElem.C_OBJECT_1)) {
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.C_CHILD_TABLE)) {
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.C_PARENT_TABLE)) {
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.C_OBJECT_1)) {
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.C_OBJECT_2)) {
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.C_KEY_COLUMNS)) {
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.C_REFERENCES)) {
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.O_REFERENCE)) {
				String RefIdToAdd = null;
				if (id != null) {
					actualRef = new PDMReference();
					actualRef.id = id;
					RefIdToAdd = id;
					htId_PDMObject.put(id, actualRef);
				} else if (ref != null) {
					RefIdToAdd = ref;
					actualRef = (PDMReference) htId_PDMObject.get(ref);
				}
				stkActualRef.push(actualRef);
				actualXE = new XmlElem(qName, id, actualRef);

				if (RefIdToAdd != null && prevActualXE != null && prevActualXE.isCReferences()) {
					try {
						alReferences.add(RefIdToAdd);
					} catch (Exception e) {
						// logger.error("alReference is null!");
						throw new SAXException("PDML: alReference is null!");
					}

				}

			} else if (qName.equals(XmlElem.O_REFERECE_JOIN)) {
				// _______________________________________________O_REFERECE_JOIN
				if (id != null) {
					// String refJoinIdToAdd = id;
					actualRJ = new ReferenceJoin();
					actualXE = new XmlElem(qName, id, actualRJ);
					htId_PDMObject.put(id, actualRJ);
					alJoins.add(actualRJ);
				} else {
					// logger.error("ReferenceJoin without Id");
					throw new SAXException("PDML: ReferenceJoin without Id");
				}

				actualRef = null;
				try {
					actualRef = stkActualRef.peek();
				} catch (EmptyStackException e) {
				}

				if (actualRef != null) {
					actualRef.addRefJoin(id);

				} else {
					// logger.error("ReferenceJoin without Reference");
					throw new SAXException("PDML: ReferenceJoin without Reference");
				}
			} else if (qName.equals(XmlElem.O_KEY)) {
				// _________________________________________________________O_KEY
				String keyIdToAdd = null;
				if (id != null) {
					actualKey = new PDMKey();
					actualKey.id = id;
					htId_PDMObject.put(id, actualKey);
					keyIdToAdd = id;
				} else if (ref != null) {
					keyIdToAdd = ref;
				}
				actualXE = new XmlElem(qName, null, null);
				if (keyIdToAdd != null && prevActualXE != null && prevActualXE.isCKeys()) {
					if (actualTable == null) {
						// logger
						// .error("c:Keys section must be declared within the "
						// + "o:Table section. id = " + keyIdToAdd);
						throw new SAXException("c:Keys section must be declared within the " + "o:Table section. id = " + keyIdToAdd);
					}
					actualTable.addKey(keyIdToAdd);
				}
			} else if (qName.equals(XmlElem.C_KEYS)) {
				// ________________________________________________________C_KEYS
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.C_COLUMNS)) {
				// _____________________________________________________C_COLUMNS
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.O_TABLE) || qName.equals(XmlElem.O_SHORTCUT)) {
				// _______________________________________________________O_TABLE
				String tableIdToAdd = null;
				if (id != null) {
					tableIdToAdd = id;
					if (!qName.equals(XmlElem.O_SHORTCUT)) {
						PDMTable pdmTableToAdd = new PDMTable();
						pdmTableToAdd.id = id;
						actualXE = new XmlElem(qName, id, pdmTableToAdd);
						htId_XE.put(id, actualXE);
						actualTable = pdmTableToAdd;
						htId_PDMObject.put(id, actualTable);
					} else {
						actualShortcut = new PDMShortcut();
						actualShortcut.id = id;
						actualXE = new XmlElem(qName, id, actualShortcut);
						htId_XE.put(id, actualXE);
					}
				} else if (ref != null) {
					tableIdToAdd = ref;
					actualXE = htId_XE.get(ref);
				}
				if (tableIdToAdd != null && prevActualXE != null) {
					XmlElem prevPrevActualXE = null;
					try {
						prevPrevActualXE = stkActualXE.underPeek();
					} catch (EmptyStackException ese) {
					}
					try {
						actualRef = stkActualRef.peek();
					} catch (EmptyStackException ese) {
					}
					// Detecting context of <o:Table>
					if (prevActualXE.isCTables() && !qName.equals(XmlElem.O_SHORTCUT)) {
						try {
							actualMModel.addTable(tableIdToAdd);
						} catch (NullPointerException npe) {
							// logger.error(npe.getMessage());
							throw new SAXException("PDML: Can't add table to " + "null MetaModel - actualMModel == null");
						}
					} else if (prevActualXE.isCParentTable()) {
						if (actualRef == null || prevPrevActualXE == null || !prevPrevActualXE.isOReference()) {
							// logger.error("actualRef is null or this is not "
							// + "a context of Reference/Object1/Table");
							throw new SAXException("PDML: actualRef is null or this is not " + "a context of Reference/Object1/Table");
						}
						actualRef.parentTableId = tableIdToAdd;
					} else if (prevActualXE.isCChildTable()) {
						if (actualRef == null || prevPrevActualXE == null || !prevPrevActualXE.isOReference()) {
							// logger
							// .error("actualRef is null or this is not "
							// + "a context of Reference/Object2/Table. RefId="
							// + tableIdToAdd);
							throw new SAXException("PDML: actualRef is null or this is not " + "a context of Reference/Object2/Table. RefId=" + tableIdToAdd);
						}
						actualRef.sourceTableId = tableIdToAdd;
					}
				}
			} else if (qName.equals(XmlElem.C_TABLES)) {
				// ______________________________________________________C_TABLES
				actualXE = new XmlElem(qName, null, null);
			} else if (qName.equals(XmlElem.O_MODEL)) {
				// _______________________________________________________O_MODEL
				if (id != null && actualMModel == null) {
					actualMModel = new PDMModel();
					actualMModel.id = id;
					actualXE = new XmlElem(qName, id, actualMModel);
					htId_XE.put(id, actualXE);

				}
			}
			stkActualXE.push(actualXE);
		}
	}

	private final static int INIT_HT_CAP = 500;

	public static final String FILENAME = "file";

	protected PDMModel actualMModel = null;

	private ArrayList<ReferenceJoin> alJoins = new ArrayList<ReferenceJoin>();

	private ArrayList<String> alReferences = new ArrayList<String>();

	protected Hashtable<String, MetaColumn> htCreatedMetaColumns_ID_MC = new Hashtable<String, MetaColumn>();

	protected Hashtable<String, MetaReference> htCreatedRefs = new Hashtable<String, MetaReference>();

	protected Hashtable<String, MetaTable> htCreatedTables = new Hashtable<String, MetaTable>();

	protected Hashtable<String, MetaTable> htCreatedTablesByCode = new Hashtable<String, MetaTable>();

	private final Hashtable<String, PDMElement> htId_PDMObject = new Hashtable<String, PDMElement>(5000);

	protected Hashtable<String, PDMShortcut> htShortcuts = new Hashtable<String, PDMShortcut>();

	public PDMetaLoader() {
		super();
	}

	public MetaColumn getMetaColumnByPDMColId(String aPDMColId) {
		MetaColumn retVal = null;
		try {
			retVal = (MetaColumn) htCreatedMetaColumns_ID_MC.get(aPDMColId);
		} catch (Exception e) {
			retVal = null;
		}
		return retVal;
	}

	/**
	 * @param Configuration
	 *            properties of PDM. These are the mandatory properties:
	 *            <p>
	 *            <code><ul>
	 *            		<li>file</li>
	 *            	</ul></code>
	 *            </p>
	 *            .
	 * 
	 * @see com.github.geequery.codegen.pdm.IMetaLoader#getMetaModel(java.util.Properties)
	 */
	public MetaModel getMetaModel(File file,String... options) throws JefException {
		MetaModel retVal = null;
		LogUtil.show("Retrieving model from the file: " + file);

		if (!file.canRead())
			throw new JefException("File is locked or it doesn't exist");
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			// logger.info("MODEL PARSING STARTED...");
			InputSource is = new InputSource(new XmlFixedReader(IOUtils.getReader(file, "UTF-8")));
			saxParser.parse(is, new XmlHandlerDelegate());
			// resolve all PDMColumn fks
			this.resolveAllPDMColFks();
			// resolve all PDMReferences
			resolveAllPDMReferences();
			retVal = actualMModel.createMetaModel(htId_PDMObject);
			// resolve all MetaColumn fks
			this.resolveAllMetaColFks();
			resolveAllMetaReferences();
			htCreatedMetaColumns_ID_MC.clear();
			htCreatedRefs.clear();
			// logger.info("DONE.");
		} catch (FileNotFoundException fnfe) {
			throw new JefException(fnfe);
		} catch (IOException ioe) {
			throw new JefException(ioe);
		} catch (SAXException saxe) {
			throw new JefException(saxe);
		} catch (ParserConfigurationException pce) {
			throw new JefException(pce);
		}
		return retVal;
	}

	private void resolveAllMetaColFks() throws SAXException {
		Enumeration<String> eColIds = htCreatedMetaColumns_ID_MC.keys();
		while (eColIds.hasMoreElements()) {
			String crntColId = eColIds.nextElement();
			try {
				PDMColumn pdmCol = (PDMColumn) htId_PDMObject.get(crntColId);
				MetaColumn metaCol = pdmCol.getCreateMetaColumn();
				MetaColumn fkMetaCol = pdmCol.fkParent.getCreateMetaColumn();
				metaCol.setFkColParent(fkMetaCol);
			} catch (Exception e) {
			}
		}
	}

	private void resolveAllMetaReferences() throws SAXException {
		Enumeration<String> eRefs = htCreatedRefs.keys();
		while (eRefs.hasMoreElements()) {
			String refId = (String) eRefs.nextElement();
			try {
				PDMReference currRef = (PDMReference) htId_PDMObject.get(refId);

				MetaReference currMRef = (MetaReference) htCreatedRefs.get(refId);
				if (currMRef == null) {
					throw new SAXException("Reference not created!. RefId = " + refId);
				}
				MetaTable srcTable = (MetaTable) htCreatedTables.get(currRef.sourceTableId);
				if (srcTable == null) {
					String srcTableCode = ((PDMShortcut) htShortcuts.get(currRef.sourceTableId)).code;
					srcTable = (MetaTable) htCreatedTablesByCode.get(srcTableCode);
				}
				MetaTable parentTable = (MetaTable) htCreatedTables.get(currRef.parentTableId);
				if (parentTable == null) {
					String parentTableCode = ((PDMShortcut) htShortcuts.get(currRef.parentTableId)).code;
					parentTable = (MetaTable) htCreatedTablesByCode.get(parentTableCode);
				}
				if (srcTable == null || parentTable == null) {
					throw new SAXException("Invalid reference! Source or parent table is null. RefId = " + refId);
				}
				currMRef.setSourceTable(srcTable);
				currMRef.setParentTable(parentTable);
				parentTable.addExportRef(currMRef);//添加
				Iterator<String> joins = currRef.alRefJoins.iterator();
				while (joins.hasNext()) {
					String joinId = (String) joins.next();
					ReferenceJoin join = (ReferenceJoin) htId_PDMObject.get(joinId);
					currMRef.addColumn(getMetaColumnByPDMColId(join.colId),getMetaColumnByPDMColId(join.fkParentColId));
				}
			} catch (Exception e) {
				// logger.error(e.getMessage());
				throw new SAXException(e.getMessage());
			}
		}
	}

	private void resolveAllPDMColFks() throws SAXException {
		Iterator<ReferenceJoin> iRJoins = alJoins.iterator();
		while (iRJoins.hasNext()) {
			ReferenceJoin rj = (ReferenceJoin) iRJoins.next();
			rj.join(htId_PDMObject);
		}
	}

	private void resolveAllPDMReferences() throws SAXException {
		Iterator<String> itRefs = alReferences.iterator();
		while (itRefs.hasNext()) {
			String currPDMRefId = (String) itRefs.next();
			PDMReference currPDMRef = (PDMReference) htId_PDMObject.get(currPDMRefId);
			PDMTable srcTbl = (PDMTable) htId_PDMObject.get(currPDMRef.sourceTableId);
			if (srcTbl != null) {
				srcTbl.addRef(currPDMRef.id);
			} else {
				 LogUtil.error("Invalid reference! Source table is null. RefId = "
				 + currPDMRefId);
				 itRefs.remove();
			}
		}
	}
}
