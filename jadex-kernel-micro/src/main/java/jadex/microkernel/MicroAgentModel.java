package jadex.microkernel;

import java.lang.reflect.Method;
import java.util.Map;

import jadex.bridge.IArgument;
import jadex.bridge.IJadexModel;
import jadex.bridge.IReport;
import jadex.commons.SUtil;

/**
 *  The agent model contains the OAV agent model in a state and
 *  a type-specific compiled rulebase (matcher functionality).
 */
public class MicroAgentModel implements IJadexModel
{
	//-------- attributes --------

	/** The microagent. */
	protected Class microagent;
	
	/** The filename. */
	protected String filename;
	
	/** The meta information .*/
	protected MicroAgentMetaInfo metainfo;
	
	//-------- constructors --------
	
	/**
	 *  Create a model.
	 */
	public MicroAgentModel(Class microagent, String filename)
	{
		this.microagent = microagent;
		this.filename = filename;
		
		// Try to read meta information from class.
		try
		{
			Method m = microagent.getMethod("getMetaInfo", new Class[0]);
			if(m!=null)
				this.metainfo = (MicroAgentMetaInfo)m.invoke(null, new Object[0]);
		}
		catch(Exception e)
		{
//			e.printStackTrace();
		}
	}
	
	//-------- IJadexModel methods --------
	
	/**
	 *  Is the model startable.
	 *  @return True, if startable.
	 */
	public boolean isStartable()
	{
		return true;
	}
	
	/**
	 *  Get the model type.
	 *  @reeturn The model type (kernel specific).
	 */
	public String getType()
	{
		// todo: 
		return "v2microagent";
	}

	//-------- IJadexModel methods --------
	
	/**
	 *  Get the name.
	 *  @return The name.
	 */
	public String getName()
	{
		String ret = microagent.getSimpleName();
		if(ret.endsWith("Agent"))
			ret = ret.substring(0, ret.lastIndexOf("Agent"));
		return ret;
		
//		String ret;
//		if(metainfo!=null && metainfo.getName()!=null)
//			ret = metainfo.getName();
//		else
//			ret = microagent.getSimpleName();
//		return ret;
	}
	
	/**
	 *  Get the model description.
	 *  @return The model description.
	 */
	public String getDescription()
	{
		String ret;
		if(metainfo!=null && metainfo.getDescription()!=null)
			ret = metainfo.getDescription();
		else
			ret = null;
		return ret;
	}
	
	/**
	 *  Get the report.
	 *  @return The report.
	 */
	public IReport getReport()
	{
		// todo: 
		return new IReport()
		{
			public Map getDocuments()
			{
				return null;
			}
			
			public boolean isEmpty()
			{
				return true;
			}
			
			public String toHTMLString()
			{
				return "";
			}
		};
	}
	
	/**
	 *  Get the configurations.
	 *  @return The configuration.
	 */
	public String[] getConfigurations()
	{
		String[] ret;
		if(metainfo!=null)
			ret = metainfo.getConfigurations();
		else
			ret = SUtil.EMPTY_STRING;
		return ret;
	}
	
	/**
	 *  Get the arguments.
	 *  @return The arguments.
	 */
	public IArgument[] getArguments()
	{		
		IArgument[] ret;
		if(metainfo!=null)
			ret = metainfo.getArguments();
		else
			ret = new IArgument[0];
		return ret;
	}
	
	/**
	 *  Get the filename.
	 *  @return The filename.
	 */
	public String getFilename()
	{
//		System.out.println("Filename: "+fn);
		return filename;
	}
	
	//-------- methods --------
	
	/**
	 *  Get the micro agent class.
	 *  @return The class.
	 */
	public Class getMicroAgentClass()
	{
		return microagent;
	}
	
	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "MicroAgentModel("+microagent+", "+filename+")";
	}
}
