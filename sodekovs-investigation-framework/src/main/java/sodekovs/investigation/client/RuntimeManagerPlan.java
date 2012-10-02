package sodekovs.investigation.client;

import jadex.bdi.runtime.Plan;
import jadex.bdi.runtime.impl.flyweights.ElementFlyweight;
import jadex.bdi.runtime.interpreter.OAVBDIFetcher;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.SReflect;
import jadex.commons.future.IFuture;
import jadex.extension.envsupport.MEnvSpaceType;
import jadex.extension.envsupport.environment.AbstractEnvironmentSpace;
import jadex.extension.envsupport.evaluation.AbstractChartDataConsumer;
import jadex.extension.envsupport.evaluation.DefaultDataProvider;
import jadex.extension.envsupport.evaluation.IObjectSource;
import jadex.extension.envsupport.evaluation.ITableDataConsumer;
import jadex.extension.envsupport.evaluation.ITableDataProvider;
import jadex.extension.envsupport.evaluation.SpaceObjectSource;
import jadex.javaparser.IExpressionParser;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.javaccimpl.JavaCCExpressionParser;
import jadex.rules.state.IOAVState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sodekovs.investigation.controlcenter.OnlineVisualisation;
import sodekovs.investigation.evaluation.SimulationDataConsumer;
import sodekovs.investigation.helper.Constants;
import sodekovs.investigation.model.Data;
import sodekovs.investigation.model.Dataconsumer;
import sodekovs.investigation.model.Dataprovider;
import sodekovs.investigation.model.InvestigationConfiguration;
import sodekovs.investigation.model.ObservedEvent;
import sodekovs.investigation.model.SemanticCondition;
import sodekovs.investigation.model.Source;
import sodekovs.investigation.model.TerminationTime;
import sodekovs.util.misc.AgentMethods;
import sodekovs.util.misc.EvaluateExpression;
import sodekovs.util.misc.FileHandler;
import sodekovs.util.misc.GlobalConstants;
import sodekovs.util.misc.TimeConverter;
import sodekovs.util.misc.XMLHandler;

public class RuntimeManagerPlan extends Plan {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6999042052110256441L;

	/**
	 * External access of the application that is executed and observed by this agent
	 */
	private IExternalAccess exta = null;
//	private IClockService clockservice = (IClockService) SServiceProvider.getService(getScope().getServiceContainer(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(this);										
	private IClockService clockservice = null;//(IClockService) getScope().getServiceContainer().getRequiredService("clockservice").get(this);
	private IComponentManagementService cms = null;
	private OnlineVisualisation vis = null;
	private int localExperimentCounter = -1;	
	private long callerID = -1;
	private String directoryPath = null;

