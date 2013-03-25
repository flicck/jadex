//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.07.23 at 01:45:23 PM MESZ 
//


package jadex.simulation.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}Imports"/>
 *         &lt;element ref="{}Dataproviders"/>
 *         &lt;element ref="{}Dataconsumers"/>
 *         &lt;element ref="{}DataVisualization"/>
 *         &lt;element ref="{}Persist"/>
 *         &lt;element ref="{}Optimization"/>
 *         &lt;element ref="{}RunConfiguration"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="applicationReference" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="applicationConfiguration" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "imports",
    "dataproviders",
    "dataconsumers",
    "dataVisualization",
    "persist",
    "optimization",
    "runConfiguration"
})
@XmlRootElement(name = "SimulationConfiguration")
public class SimulationConfiguration  implements Serializable{

    /**
	 * 
	 */
//	private static final long serialVersionUID = 488853484316385569L;
	@XmlElement(name = "Imports", required = true)
    protected Imports imports;
    @XmlElement(name = "Dataproviders", required = true)
    protected Dataproviders dataproviders;
    @XmlElement(name = "Dataconsumers", required = true)
    protected Dataconsumers dataconsumers;
    @XmlElement(name = "DataVisualization", required = true)
    protected DataVisualization dataVisualization;
    @XmlElement(name = "Persist", required = true)
    protected Persist persist;
    @XmlElement(name = "Optimization", required = true)
    protected Optimization optimization;
    @XmlElement(name = "RunConfiguration", required = true)
    protected RunConfiguration runConfiguration;
    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute(required = true)
    protected String applicationReference;
    @XmlAttribute(required = true)
    protected String applicationConfiguration;
    @XmlAttribute(required = true)
    protected String nameOfSpace;
    @XmlAttribute(required = false)
    protected String description;


    /**
     * Gets the value of the imports property.
     * 
     * @return
     *     possible object is
     *     {@link Imports }
     *     
     */
    public Imports getImports() {
        return imports;
    }

    /**
     * Sets the value of the imports property.
     * 
     * @param value
     *     allowed object is
     *     {@link Imports }
     *     
     */
    public void setImports(Imports value) {
        this.imports = value;
    }

    /**
     * Gets the value of the dataproviders property.
     * 
     * @return
     *     possible object is
     *     {@link Dataproviders }
     *     
     */
    public Dataproviders getDataproviders() {
        return dataproviders;
    }

    /**
     * Sets the value of the dataproviders property.
     * 
     * @param value
     *     allowed object is
     *     {@link Dataproviders }
     *     
     */
    public void setDataproviders(Dataproviders value) {
        this.dataproviders = value;
    }

    /**
     * Gets the value of the dataconsumers property.
     * 
     * @return
     *     possible object is
     *     {@link Dataconsumers }
     *     
     */
    public Dataconsumers getDataconsumers() {
        return dataconsumers;
    }

    /**
     * Sets the value of the dataconsumers property.
     * 
     * @param value
     *     allowed object is
     *     {@link Dataconsumers }
     *     
     */
    public void setDataconsumers(Dataconsumers value) {
        this.dataconsumers = value;
    }

    /**
     * Gets the value of the dataVisualization property.
     * 
     * @return
     *     possible object is
     *     {@link DataVisualization }
     *     
     */
    public DataVisualization getDataVisualization() {
        return dataVisualization;
    }

    /**
     * Sets the value of the dataVisualization property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataVisualization }
     *     
     */
    public void setDataVisualization(DataVisualization value) {
        this.dataVisualization = value;
    }

    /**
     * Gets the value of the persist property.
     * 
     * @return
     *     possible object is
     *     {@link Persist }
     *     
     */
    public Persist getPersist() {
        return persist;
    }

    /**
     * Sets the value of the persist property.
     * 
     * @param value
     *     allowed object is
     *     {@link Persist }
     *     
     */
    public void setPersist(Persist value) {
        this.persist = value;
    }

    /**
     * Gets the value of the optimization property.
     * 
     * @return
     *     possible object is
     *     {@link Optimization }
     *     
     */
    public Optimization getOptimization() {
        return optimization;
    }

    /**
     * Sets the value of the optimization property.
     * 
     * @param value
     *     allowed object is
     *     {@link Optimization }
     *     
     */
    public void setOptimization(Optimization value) {
        this.optimization = value;
    }

    /**
     * Gets the value of the runConfiguration property.
     * 
     * @return
     *     possible object is
     *     {@link RunConfiguration }
     *     
     */
    public RunConfiguration getRunConfiguration() {
        return runConfiguration;
    }

    /**
     * Sets the value of the runConfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link RunConfiguration }
     *     
     */
    public void setRunConfiguration(RunConfiguration value) {
        this.runConfiguration = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the applicationReference property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getApplicationReference() {
        return applicationReference;
    }

    /**
     * Sets the value of the applicationReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setApplicationReference(String value) {
        this.applicationReference = value;
    }

    /**
     * Gets the value of the applicationConfiguration property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getApplicationConfiguration() {
        return applicationConfiguration;
    }

    /**
     * Sets the value of the applicationConfiguration property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setApplicationConfiguration(String value) {
        this.applicationConfiguration = value;
    }
    
    /**
     * Gets the value of the nameOfSpace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNameOfSpace() {
        return nameOfSpace;
    }

    /**
     * Sets the value of the nameOfSpace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNameOfSpace(String value) {
        this.nameOfSpace = value;
    }
      
    
    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }
    
    
    
    

}
