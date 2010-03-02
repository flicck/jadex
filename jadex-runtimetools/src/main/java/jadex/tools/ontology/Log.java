package jadex.tools.ontology;

import jadex.base.fipa.IComponentAction;



public class Log implements IComponentAction
{
	//-------- attributes ----------

	/** A tool type such as "tracer". */
	protected String	tooltype;

	//-------- constructors --------

	/**
	 *  Default Constructor.
	 *  Create a new Log.
	 */
	public Log()
	{
		System.out.println("Created " + this);
	}

	//-------- accessor methods --------

	/**
	 *  Get the tool-type of this Log.
	 *  A tool type such as "tracer".
	 * @return tool-type
	 */
	public String getToolType()
	{
		return this.tooltype;
	}

	/**
	 *  Set the tool-type of this Log.
	 *  A tool type such as "tracer".
	 * @param tooltype the value to be set
	 */
	public void setToolType(String tooltype)
	{
		this.tooltype = tooltype;
	}

	//-------- additional methods --------

	/**
	 *  Get a string representation of this Log.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "Log(" + "tooltype=" + getToolType() + ")";
	}
}