	public void body() {
		// Increment the number of currently running experiments on this agent
		numberOfRunningExperiments(1);

		init();

		// Get local id for this experiment to be conducted and increment counter
		localExperimentCounter = (Integer) getBeliefbase().getBelief("experimentCounter").getFact();
		getBeliefbase().getBelief("experimentCounter").setFact(localExperimentCounter+1);
			
		//init mapping between calling service and the executed experiment. needed in order to be able to execute experiments in parallel.
		HashMap<Long,Integer>callerExperimentReference = (HashMap<Long,Integer>)getBeliefbase().getBelief("callerExperimentReference").getFact();
		callerExperimentReference.put(callerID, localExperimentCounter);
		getBeliefbase().getBelief("callerExperimentReference").setFact(callerExperimentReference);

		HashMap<String, Object> clientConfMap = (HashMap<String, Object>) getParameter("clientConf").getValue();
		
		// extract and persist *.configuration.xml and *.application.xml
		InvestigationConfiguration investigationConf = prepareXMLFiles(clientConfMap);

		// SimulationConfiguration simConf = (SimulationConfiguration) XMLHandler
		// .parseXMLFromString((String) clientConfMap.get(Constants.CONFIGURATION_FILE_AS_XML_STRING), SimulationConfiguration.class);
//		cms = (IComponentManagementService) SServiceProvider.getService(getScope().getServiceContainer(), IComponentManagementService.class,RequiredServiceInfo.SCOPE_PLATFORM).get(this);
		cms = (IComponentManagementService) getScope().getServiceContainer().getRequiredService("cms").get(this);
				
		startApplication((Map) getParameter("applicationConf").getValue(), clientConfMap, investigationConf);
		System.out.println("#RumtimeManagerPlan- InvestigationFramework# Startet Simulation Experiment Nr.:" + clientConfMap.get(GlobalConstants.EXPERIMENT_ID) + ") with Optimization Values: "
				+ clientConfMap.get(GlobalConstants.CURRENT_PARAMETER_CONFIGURATION));
		System.out.println("Number of Exp at this agent: " + (Integer) getBeliefbase().getBelief("numberOfRunningExperiments").getFact());

		IFuture fut = exta.getExtension(investigationConf.getNameOfSpace());
		AbstractEnvironmentSpace space = (AbstractEnvironmentSpace) fut.get(this);

		// Determine terminate condition
		// Time determines termination
		if (investigationConf.getRunConfiguration().getRows().getTerminateCondition().getTerminationTime() != null) {

//			Time time = investigationConf.getRunConfiguration().getRows().getTerminateCondition().getTime();
			TerminationTime time = investigationConf.getRunConfiguration().getRows().getTerminateCondition().getTerminationTime();
			Long terminationTime = new Long(-1);

			if (time.getType().equals(Constants.RELATIVE_TIME_EXPRESSION)) {
				terminationTime = getTerminationTime(0, time.getValue());
			} else if (time.getType().equals(Constants.ABSOLUTE_TIME_EXPRESSION)) {
				terminationTime = getTerminationTime(1, time.getValue());
			} else {
				System.err.println("#RunTimeManagerPlan# Time type missing " + investigationConf);
			}
			if (terminationTime.longValue() > -1) {
				waitFor(terminationTime);
			} else {
				System.err.println("#RunTimeManagerPlan# Error on setting termination time " + investigationConf);
			}
			// Application semantic determines termination, e.g.
			// $homebase.NumberOfOre > 100
		} else if (investigationConf.getRunConfiguration().getRows().getTerminateCondition().getSemanticCondition() != null) {

			SemanticCondition semanticCond = investigationConf.getRunConfiguration().getRows().getTerminateCondition().getSemanticCondition();

			// HACK: Need a observer / listener instead evaluating expression every 1000ms
			while (true) {
				waitFor(1000);
								
				// Hack: Works right now only for single objects but not for all
				// of that type...
				// Additionally: only one part of the equation can be an
				// object...
				if (semanticCond.getObjectSource().getType().equalsIgnoreCase(GlobalConstants.ISPACE_OBJECT)) {

					// // String expression =
					// "$object.getProperty(\"ore\") >= 10";

					boolean res = EvaluateExpression.evaluate(space, semanticCond.getCondition(), semanticCond.getObjectSource().getName(), semanticCond.getObjectSource().getType());

					if (res) {
						System.out.println("#RuntimeManagerPlan# Terminate experiment: Semantic termination condition has been evaluated being true.");
						// Experiment has reached Target Function. Terminate
						break;
					}
				} else {
					IComponentIdentifier agentIdentifier = AgentMethods.getIComponentIdentifier(space, semanticCond.getObjectSource().getName());
					fut = cms.getExternalAccess(agentIdentifier);
					IExternalAccess exta = (IExternalAccess) fut.get(this);

					IOAVState state = ((ElementFlyweight) exta).getState();
					Object rCapability = ((ElementFlyweight) exta).getScope();

					// Evaluate condition/expression
					OAVBDIFetcher fetcher = new OAVBDIFetcher(state, rCapability);
					boolean res = EvaluateExpression.evaluateExpression(fetcher, semanticCond.getCondition());

					if (res) {
						System.out.println("#RuntimeManagerPlan# Terminate experiment: Semantic termination condition has been evaluated being true.");
						// Experiment has reached Target Function. Terminate
						break;
					}
				}
			}

		} else {
			System.err.println("#RunTimeManagerPlan# Terminate Condition missing " + investigationConf);
		}

		ConcurrentHashMap<Long, ArrayList<ObservedEvent>> results = getResult(space);

		prepareResult(results,(String) clientConfMap.get(GlobalConstants.EXPERIMENT_ID));

		System.out.println("#RuntimeManagerPlan# Killing executed application....");
		vis.setExit();
		vis.dispose();

		// FileHandler.deleteFile(appFilePath);
		// appFilePath = null;

		// Decrement the number of currently running experiments on this agent
		numberOfRunningExperiments(-1);
		cms.destroyComponent(exta.getComponentIdentifier());
		//delete *.application.xml from disk
		FileHandler.deleteFile(directoryPath+investigationConf.getName()+".application.xml");
		System.out.println("Number of Exp at this agent: " + (Integer) getBeliefbase().getBelief("numberOfRunningExperiments").getFact());
//		System.out.println("#RuntimeManagerPlan# Goal over???");		
	}

