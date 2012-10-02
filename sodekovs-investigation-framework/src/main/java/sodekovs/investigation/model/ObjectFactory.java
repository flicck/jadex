//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.10.02 at 03:13:06 PM CEST 
//


package sodekovs.investigation.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the sodekovs.investigation.model package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Input_QNAME = new QName("", "Input");
    private final static QName _Import_QNAME = new QName("", "Import");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: sodekovs.investigation.model
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Schedule }
     * 
     */
    public Schedule createSchedule() {
        return new Schedule();
    }

    /**
     * Create an instance of {@link Sequence }
     * 
     */
    public Sequence createSequence() {
        return new Sequence();
    }

    /**
     * Create an instance of {@link BenchmarkingSetup }
     * 
     */
    public BenchmarkingSetup createBenchmarkingSetup() {
        return new BenchmarkingSetup();
    }

    /**
     * Create an instance of {@link TerminateCondition }
     * 
     */
    public TerminateCondition createTerminateCondition() {
        return new TerminateCondition();
    }

    /**
     * Create an instance of {@link TerminationTime }
     * 
     */
    public TerminationTime createTerminationTime() {
        return new TerminationTime();
    }

    /**
     * Create an instance of {@link SemanticCondition }
     * 
     */
    public SemanticCondition createSemanticCondition() {
        return new SemanticCondition();
    }

    /**
     * Create an instance of {@link ObjectSource }
     * 
     */
    public ObjectSource createObjectSource() {
        return new ObjectSource();
    }

    /**
     * Create an instance of {@link Schedule.SytemUnderTest }
     * 
     */
    public Schedule.SytemUnderTest createScheduleSytemUnderTest() {
        return new Schedule.SytemUnderTest();
    }

    /**
     * Create an instance of {@link Schedule.Sequences }
     * 
     */
    public Schedule.Sequences createScheduleSequences() {
        return new Schedule.Sequences();
    }

    /**
     * Create an instance of {@link Schedule.BenchmarkingEvaluation }
     * 
     */
    public Schedule.BenchmarkingEvaluation createScheduleBenchmarkingEvaluation() {
        return new Schedule.BenchmarkingEvaluation();
    }

    /**
     * Create an instance of {@link AdaptationAnalysis }
     * 
     */
    public AdaptationAnalysis createAdaptationAnalysis() {
        return new AdaptationAnalysis();
    }

    /**
     * Create an instance of {@link TargetDefinition }
     * 
     */
    public TargetDefinition createTargetDefinition() {
        return new TargetDefinition();
    }

    /**
     * Create an instance of {@link IncidentEvent }
     * 
     */
    public IncidentEvent createIncidentEvent() {
        return new IncidentEvent();
    }

    /**
     * Create an instance of {@link AdaptationEvent }
     * 
     */
    public AdaptationEvent createAdaptationEvent() {
        return new AdaptationEvent();
    }

    /**
     * Create an instance of {@link SystemReadyEvent }
     * 
     */
    public SystemReadyEvent createSystemReadyEvent() {
        return new SystemReadyEvent();
    }

    /**
     * Create an instance of {@link Rows }
     * 
     */
    public Rows createRows() {
        return new Rows();
    }

    /**
     * Create an instance of {@link Dataproviders }
     * 
     */
    public Dataproviders createDataproviders() {
        return new Dataproviders();
    }

    /**
     * Create an instance of {@link Dataprovider }
     * 
     */
    public Dataprovider createDataprovider() {
        return new Dataprovider();
    }

    /**
     * Create an instance of {@link Source }
     * 
     */
    public Source createSource() {
        return new Source();
    }

    /**
     * Create an instance of {@link Data }
     * 
     */
    public Data createData() {
        return new Data();
    }

    /**
     * Create an instance of {@link ElementSource }
     * 
     */
    public ElementSource createElementSource() {
        return new ElementSource();
    }

    /**
     * Create an instance of {@link Persist }
     * 
     */
    public Persist createPersist() {
        return new Persist();
    }

    /**
     * Create an instance of {@link Property }
     * 
     */
    public Property createProperty() {
        return new Property();
    }

    /**
     * Create an instance of {@link Properties }
     * 
     */
    public Properties createProperties() {
        return new Properties();
    }

    /**
     * Create an instance of {@link InvestigationConfiguration }
     * 
     */
    public InvestigationConfiguration createInvestigationConfiguration() {
        return new InvestigationConfiguration();
    }

    /**
     * Create an instance of {@link Imports }
     * 
     */
    public Imports createImports() {
        return new Imports();
    }

    /**
     * Create an instance of {@link Dataconsumers }
     * 
     */
    public Dataconsumers createDataconsumers() {
        return new Dataconsumers();
    }

    /**
     * Create an instance of {@link Dataconsumer }
     * 
     */
    public Dataconsumer createDataconsumer() {
        return new Dataconsumer();
    }

    /**
     * Create an instance of {@link MixedProperty }
     * 
     */
    public MixedProperty createMixedProperty() {
        return new MixedProperty();
    }

    /**
     * Create an instance of {@link DataVisualization }
     * 
     */
    public DataVisualization createDataVisualization() {
        return new DataVisualization();
    }

    /**
     * Create an instance of {@link Function }
     * 
     */
    public Function createFunction() {
        return new Function();
    }

    /**
     * Create an instance of {@link Optimization }
     * 
     */
    public Optimization createOptimization() {
        return new Optimization();
    }

    /**
     * Create an instance of {@link ParameterSweeping }
     * 
     */
    public ParameterSweeping createParameterSweeping() {
        return new ParameterSweeping();
    }

    /**
     * Create an instance of {@link Configuration }
     * 
     */
    public Configuration createConfiguration() {
        return new Configuration();
    }

    /**
     * Create an instance of {@link Algorithm }
     * 
     */
    public Algorithm createAlgorithm() {
        return new Algorithm();
    }

    /**
     * Create an instance of {@link RunConfiguration }
     * 
     */
    public RunConfiguration createRunConfiguration() {
        return new RunConfiguration();
    }

    /**
     * Create an instance of {@link General }
     * 
     */
    public General createGeneral() {
        return new General();
    }

    /**
     * Create an instance of {@link StartTime }
     * 
     */
    public StartTime createStartTime() {
        return new StartTime();
    }

    /**
     * Create an instance of {@link Action }
     * 
     */
    public Action createAction() {
        return new Action();
    }

    /**
     * Create an instance of {@link Sequence.RepeatConfiguration }
     * 
     */
    public Sequence.RepeatConfiguration createSequenceRepeatConfiguration() {
        return new Sequence.RepeatConfiguration();
    }

    /**
     * Create an instance of {@link Sequence.Actions }
     * 
     */
    public Sequence.Actions createSequenceActions() {
        return new Sequence.Actions();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "Input")
    public JAXBElement<String> createInput(String value) {
        return new JAXBElement<String>(_Input_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "Import")
    public JAXBElement<String> createImport(String value) {
        return new JAXBElement<String>(_Import_QNAME, String.class, null, value);
    }

}
