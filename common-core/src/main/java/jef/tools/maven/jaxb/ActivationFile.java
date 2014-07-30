package jef.tools.maven.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *         This is the file specification used to activate the profile. The missing value will be the location
 *         of a file that needs to exist, and if it doesn't the profile will be activated.  On the other hand exists will test
 *         for the existence of the file and if it is there the profile will be activated.
 *       
 * 
 * <p>Java class for ActivationFile complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ActivationFile">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="missing" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="exists" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ActivationFile", propOrder = {

})
public class ActivationFile {

    protected String missing;
    protected String exists;

    /**
     * Gets the value of the missing property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMissing() {
        return missing;
    }

    /**
     * Sets the value of the missing property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMissing(String value) {
        this.missing = value;
    }

    /**
     * Gets the value of the exists property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getExists() {
        return exists;
    }

    /**
     * Sets the value of the exists property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setExists(String value) {
        this.exists = value;
    }

}