	private void prepareResult(ConcurrentHashMap<Long, ArrayList<ObservedEvent>> observedEvents, String experimentID) {

		Map factsAboutAllExperiments = (HashMap<Integer, HashMap>) getBeliefbase().getBelief("factsAboutAllExperiments").getFact();

		// Map facts = (Map) getBeliefbase().getBelief("simulationFacts").getFact();
		Map facts = (Map) factsAboutAllExperiments.get(localExperimentCounter);

		// System.out.println("GGG: Size" + facts.size());

		facts.put(Constants.EXPERIMENT_END_TIME, new Long(clockservice.getTime()));
		facts.put(Constants.OBSERVED_EVENTS_MAP, observedEvents);
		facts.put(GlobalConstants.EXPERIMENT_ID, experimentID);
		// does not need to be send back to master agent
		// facts.remove(Constants.SIMULATION_FACTS_FOR_CLIENT);

		// System.out.println("GGG2: Size" + facts.size());

		// getBeliefbase().getBelief("simulationFacts").setFact(facts);
		getBeliefbase().getBelief("factsAboutAllExperiments").setFact(factsAboutAllExperiments);
	}

	/**
	 * Returns the observedEvents from the SimulationDataConsumer Hack: Can only process one SimualtioDataConsumer, i.e. it returns the events from the FIRST SimulatioDataConsumer
	 * 
	 * @param space
	 * @return
	 */
	private ConcurrentHashMap<Long, ArrayList<ObservedEvent>> getResult(AbstractEnvironmentSpace space) {
		Collection collection = space.getDataConsumers();

		Iterator itr = collection.iterator();

		while (itr.hasNext()) {
			Object con = itr.next();
			if (con instanceof SimulationDataConsumer)
				return ((SimulationDataConsumer) con).getResults();
		}
		return null;
	}

	/**
	 * Compute Termination: Input: Mode=0 ->relative Time; Mode=1 ->absolute Time
	 * 
	 * @return the termination time as relative time, e.g. the time the simulation has to run.
	 */
	private Long getTerminationTime(int mode, long value) {
		Long currentTime = new Long(clockservice.getTime());

		if (mode == 0) {
			// Long relativeTime = new Long(10000);
			Long terminationTime = new Long(value + currentTime.longValue());
			System.out.println("#RuntimeManagerPlan# StartTime: " + TimeConverter.longTime2DateString(currentTime) + " - TerminationTime: " + TimeConverter.longTime2DateString(terminationTime));
			return new Long(value);
		} else {// TODO: There might be a problem with Day Light Savings Time!
			// Calendar cal = Calendar.getInstance();
			// Date terminationTime = cal.getTime();
			Long duration = new Long(value - currentTime.longValue());
			System.out.println("#RuntimeManagerPlan# StartTime: " + TimeConverter.longTime2DateString(currentTime) + " TerminationTime: " + TimeConverter.longTime2DateString(new Long(value))
					+ ", Duration: " + duration.longValue());
			return duration;
		}
	}

