package jadex.bridge;

import java.util.Map;

/**
 *  Interface for an agent factory
 *  (a factory typically belongs to a specific kernel).
 */
public interface IAgentFactory extends IElementFactory
{
	/**
	 *  Create a kernel agent.
	 *  @param adapter	The platform adapter for the agent. 
	 *  @param model	The agent model file (i.e. the name of the XML file).
	 *  @param config	The name of the configuration (or null for default configuration) 
	 *  @param arguments	The arguments for the agent as name/value pairs.
	 *  @return	An instance of a kernel agent.
	 */
	public IKernelAgent createKernelAgent(IAgentAdapter adapter, String model, String config, Map arguments);
}
