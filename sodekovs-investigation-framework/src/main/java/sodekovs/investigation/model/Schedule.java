//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.10.02 at 03:13:06 PM CEST 
//


package sodekovs.investigation.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


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
 *         &lt;element ref="{}TerminateCondition"/>
 *         &lt;element name="SytemUnderTest">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{}Properties"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Sequences">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{}Sequence" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="scaleFactor" type="{http://www.w3.org/2001/XMLSchema}double" default="1.0" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="BenchmarkingEvaluation">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{}Dataproviders"/>
 *                   &lt;element ref="{}Dataconsumers"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element ref="{}AdaptationAnalysis"/>
 *       &lt;/sequence>
 *       &lt;attribute name="type" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NMTOKEN">
 *             &lt;enumeration value="Workload"/>
 *             &lt;enumeration value="Faultload"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="warmUpTime" type="{http://www.w3.org/2001/XMLSchema}long" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "terminateCondition",
    "sytemUnderTest",
    "sequences",
    "benchmarkingEvaluation",
    "adaptationAnalysis"
})
@XmlRootElement(name = "Schedule")
public class Schedule {

    @XmlElement(name = "TerminateCondition", required = true)
    protected TerminateCondition terminateCondition;
    @XmlElement(name = "SytemUnderTest", required = true)
    protected Schedule.SytemUnderTest sytemUnderTest;
    @XmlElement(name = "Sequences", required = true)
    protected Schedule.Sequences sequences;
    @XmlElement(name = "BenchmarkingEvaluation", required = true)
    protected Schedule.BenchmarkingEvaluation benchmarkingEvaluation;
    @XmlElement(name = "AdaptationAnalysis", required = true)
    protected AdaptationAnalysis adaptationAnalysis;
    @XmlAttribute(name = "type", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String type;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "warmUpTime")
    protected Long warmUpTime;

    /**
     * Gets the value of the terminateCondition property.
     * 
     * @return
     *     possible object is
     *     {@link TerminateCondition }
     *     
     */
    public TerminateCondition getTerminateCondition() {
        return terminateCondition;
    }

    /**
     * Sets the value of the terminateCondition property.
     * 
     * @param value
     *     allowed object is
     *     {@link TerminateCondition }
     *     
     */
    public void setTerminateCondition(TerminateCondition value) {
        this.terminateCondition = value;
    }

    /**
     * Gets the value of the sytemUnderTest property.
     * 
     * @return
     *     possible object is
     *     {@link Schedule.SytemUnderTest }
     *     
     */
    public Schedule.SytemUnderTest getSytemUnderTest() {
        return sytemUnderTest;
    }

    /**
     * Sets the value of the sytemUnderTest property.
     * 
     * @param value
     *     allowed object is
     *     {@link Schedule.SytemUnderTest }
     *     
     */
    public void setSytemUnderTest(Schedule.SytemUnderTest value) {
        this.sytemUnderTest = value;
    }

    /**
     * Gets the value of the sequences property.
     * 
     * @return
     *     possible object is
     *     {@link Schedule.Sequences }
     *     
     */
    public Schedule.Sequences getSequences() {
        return sequences;
    }

    /**
     * Sets the value of the sequences property.
     * 
     * @param value
     *     allowed object is
     *     {@link Schedule.Sequences }
     *     
     */
    public void setSequences(Schedule.Sequences value) {
        this.sequences = value;
    }

    /**
     * Gets the value of the benchmarkingEvaluation property.
     * 
     * @return
     *     possible object is
     *     {@link Schedule.BenchmarkingEvaluation }
     *     
     */
    public Schedule.BenchmarkingEvaluation getBenchmarkingEvaluation() {
        return benchmarkingEvaluation;
    }

    /**
     * Sets the value of the benchmarkingEvaluation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Schedule.BenchmarkingEvaluation }
     *     
     */
    public void setBenchmarkingEvaluation(Schedule.BenchmarkingEvaluation value) {
        this.benchmarkingEvaluation = value;
    }

    /**
     * Gets the value of the adaptationAnalysis property.
     * 
     * @return
     *     possible object is
     *     {@link AdaptationAnalysis }
     *     
     */
    public AdaptationAnalysis getAdaptationAnalysis() {
        return adaptationAnalysis;
    }

    /**
     * Sets the value of the adaptationAnalysis property.
     * 
     * @param value
     *     allowed object is
     *     {@link AdaptationAnalysis }
     *     
     */
    public void setAdaptationAnalysis(AdaptationAnalysis value) {
        this.adaptationAnalysis = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
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
     * Gets the value of the warmUpTime property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getWarmUpTime() {
        return warmUpTime;
    }

    /**
     * Sets the value of the warmUpTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setWarmUpTime(Long value) {
        this.warmUpTime = value;
    }


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
     *         &lt;element ref="{}Dataproviders"/>
     *         &lt;element ref="{}Dataconsumers"/>
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
        "dataproviders",
        "dataconsumers"
    })
    public static class BenchmarkingEvaluation {

        @XmlElement(name = "Dataproviders", required = true)
        protected Dataproviders dataproviders;
        @XmlElement(name = "Dataconsumers", required = true)
        protected Dataconsumers dataconsumers;

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

    }


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
     *         &lt;element ref="{}Sequence" maxOccurs="unbounded"/>
     *       &lt;/sequence>
     *       &lt;attribute name="scaleFactor" type="{http://www.w3.org/2001/XMLSchema}double" default="1.0" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "sequence"
    })
    public static class Sequences {

        @XmlElement(name = "Sequence", required = true)
        protected List<Sequence> sequence;
        @XmlAttribute(name = "scaleFactor")
        protected Double scaleFactor;

        /**
         * Gets the value of the sequence property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the sequence property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getSequence().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link Sequence }
         * 
         * 
         */
        public List<Sequence> getSequence() {
            if (sequence == null) {
                sequence = new ArrayList<Sequence>();
            }
            return this.sequence;
        }

        /**
         * Gets the value of the scaleFactor property.
         * 
         * @return
         *     possible object is
         *     {@link Double }
         *     
         */
        public double getScaleFactor() {
            if (scaleFactor == null) {
                return  1.0D;
            } else {
                return scaleFactor;
            }
        }

        /**
         * Sets the value of the scaleFactor property.
         * 
         * @param value
         *     allowed object is
         *     {@link Double }
         *     
         */
        public void setScaleFactor(Double value) {
            this.scaleFactor = value;
        }

    }


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
     *         &lt;element ref="{}Properties"/>
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
        "properties"
    })
    public static class SytemUnderTest {

        @XmlElement(name = "Properties", required = true)
        protected Properties properties;

        /**
         * Gets the value of the properties property.
         * 
         * @return
         *     possible object is
         *     {@link Properties }
         *     
         */
        public Properties getProperties() {
            return properties;
        }

        /**
         * Sets the value of the properties property.
         * 
         * @param value
         *     allowed object is
         *     {@link Properties }
         *     
         */
        public void setProperties(Properties value) {
            this.properties = value;
        }

    }

}