	private void startApplication(Map appConf, HashMap<String, Object> clientConf, InvestigationConfiguration investigationConf) {

		// store file application.xml
		// appFilePath = System.getProperty("user.dir") + "\\"+ simConf.getName()+ (String) clientConf.get(Constants.EXPERIMENT_ID)+ ".application.xml";
		// FileHandler.writeToFile(System.getProperty("user.dir"), simConf.getName()+ (String) clientConf.get(Constants.EXPERIMENT_ID)+ ".application.xml", (String)
		// clientConf.get(Constants.APPLICATION_FILE_AS_XML_STRING));

		// create application in suspended modus
		IFuture fut = cms.createComponent(investigationConf.getName() + (String) clientConf.get(GlobalConstants.EXPERIMENT_ID) + " - " + localExperimentCounter , investigationConf.getApplicationReference(), new CreationInfo(investigationConf.getApplicationConfiguration(), appConf, null, true, false), null);
		IComponentIdentifier cid = (IComponentIdentifier) fut.get(this);
		this.exta = (IExternalAccess) cms.getExternalAccess(cid).get(this);

		// add DataConsumer and Provider
		addDataConsumerAndProvider(investigationConf);

		// Start Online Visualization
		startOnlineVisualization(investigationConf);

		// Hack: Synchronize start time!
		// System.out.println("-->StartTime at Client: " + startTime);
		long startTime = clockservice.getTime();

		fut = exta.getExtension(investigationConf.getNameOfSpace());
		AbstractEnvironmentSpace space = (AbstractEnvironmentSpace) fut.get(this);
		space.setProperty("REAL_START_TIME_OF_SIMULATION", startTime);

		// (Hack?): add experiment-id to space
		space.setProperty(GlobalConstants.EXPERIMENT_ID, (String) clientConf.get(GlobalConstants.EXPERIMENT_ID));

		// resume application
		cms.resumeComponent(cid);

		// Save initial facts of this simulation run.
		Map facts = new HashMap();
		facts.put(Constants.EXPERIMENT_START_TIME, new Long(startTime));
		// getBeliefbase().getBelief("simulationFacts").setFact(facts);
		Map factsAboutAllExperiments = (HashMap<Integer, HashMap>) getBeliefbase().getBelief("factsAboutAllExperiments").getFact();
		factsAboutAllExperiments.put(localExperimentCounter, facts);
		// Map facts = (Map) getBeliefbase().getBelief("simulationFacts").getFact();
		getBeliefbase().getBelief("factsAboutAllExperiments").setFact(factsAboutAllExperiments);

		// *************************************************************
		// This is a hack for this special application.xml -> MarsWorld
		if (space.getSpaceObjectsByType("homebase").length > 0) {
			space.getSpaceObjectsByType("homebase")[0].setProperty("start_time", startTime);
		}
	}

