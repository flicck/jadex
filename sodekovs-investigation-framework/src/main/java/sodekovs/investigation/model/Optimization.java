//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.10.02 at 12:57:25 PM CEST 
//


package sodekovs.investigation.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
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
 *         &lt;element ref="{}Data"/>
 *         &lt;element ref="{}ParameterSweeping"/>
 *         &lt;element ref="{}Algorithm"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "data",
    "parameterSweeping",
    "algorithm"
})
@XmlRootElement(name = "Optimization")
public class Optimization {

    @XmlElement(name = "Data", required = true)
    protected List<Data> data;
    @XmlElement(name = "ParameterSweeping", required = true)
    protected ParameterSweeping parameterSweeping;
    @XmlElement(name = "Algorithm", required = true)
    protected Algorithm algorithm;

    /**
     * Gets the value of the data property.
     * 
     * @return
     *     possible object is
     *     {@link Data }
     *     
     */
    public List<Data> getData() {
		return data;
		}

    /**
     * Sets the value of the data property.
     * 
     * @param value
     *     allowed object is
     *     {@link Data }
     *     
     */
    public void setData(List<Data> data) {
		this.data = data;
		}

    /**
     * Gets the value of the parameterSweeping property.
     * 
     * @return
     *     possible object is
     *     {@link ParameterSweeping }
     *     
     */
    public ParameterSweeping getParameterSweeping() {
        return parameterSweeping;
    }

    /**
     * Sets the value of the parameterSweeping property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParameterSweeping }
     *     
     */
    public void setParameterSweeping(ParameterSweeping value) {
        this.parameterSweeping = value;
    }

    /**
     * Gets the value of the algorithm property.
     * 
     * @return
     *     possible object is
     *     {@link Algorithm }
     *     
     */
    public Algorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets the value of the algorithm property.
     * 
     * @param value
     *     allowed object is
     *     {@link Algorithm }
     *     
     */
    public void setAlgorithm(Algorithm value) {
        this.algorithm = value;
    }

}
