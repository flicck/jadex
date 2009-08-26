package jadex.wfms.service.impl;

import java.util.Set;

import jadex.bpmn.model.MBpmnModel;
import jadex.gpmn.model.MGpmnModel;
import jadex.wfms.IWfms;
import jadex.wfms.client.IClient;
import jadex.wfms.service.IAAAService;
import jadex.wfms.service.IProcessDefinitionService;
import jadex.wfms.service.IModelRepositoryService;

public class ProcessDefinitionConnector implements IProcessDefinitionService
{
	/** The WFMS */
	private IWfms wfms;
	
	public ProcessDefinitionConnector(IWfms wfms)
	{
		this.wfms = wfms;
	}
	
	/**
	 * Adds a BPMN model to the repository
	 * 
	 * @param client the client
	 * @param name name of the model
	 * @param path path to the model
	 */
	public void addBpmnModel(IClient client, String name, String path)
	{
		if (!((IAAAService) wfms.getService(IAAAService.class)).accessAction(client, IAAAService.ADD_BPMN_PROCESS_MODEL))
			return;
		BasicModelRepositoryService mr = (BasicModelRepositoryService) wfms.getService(IModelRepositoryService.class);
		mr.addBpmnModel(name, path);
	}
	
	/**
	 * Gets a BPMN model.
	 * @param name name of the model
	 * @return the model
	 */
	public MBpmnModel getBpmnModel(IClient client, String name)
	{
		if (!((IAAAService) wfms.getService(IAAAService.class)).accessAction(client, IAAAService.REQUEST_BPMN_PROCESS_MODEL))
			return null;
		IModelRepositoryService mr = (IModelRepositoryService) wfms.getService(IModelRepositoryService.class);
		return mr.getBpmnModel(name);
	}
	
	/**
	 * Gets the names of all available BPMN-models
	 * 
	 * @param client the client
	 * @return the names of all available BPMN-models
	 */
	public Set getBpmnModelNames(IClient client)
	{
		if (!((IAAAService) wfms.getService(IAAAService.class)).accessAction(client, IAAAService.REQUEST_BPMN_MODEL_NAMES))
			return null;
		IModelRepositoryService rs = (IModelRepositoryService) wfms.getService(IModelRepositoryService.class);
		return rs.getBpmnModelNames();
	}
	
	/**
	 * Adds a GPMN model to the repository
	 * @param client the client
	 * @param name name of the model
	 * @param path path to the model
	 */
	public void addGpmnModel(IClient client, String name, String path)
	{
		if (!((IAAAService) wfms.getService(IAAAService.class)).accessAction(client, IAAAService.ADD_GPMN_PROCESS_MODEL))
			return;
		BasicModelRepositoryService mr = (BasicModelRepositoryService) wfms.getService(IModelRepositoryService.class);
		mr.addGpmnModel(name, path);
	}
	
	/**
	 * Gets a GPMN model.
	 * @param name name of the model
	 * @return the model
	 */
	public MGpmnModel getGpmnModel(IClient client, String name)
	{
		if (!((IAAAService) wfms.getService(IAAAService.class)).accessAction(client, IAAAService.REQUEST_GPMN_PROCESS_MODEL))
			return null;
		IModelRepositoryService mr = (IModelRepositoryService) wfms.getService(IModelRepositoryService.class);
		return mr.getGpmnModel(name);
	}
	
	/**
	 * Gets the names of all available GPMN-models
	 * 
	 * @param client the client
	 * @return the names of all available GPMN-models
	 */
	public Set getGpmnModelNames(IClient client)
	{
		if (!((IAAAService) wfms.getService(IAAAService.class)).accessAction(client, IAAAService.REQUEST_GPMN_MODEL_NAMES))
			return null;
		IModelRepositoryService rs = (IModelRepositoryService) wfms.getService(IModelRepositoryService.class);
		return rs.getGpmnModelNames();
	}
}