	private void addDataConsumerAndProvider(InvestigationConfiguration investigationConf) {

		IFuture fut =  (exta).getExtension(investigationConf.getNameOfSpace());
		AbstractEnvironmentSpace space = (AbstractEnvironmentSpace) fut.get(this);
		
//		AbstractEnvironmentSpace space = ((AbstractEnvironmentSpace) (exta).getExtension(simConf.getNameOfSpace()));
		IExpressionParser parser = new JavaCCExpressionParser();

		// add new data provider
		List<Dataprovider> providers = investigationConf.getDataproviders().getDataprovider();
		// List tmp = si.getPropertyList("dataproviders");

		// System.out.println("data providers: "+providers);
		if (providers != null) {
			// iteration through all dataprovider
			for (int i = 0; i < providers.size(); i++) {
				// Map dcol = (Map)providers.get(i);

				List<Source> sources = providers.get(i).getSource();
				IObjectSource[] provs = new IObjectSource[sources.size()];
				for (int j = 0; j < sources.size(); j++) {
					Source source = sources.get(j);
					String varname = source.getName() != null ? source.getName() : "$object";
					String objecttype = source.getObjecttype();
					boolean aggregate = source.isAggregate() == null ? false : source.isAggregate();
					IParsedExpression dataexp = EvaluateExpression.getParsedExpression(source.getContent(), parser);
					// Hack: Includeconditon is not implemented, yet.
					IParsedExpression includeexp = null;
					provs[j] = new SpaceObjectSource(varname, space, objecttype, aggregate, dataexp, includeexp);
				}

				String tablename = providers.get(i).getName();
				List<Data> subdatas = providers.get(i).getData();
				String[] columnnames = new String[subdatas.size()];
				IParsedExpression[] exps = new IParsedExpression[subdatas.size()];
				for (int j = 0; j < subdatas.size(); j++) {
					Data subdata = subdatas.get(j);
					columnnames[j] = subdata.getName();
					exps[j] = EvaluateExpression.getParsedExpression((String) subdata.getContent().get(0), parser);
				}

				ITableDataProvider tprov = new DefaultDataProvider(space, provs, tablename, columnnames, exps);
				space.addDataProvider(tablename, tprov);
			}
		}

		// Create the data consumers.
		List<Dataconsumer> consumers = investigationConf.getDataconsumers().getDataconsumer();

		// System.out.println("data consumers: "+consumers);
		if (consumers != null) {
			for (int i = 0; i < consumers.size(); i++) {
				Dataconsumer dcon = consumers.get(i);
				String name = dcon.getName();

				// Class clazz = (Class)MEnvSpaceInstance.getProperty(dcon,
				// "clazz");
				Class clazz = null;
				try {
//					clazz = SReflect.findClass(dcon.getClazz(), toStringArray((ArrayList<String>) simConf.getImports().getImport()),
//							((ILibraryService) SServiceProvider.getService(getScope().getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(this)).getClassLoader());
					
//					clazz = SReflect.findClass(dcon.getClazz(), toStringArray((ArrayList<String>) simConf.getImports().getImport()),
//							((ILibraryService) SServiceProvider.getService(getScope().getServiceContainer(), ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(this)).getClassLoader());
					
					//TODO: Check, if works!
					clazz = SReflect.findClass(dcon.getClazz(), toStringArray((ArrayList<String>) investigationConf.getImports().getImport()),getState().getTypeModel().getClassLoader());								

					// clazz = SReflect.findClass0(classname, imports, classloader);

				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ITableDataConsumer con = null;
				try {
					con = (ITableDataConsumer) clazz.newInstance();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// List of Hashmaps with dynamic=false
				List<Map> tmpPropertyList = new ArrayList<Map>();
				for (int j = 0; j < dcon.getMixedProperty().size(); j++) {
					Map map = new HashMap();
					map.put("dynamic", false);
					map.put("name", dcon.getMixedProperty().get(j).getName());
					// map.put("value",
					// dcon.getPropertyList().get(j).getValue());
					// Hack: has to be "initially" an expression that can be
					// parsed
					map.put("value", EvaluateExpression.getParsedExpression(dcon.getMixedProperty().get(j).getContent(), parser));
					tmpPropertyList.add(map);
				}
				// MEnvSpaceInstance.setProperties(con,
				// (List)dcon.get("properties"), fetcher);
				MEnvSpaceType.setProperties(con, tmpPropertyList, space.getFetcher());//
				con.setProperty("envspace", space);
				space.addDataConsumer(name, con);
			}
		}
		// System.out.println("nnnnnnnnnnneded iterations: " + counterTmp);
	}

	private void startOnlineVisualization(InvestigationConfiguration investigationConf) {

		IFuture fut = exta.getExtension(investigationConf.getNameOfSpace());
		AbstractEnvironmentSpace space = (AbstractEnvironmentSpace) fut.get(this);

		// init online visualization
		ArrayList<AbstractChartDataConsumer> chartDataConsumer = new ArrayList<AbstractChartDataConsumer>();
		for (Iterator it = space.getDataConsumers().iterator(); it.hasNext();) {
			Object abstractConsumer = it.next();

			if (abstractConsumer instanceof AbstractChartDataConsumer)
				chartDataConsumer.add((AbstractChartDataConsumer) abstractConsumer);
		}
		this.vis = new OnlineVisualisation(chartDataConsumer);
	}

	/**
	 * Extract the '*.application.xml' and 'configuration.xml' file from the clientConfMap, store them on the local file system. then they can be used within the RuntimeManagerPlan. There are also
	 * happening some changes to the paths in order to make sure the right *.application.xml is executed on the remote agent.
	 * 
	 * @param clientConfMap
	 */
	private InvestigationConfiguration prepareXMLFiles(HashMap<String, Object> clientConfMap) {

		InvestigationConfiguration investigationConf = (InvestigationConfiguration) XMLHandler
				.parseXMLFromString((String) clientConfMap.get(GlobalConstants.CONFIGURATION_FILE_AS_XML_STRING), InvestigationConfiguration.class);
		
		// adjust path of directory
		// 1: delete the ".." at the beginning
		String oldDirpath = investigationConf.getApplicationReference().substring(2);
		// 2.delete the *.application.xml at the end of the path
		directoryPath += oldDirpath.substring(0, oldDirpath.lastIndexOf("\\\\") + 2);


		// ***********************************************************
		// Purpose: Replace 'name' of application in the *.application.xml with the new name
		String applicationAsString = (String) clientConfMap.get(Constants.APPLICATION_FILE_AS_XML_STRING);
		int first = applicationAsString.indexOf("name=\"");
		// System.out.println("*********************\n" + app.substring(first));
		// first+7: to start searching for next '"' after the 'name="' one above.
		int second = applicationAsString.indexOf("\"", first + 7);
//		System.out.println("*********************\n" + applicationAsString.substring(second));

		// get the first part of the *.application.xml till: 'name="'
		String applicationAsStringNew = applicationAsString.substring(0, first + 6);
		// set the new name of the application
		applicationAsStringNew += investigationConf.getName() + "-" + callerID + "\"";
		investigationConf.setName(investigationConf.getName() + "-" + callerID);
		// get the second part of the *.application.xml after: 'name="'
		applicationAsStringNew += applicationAsString.substring(second + 1);

		// store changes *.application.xml
		FileHandler.writeToFile(directoryPath, investigationConf.getName() + ".application.xml", applicationAsStringNew);
		// ***********************************************************

		// change reference to current path
		investigationConf.setApplicationReference(directoryPath + "\\" + investigationConf.getName() + ".application.xml");

		return investigationConf;
	}

	/**
	 * Increment / Decrement the counter which contains the number of currently executed experiments
	 * 
	 * @param i
	 */
	private void numberOfRunningExperiments(int i) {
		int n = (Integer) getBeliefbase().getBelief("numberOfRunningExperiments").getFact();
		getBeliefbase().getBelief("numberOfRunningExperiments").setFact(n + i);
	}

	private void init() {
		callerID = (Long) getParameter("callerID").getValue();
		clockservice = (IClockService) getScope().getServiceContainer().getRequiredService("clockservice").get(this);
		File file = new File("..");
		try {
			// System.out.println("-->" + file.getCanonicalPath().toString());
			directoryPath = file.getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// directoryPath = System.getProperty("user.home", ".") + "\\SimulationExperiments\\";

	}

	/***
	 * Transforms an ArrayList<String> into String[]
	 * 
	 * @param list
	 * @return
	 */
	private String[] toStringArray(ArrayList<String> list) {
		String[] array = new String[list.size()];

		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}	
		
		return array;
	}
}
