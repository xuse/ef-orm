package jef.database.meta.object;


/**
 * The type of database constraints.
 * @author jiyi
 *
 */
public enum ConstraintType {
	C,  //Check on a table  Column
	O,  //Read Only on a view  
	P,  //Primary Key
	R,  //Referential AKA Foreign Key
	U,  //Unique Key
	V  //Check Option on a view
}
