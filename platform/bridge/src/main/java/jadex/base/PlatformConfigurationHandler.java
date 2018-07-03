package jadex.base;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import jadex.bridge.ClassInfo;
import jadex.bridge.ProxyFactory;
import jadex.bridge.modelinfo.ConfigurationInfo;
import jadex.bridge.modelinfo.IArgument;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.types.monitoring.IMonitoringService;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.javaparser.SJavaParser;

/**
 *  Platform configuration handler is a proxy handler that implements
 *  the logic for the configuration interfaces.
 */
public class PlatformConfigurationHandler implements InvocationHandler
{
	/** Readonly flag. */
	protected boolean readonly;
	
	/** The map of values. */
	protected Map<String, Object> values = new HashMap<String, Object>();
	
	/** Used for mapping method names to property names (if they differ). */
	public static Map<String, String> namemappings = new HashMap<String, String>();
	
	/** The default values. */
	public static  Map<String, Object> defvalues = new HashMap<String, Object>();
	
	static
	{
		namemappings.put("configurationfile", IPlatformConfiguration.CONFIGURATION_FILE);
		
		// This stuff must be in the configuration because it configures the starter (not the platform component itself)
//		defvalues.put(IPlatformConfiguration.PLATFORM_NAME, "jadex");
		defvalues.put(IPlatformConfiguration.CONFIGURATION_NAME, "auto");
		defvalues.put(IPlatformConfiguration.AUTOSHUTDOWN, Boolean.FALSE);
		defvalues.put(IPlatformConfiguration.WELCOME, Boolean.TRUE);
		defvalues.put(IPlatformConfiguration.COMPONENT_FACTORY, IPlatformConfiguration.FALLBACK_COMPONENT_FACTORY);
		defvalues.put(IPlatformConfiguration.CONFIGURATION_FILE, IPlatformConfiguration.FALLBACK_PLATFORM_CONFIGURATION);
		defvalues.put("platformcomponent", new ClassInfo("jadex.platform.service.cms.PlatformComponent"));
		Long	timeout	= PlatformConfigurationHandler.getDefaultTimeout();
		defvalues.put("localdefaulttimeout", timeout);
		defvalues.put("remotedefaulttimeout", timeout);

		//		defvalues.put("components", new ArrayList<String>());
//		defvalues.put(GUI, Boolean.TRUE);
//		defvalues.put(CLI, Boolean.TRUE);
//		defvalues.put(CLICONSOLE, Boolean.FALSE);
//		defvalues.put(SAVEONEXIT, Boolean.TRUE);
//		defvalues.put(LOGGING, Boolean.FALSE);
//		defvalues.put(LOGGING_LEVEL, java.util.logging.Level.SEVERE);
//		defvalues.put(THREADPOOLDEFER, Boolean.TRUE);
//		defvalues.put(UNIQUEIDS, Boolean.TRUE);
//		defvalues.put(CHAT, Boolean.TRUE);
//		defvalues.put(AWARENESS, Boolean.TRUE);
//		defvalues.put(AWAMECHANISMS, "Multicast, Local, Broadcast");
//		defvalues.put(AWADELAY, 20000);
//		defvalues.put(BINARYMESSAGES, Boolean.TRUE);
//		defvalues.put(STRICTCOM, Boolean.FALSE);
//		defvalues.put(LOCALTRANSPORT, Boolean.TRUE);
//		defvalues.put(TCPTRANSPORT, Boolean.TRUE);
//		defvalues.put(TCPPORT, 0);
//		defvalues.put(WSTRANSPORT, Boolean.TRUE);
//		defvalues.put(WSPORT, -1);
//		defvalues.put(RELAYTRANSPORT, Boolean.TRUE);
////		defvalues.put(RELAYADDRESSES, "");
//		defvalues.put(RELAYFORWARDING, Boolean.FALSE);
//		defvalues.put(WSPUBLISH, Boolean.FALSE);
//		defvalues.put(RSPUBLISH, Boolean.FALSE);
////		defvalues.put(RSPUBLISHCOMPONENT, "%{jadex.commons.SReflect.chooseAvailableResource(jadex.bridge.service.types.publish.IPublishService.DEFAULT_RSPUBLISH_COMPONENTS)}");
//		defvalues.put(RSPUBLISHCOMPONENT, SReflect.chooseAvailableResource(jadex.bridge.service.types.publish.IPublishService.DEFAULT_RSPUBLISH_COMPONENTS));
//		defvalues.put(MAVEN_DEPENDENCIES, Boolean.FALSE);
//		defvalues.put("superpeer", Boolean.FALSE);
//		defvalues.put("supersuperpeer", Boolean.FALSE);
//		defvalues.put("kernel_multi", Boolean.TRUE);
//		defvalues.put("kernel_micro", Boolean.FALSE);
//		defvalues.put("kernel_component", Boolean.FALSE);
//		defvalues.put("kernel_application",  Boolean.FALSE);
//		defvalues.put("kernel_bdiv3",  Boolean.FALSE);
//		defvalues.put("kernel_bdi",  Boolean.FALSE);
//		defvalues.put("kernel_bpmn",  Boolean.FALSE);
//		defvalues.put("sensors", Boolean.FALSE);
//		defvalues.put("mon", Boolean.TRUE);
//		defvalues.put("df", Boolean.FALSE);
//		defvalues.put("clock", Boolean.TRUE);
//		defvalues.put("simul", Boolean.TRUE);
//		defvalues.put("filetransfer", Boolean.FALSE);
//		defvalues.put("security", Boolean.TRUE);
//		defvalues.put("library", Boolean.TRUE);
//		defvalues.put("settings", Boolean.TRUE);
//		defvalues.put("context", Boolean.TRUE);
//		defvalues.put("address", Boolean.TRUE);

//		defvalues.put("superpeerclient", "%{$args.superpeer==null && $args.supersuperpeer? true: !$args.superpeer && !$args.supersuperpeer}");
//		System.out.println("PlatformConfigurationHandler: "+defvalues+" "+hashCode());
	}
	
	/**
	 *  Create a new handler.
	 */
	public PlatformConfigurationHandler()
	{
		synchronized(defvalues)
		{
			values.putAll(defvalues);
		}
	}	
	
//	/**
//	 *  Get the argument declarations.
//	 *  @return The argument declarations.
//	 */
//	public static Argument[] getArguments()
//	{
//		List<Argument> ret = new ArrayList<Argument>();
//		
//		for(Map.Entry<String, Object> entry: defvalues.entrySet())
//		{
//			Object val = entry.getValue();
//			ret.add(new Argument(entry.getKey(), null, val!=null? val.getClass().getName(): null, null));
//		}
//		
//		ret.add(new Argument(IPlatformConfiguration.PLATFORM_NAME, "", String.class.getName(), null));
//		ret.add(new Argument(PROGRAM_ARGUMENTS, "", SReflect.getClassName(String[].class), null));
//		ret.add(new Argument(JCCPLATFORMS, "", String.class.getName(), null));
//		ret.add(new Argument(SIMULATION, "", Boolean.class.getName(), null));
//		ret.add(new Argument(ASYNCEXECUTION, "", Boolean.class.getName(), null));
//		ret.add(new Argument(LIBPATH, "", String.class.getName(), null));
//		ret.add(new Argument(BASECLASSLOADER, "", ClassLoader.class.getName(), null));
//		ret.add(new Argument(AWAINCLUDES, "", String.class.getName(), null));
//		ret.add(new Argument(AWAEXCLUDES, "", String.class.getName(), null));
//		ret.add(new Argument("usesecret", "", Boolean.class.getName(), null));
//		ret.add(new Argument("printsecret", "", Boolean.class.getName(), null));
//		ret.add(new Argument("networkname", "", String.class.getName(), null));
//		ret.add(new Argument("networksecret", "", String.class.getName(), null));
//		ret.add(new Argument("roles", "", String.class.getName(), null));
//		ret.add(new Argument(THREADPOOLCLASS, "", String.class.getName(), null));
//		ret.add(new Argument(CONTEXTSERVICECLASS, "", String.class.getName(), null));
//		ret.add(new Argument("superpeerclient", "", Boolean.class.getName(), null));
//		
//		return ret.toArray(new Argument[ret.size()]);
//	}
	
	/**
	 *  Called on method invocation.
	 *  @param proxy The proxy.
	 *  @param method The method.
	 *  @param args The arguments.
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		Object ret = null;
		
		String mname = method.getName();
		
		// from IPlatformConfiguration
		if(mname.equals("getExtendedPlatformConfiguration"))
		{
			ret = proxy;
		}
		// from IPlatformConfiguration
//		else if(mname.equals("getStarterConfig"))
//		{
//			ret = proxy;
//		}
		// Convert class to name.
		else if(mname.equals("addComponent") && args[0] instanceof Class<?>)
		{
			checkReadOnly();
			((IPlatformConfiguration)proxy).addComponent(((Class<?>)args[0]).getName()+".class");
		}
		else if(mname.equals("setReadOnly"))
		{
			readonly = ((Boolean)args[0]).booleanValue();
		}
		else if(mname.equals("isReadOnly"))
		{
			ret = readonly;
		}
		else if(mname.equals("getSuperpeerClient") || mname.equals("isSuperpeerClient"))
		{
			Boolean spc = (Boolean)values.get("superpeerclient");
			if(spc==null)
			{
				Boolean sp = (Boolean)values.get("superpeer");
				Boolean ssp = (Boolean)values.get("supersuperpeer");
				spc =  sp==null && ssp==null? true: sp!=null && sp.booleanValue() || ssp!=null && ssp.booleanValue();
			}
			return spc;
		}
		else if(mname.equals("setValue"))
		{
			checkReadOnly();
			values.put((String)args[0], args[1]);
		}
		else if(mname.equals("getValue"))
		{
			ret = values.get(args[0]);
		}
//		else if(mname.equals("parseArg"))
//		{
//			checkReadOnly();
//			parseArg((IPlatformConfiguration)proxy, (String)args[0], (String)args[1], args[2]);
//		}
		else if(mname.equals("getValues"))
		{
			ret = new HashMap<String, Object>(values);
			String[] kernels = (String[])values.get("kernels");
			if(kernels!=null)
			{
				for(String kernel: kernels)
				{
					((Map<String, Object>)ret).put("kernel_"+kernel, Boolean.TRUE);
				}
			}
		}
		else if(mname.equals("enhanceWith"))
		{
			checkReadOnly();
			Map<String, Object>	other	= ((PlatformConfigurationHandler)ProxyFactory.getInvocationHandler(args[0])).values;
	        for(Map.Entry<String, Object> entry : other.entrySet())
	        {
	            values.put(entry.getKey(), entry.getValue());
	        }
		}
		else if(mname.equals("clone"))
		{
			PlatformConfigurationHandler h = new PlatformConfigurationHandler();
			h.values = new HashMap<String, Object>(values);
			ret = getPlatformConfiguration(null, h);
		}
//		else if(mname.equals("getComponentFactory"))
//		{
//			 return (String)values.get(IStarterConfiguration.COMPONENT_FACTORY)!=null?
//				 (String)values.get(IStarterConfiguration.COMPONENT_FACTORY): IStarterConfiguration.FALLBACK_COMPONENT_FACTORY;
//		}
		
		else if(mname.startsWith("set"))
		{
			checkReadOnly();
			values.put(getKeyForMethodname(mname, 3), args[0]);
		}
		else if(mname.startsWith("add"))
		{
			checkReadOnly();
			String prop	= getKeyForMethodname(SUtil.getPlural(mname), 3);
			Collection<Object> vals = (Collection<Object>)values.get(prop);
			if(vals==null)
			{
				vals = new ArrayList<Object>();
				values.put(prop, vals);
			}
			vals.add(args[0]);
		}
		else if(mname.startsWith("get") || method.getName().startsWith("has"))
		{
//			ret = values.get(getKeyForMethodname(mname, 3));
			ret = getValue(getKeyForMethodname(mname, 3), args!=null && args.length>0? (IModelInfo)args[1]: null);
		}
		else if(method.getName().startsWith("is"))
		{
			ret = values.get(getKeyForMethodname(mname, 2));
		}
		else if(mname.startsWith("remove"))
		{
			checkReadOnly();
			String prop	= getKeyForMethodname(SUtil.getPlural(mname), 3);
			Collection<Object> vals = (Collection<Object>)values.get(prop);
			if(vals!=null)
				vals.remove(args[0]);
		}
		else
		{
			throw new UnsupportedOperationException(method.getName());
		}
		
		if(ret==null && method.getReturnType().equals(boolean.class))
			ret = false;
		
//		System.out.println("config: "+method.getName()+" "+hashCode()+" "+method.getDeclaringClass());
		
		return ret;
	}
	
	/**
	 *  Check the readonly state.
	 */
	protected void checkReadOnly()
	{
		if(readonly)
			throw new RuntimeException("Config is readonly");
	}
	
	/**
	 *  Get the key for a method name.
	 *  @param mname The method name.
	 *  @param prefixlen The prefix length.
	 *  @return The key.
	 */
	protected String getKeyForMethodname(String mname, int prefixlen)
	{
		String ret = mname.substring(prefixlen).toLowerCase();
		if(namemappings.containsKey(ret))
			ret = namemappings.get(ret);
		return ret;
	}
	
	/**
     *  Parse an argument.
     *  @param key The key.
     *  @param strval The value.
     */
    public void parseArg(IPlatformConfiguration config, String key, String strval, Object value)
    {
        if(IPlatformConfiguration.COMPONENT.equals(key))
        {
            config.addComponent((String)strval);
        }
        else if(IPlatformConfiguration.DEBUGFUTURES.equals(key) && "true".equals(strval))
        {
        	config.getExtendedPlatformConfiguration().setDebugFutures(true);
        }
        else if(IPlatformConfiguration.DEBUGSERVICES.equals(key) && "true".equals(strval))
        {
        	config.getExtendedPlatformConfiguration().setDebugServices(true);
        }
        else if(IPlatformConfiguration.DEBUGSTEPS.equals(key) && "true".equals(strval))
        {
        	config.getExtendedPlatformConfiguration().setDebugSteps(true);
        }
        else if(IPlatformConfiguration.DEFTIMEOUT.equals(key))
        {
        	value = SJavaParser.evaluateExpression(strval, null);
//			BasicService.DEFTIMEOUT	= ((Number)stringValue).longValue();
            long to	= ((Number)value).longValue();
//			setLocalDefaultTimeout(platform, to);
//			setRemoteDefaultTimeout(platform, to);
            config.setDefaultTimeout(to);

//			BasicService.setRemoteDefaultTimeout(to);
//			BasicService.setLocalDefaultTimeout(to);
//			System.out.println("timeout: "+BasicService.DEFAULT_LOCAL);
        }
        else if(IPlatformConfiguration.NOSTACKCOMPACTION.equals(key) && "true".equals(strval))
        {
        	config.getExtendedPlatformConfiguration().setNoStackCompaction(true);
        }
        else if(IPlatformConfiguration.OPENGL.equals(key) && "false".equals(strval))
        {
        	config.getExtendedPlatformConfiguration().setOpenGl(false);
        }
        else if(IPlatformConfiguration.MONITORING.equals(key))
        {
//            Object tmpmoni = getValue(IStarterConfiguration.MONITORING);
            Object tmpmoni = values.get(IPlatformConfiguration.MONITORING);
            IMonitoringService.PublishEventLevel moni = IMonitoringService.PublishEventLevel.OFF;
            if(tmpmoni instanceof Boolean)
            {
                moni = ((Boolean)tmpmoni).booleanValue()? IMonitoringService.PublishEventLevel.FINE: IMonitoringService.PublishEventLevel.OFF;
            }
            else if(tmpmoni instanceof String)
            {
                moni = IMonitoringService.PublishEventLevel.valueOf((String)tmpmoni);
            }
            else if(tmpmoni instanceof IMonitoringService.PublishEventLevel)
            {
                moni = (IMonitoringService.PublishEventLevel)tmpmoni;
            }
            config.getExtendedPlatformConfiguration().setMonitoring(moni);
        }
        else
        {
        	config.setValue(key, value);
        }
    }
    
    // for starter
//    /**
//     * Generic getter for configuration parameters.
//     * Retrieves values in 3 stages:
//     * 1. From given command line arguments.
//     * 2. From given model configuration ("auto", "fixed", ...)
//     * 3. From model default values.
//     *
//     * For retrieval from model, setPlatformModel has to be called before.
//     * @param key
//     * @return
//     */
//    protected Object getValue(String key) 
//    {
//        Object val = values.get(key);
//        if(val==null && getModel()!= null && getConfigurationInfo(getModel()) != null)
//        {
//            val = getArgumentValueFromModel(key);
//        }
//        else if(val instanceof String)
//        {
//            // Try to parse value from command line.
//            try
//            {
//                Object newval = SJavaParser.evaluateExpression((String)val, null);
//                if(newval!=null)
//                {
//                    val	= newval;
//                }
//            }
//            catch(RuntimeException e)
//            {
//            }
//        }
//
//        return val;
//    }
    
    /**
     * Generic getter for configuration parameters.
     * Retrieves values in 3 stages:
     * 1. From given command line arguments.
     * 2. From given model configuration ("auto", "fixed", ...)
     * 3. From model default values.
     *
     * For retrieval from model, setPlatformModel has to be called before.
     * @param key
     * @return
     */
    protected Object getValue(String key, IModelInfo model) 
    {
        Object val = values.get(key);
        if(val==null && getModel()!= null && getConfigurationInfo(getConfigurationName(), model) != null)
        {
            val = getArgumentValueFromModel(key, model);
        }
        else if(val instanceof String)
        {
            // Try to parse value from command line.
            try
            {
                Object newval = SJavaParser.evaluateExpression((String)val, null);
                if(newval!=null)
                {
                    val	= newval;
                }
            }
            catch(RuntimeException e)
            {
            }
        }

        return val;
    }
    
//    /**
//     * 
//     */
//    public static Object getValue(String key, IPlatformConfiguration config, IModelInfo model)
//    {
//    	 Object val = config.getValue(key);
//    	 String configname = config.getConfigurationName();
//         if(val==null && model!= null && getConfigurationInfo(configname, model) != null)
//         {
//             val = getArgumentValueFromModel(key, model);
//         }
//         else if(val instanceof String)
//         {
//             // Try to parse value from command line.
//             try
//             {
//                 Object newval = SJavaParser.evaluateExpression((String)val, null);
//                 if(newval!=null)
//                 {
//                     val	= newval;
//                 }
//             }
//             catch(RuntimeException e)
//             {
//             }
//         }
//
//         return val;
//    }
    
    /**
     * 
     */
    protected IModelInfo getModel()
    {
    	return (IModelInfo)values.get("platformmodel");
    }
    
//    /**
//     *  Get the configuration name.
//     */
//    protected ConfigurationInfo	getConfigurationInfo(IModelInfo model)
//    {
//        String	configname	= getConfigurationName();//(String)cmdargs.get(CONFIGURATION_NAME);
//        if(configname==null)
//        {
//            Object	val	= null;
//            IArgument	arg	= model.getArgument(IStarterConfiguration.CONFIGURATION_NAME);
//            if(arg!=null)
//            {
//                val	= arg.getDefaultValue();
//            }
//            val	= SJavaParser.getParsedValue(val, model.getAllImports(), null, Starter.class.getClassLoader());
////			val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, model.getClassLoader());
//            configname	= val!=null ? val.toString() : null;
//        }
//
//        ConfigurationInfo	compConfig	= configname!=null
//                ? model.getConfiguration(configname)
//                : model.getConfigurations().length>0 ? model.getConfigurations()[0] : null;
//
//        return compConfig;
//    }
    
    /**
     *  Get the configuration name.
     */
    public static ConfigurationInfo	getConfigurationInfo(String configname, IModelInfo model)
    {
       // String	configname	= getConfigurationName();//(String)cmdargs.get(CONFIGURATION_NAME);
        if(configname==null)
        {
            Object	val	= null;
            IArgument	arg	= model.getArgument(IPlatformConfiguration.CONFIGURATION_NAME);
            if(arg!=null)
            {
                val	= arg.getDefaultValue();
            }
            val	= SJavaParser.getParsedValue(val, model.getAllImports(), null, Starter.class.getClassLoader());
//			val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, model.getClassLoader());
            configname	= val!=null ? val.toString() : null;
        }

        ConfigurationInfo	compConfig	= configname!=null
                ? model.getConfiguration(configname)
                : model.getConfigurations().length>0 ? model.getConfigurations()[0] : null;

        return compConfig;
    }
    
    /**
     * 
     */
    public String getConfigurationName()
    {
        return (String)values.get(IPlatformConfiguration.CONFIGURATION_NAME);
    }
    
//    /**
//     * 
//     * @param name
//     * @return
//     */
//    protected Object getArgumentValueFromModel(String name)
//    {
//        Object val = null;
//
//        boolean	found	= false;
//        // first try to get the value from choosen configuration
//        if(getConfigurationInfo(getModel())!=null)
//        {
//            UnparsedExpression[]	upes	= getConfigurationInfo(getModel()).getArguments();
//            for(int i=0; !found && i<upes.length; i++)
//            {
//                if(name.equals(upes[i].getName()))
//                {
//                    found	= true;
//                    val	= upes[i];
//                }
//            }
//        }
//        // if this fails, get default value.
//        if(!found)
//        {
//            IArgument arg	= getModel().getArgument(name);
//            if(arg!=null)
//            {
//                val	= arg.getDefaultValue();
//            }
//        }
//        val	= SJavaParser.getParsedValue(val, getModel().getAllImports(), null, Starter.class.getClassLoader());
////		val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, model.getClassLoader());
//        return val;
//    }
    
    /**
     * 
     * @param name
     * @return
     */
    public static Object getArgumentValueFromModel(String name, IModelInfo model)
    {
        Object val = null;

        boolean	found	= false;
        // first try to get the value from choosen configuration
        if(getConfigurationInfo(name, model)!=null)
        {
            UnparsedExpression[]	upes	= getConfigurationInfo(name, model).getArguments();
            for(int i=0; !found && i<upes.length; i++)
            {
                if(name.equals(upes[i].getName()))
                {
                    found	= true;
                    val	= upes[i];
                }
            }
        }
        // if this fails, get default value.
        if(!found)
        {
            IArgument arg	= model.getArgument(name);
            if(arg!=null)
                val	= arg.getDefaultValue();
        }
        val	= SJavaParser.getParsedValue(val, model.getAllImports(), null, Starter.class.getClassLoader());
//		val	= UnparsedExpression.getParsedValue(val, model.getAllImports(), null, model.getClassLoader());
        return val;
    }
    
//    /**
//     * 
//     */
//    public Map<String, Object> getCmdArgs()
//    {
//    	return (Map<String, Object>)values.get();
//    }
    
//	/**
//	 *  Get the default platform configuration.
//	 *  @return The default configuration.
//	 */
//	public static IPlatformConfiguration getDefaultPlatformConfiguration()
//	{
//		return getDefaultPlatformConfiguration(null);
//	}
//	
//	/**
//	 *  Get the default platform configuration.
//	 *  @param cl The classloader.
//	 *  @return The default configuration.
//	 */
//	public static IPlatformConfiguration getDefaultPlatformConfiguration(ClassLoader cl)
//	{
//		IPlatformConfiguration ret = (IPlatformConfiguration)ProxyFactory.newProxyInstance(cl, new Class[]{IPlatformConfiguration.class}, new PlatformConfigurationHandler());
//		return ret;
//	}
	
	/**
	 *  Get the default platform configuration.
	 *  @return The default configuration.
	 */
	public static IPlatformConfiguration getPlatformConfiguration()
	{
		return getPlatformConfiguration((ClassLoader)null);
	}
	
	/**
	 *  Get the default platform configuration.
	 *  @param cl The classloader.
	 *  @return The default configuration.
	 */
	public static IPlatformConfiguration getPlatformConfiguration(ClassLoader cl)
	{
		return getPlatformConfiguration(cl, new PlatformConfigurationHandler());
	}
	
	/**
	 *  Get the default platform configuration.
	 *  @param cl The classloader.
	 *  @return The default configuration.
	 */
	public static IPlatformConfiguration getPlatformConfiguration(ClassLoader cl, PlatformConfigurationHandler h)
	{
		cl = cl==null? (ClassLoader)IPlatformConfiguration.class.getClassLoader(): cl;
		IPlatformConfiguration ret = (IPlatformConfiguration)ProxyFactory.newProxyInstance(cl, new Class[]{IPlatformConfiguration.class, IExtendedPlatformConfiguration.class}, h);
		return ret;
	}
	
//	/**
//	 *  Get the default platform configuration.
//	 *  @return The default configuration.
//	 */
//	public static IPlatformConfiguration getPlatformConfiguration(String[] args)
//	{
//		return getPlatformConfiguration(args, null);
//	}
//	
//	/**
//	 *  Get the default platform configuration.
//	 *  @param cl The classloader.
//	 *  @return The default configuration.
//	 */
//	public static IPlatformConfiguration getPlatformConfiguration(String[] args, ClassLoader cl)
//	{
//		IPlatformConfiguration ret = (IPlatformConfiguration)ProxyFactory.newProxyInstance(cl, new Class[]{IPlatformConfiguration.class}, new PlatformConfigurationHandler());
//		ret.setProgramArguments(args);
//		return ret;
//	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	public static Set<String> createReserved() 
//    {
//		Set<String> RESERVED = new HashSet<String>();
//        RESERVED = new HashSet<String>();
//        RESERVED.add(IPlatformConfiguration.CONFIGURATION_FILE);
//        RESERVED.add(IPlatformConfiguration.CONFIGURATION_NAME);
//        RESERVED.add(IPlatformConfiguration.PLATFORM_NAME);
//        RESERVED.add(IPlatformConfiguration.COMPONENT_FACTORY);
//        RESERVED.add(IPlatformConfiguration.PLATFORM_COMPONENT);
//        RESERVED.add(IPlatformConfiguration.AUTOSHUTDOWN);
//        RESERVED.add(IPlatformConfiguration.MONITORING);
//        RESERVED.add(IPlatformConfiguration.WELCOME);
//        RESERVED.add(IPlatformConfiguration.COMPONENT);
//        RESERVED.add(IPlatformConfiguration.PARAMETERCOPY);
//        RESERVED.add(IPlatformConfiguration.REALTIMETIMEOUT);
//        RESERVED.add(IPlatformConfiguration.PERSIST);
//        RESERVED.add(IPlatformConfiguration.DEBUGFUTURES);
//        RESERVED.add(IPlatformConfiguration.DEBUGSERVICES);
//        RESERVED.add(IPlatformConfiguration.DEBUGSTEPS);
//        RESERVED.add(IPlatformConfiguration.NOSTACKCOMPACTION);
//        RESERVED.add(IPlatformConfiguration.OPENGL);
//        RESERVED.add(IPlatformConfiguration.DEFTIMEOUT);
//        RESERVED.add(IPlatformConfiguration.PRINTEXCEPTIONS);
//        return RESERVED;
//	}
	
	/**
	 * 
	 * @return
	 */
	public static Long getEnvironmentDefaultTimeout()
	{
		// Set deftimeout from environment, if set.
	    String	dtoprop	= System.getProperty("jadex.deftimeout", System.getenv("jadex.deftimeout"));
	    if(dtoprop==null)
	    	dtoprop	= System.getProperty("jadex_deftimeout", System.getenv("jadex_deftimeout"));
	    if(dtoprop==null)
	    	dtoprop	= System.getProperty("jadex_timeout", System.getenv("jadex_timeout"));
//	    if(dtoprop!=null)
//	    {
//	        System.out.println("Property jadex.deftimeout is deprecated. Use jadex_deftimeout instead.");
//	    }
//	    else
//	    {
//	        dtoprop	= System.getProperty("jadex_deftimeout", System.getenv("jadex_deftimeout"));
//	    }
	    if(dtoprop!=null)
	    {
//	        DEFAULT_REMOTE_TIMEOUT = (Long.parseLong(dtoprop));
//	        DEFAULT_LOCAL_TIMEOUT = (Long.parseLong(dtoprop));
	        System.out.println("Setting jadex_timeout: "+dtoprop);
	    }
	    return dtoprop!=null? Long.parseLong(dtoprop): null;
	}
	
	/**
	 * 
	 */
	public static Long getDefaultTimeout()
	{
		Long ret = getEnvironmentDefaultTimeout();
		if(ret==null)
			ret = SReflect.isAndroid() ? 60000L : 30000;
		return ret;
	}
	
	/**
	 * Returns a PlatformConfiguration with the default parameters.
	 */
	public static IPlatformConfiguration getDefault()
	{
		IPlatformConfiguration config = getPlatformConfiguration();
//		IExtendedPlatformConfiguration econfig = (IExtendedPlatformConfiguration)config;
//		rootconf.setAwaMechanisms(IRootComponentConfiguration.AWAMECHANISM.broadcast, IRootComponentConfiguration.AWAMECHANISM.multicast, IRootComponentConfiguration.AWAMECHANISM.message,
//			IRootComponentConfiguration.AWAMECHANISM.relay, IRootComponentConfiguration.AWAMECHANISM.local);
//		econfig.setAwaMechanisms(IPlatformConfiguration.AWAMECHANISM_BROADCAST, IPlatformConfiguration.AWAMECHANISM_MULTICAST, IPlatformConfiguration.AWAMECHANISM_LOCAL);
//		config.setValue("compregistry", Boolean.TRUE);
		return config;
	}

	/**
	 * Returns a PlatformConfiguration with the default parameters but without gui.
	 */
	public static IPlatformConfiguration getDefaultNoGui()
	{
		IPlatformConfiguration config = getDefault();
		config.setGui(false);
		return config;
	}

	/**
	 * Returns a PlatformConfiguration with the default parameters.
	 */
	public static IPlatformConfiguration getAndroidDefault()
	{
		IPlatformConfiguration config = getDefault();
		config.setGui(false);
		config.getExtendedPlatformConfiguration().setChat(false);
		config.setKernels(IPlatformConfiguration.KERNEL_COMPONENT, 
			IPlatformConfiguration.KERNEL_MICRO, IPlatformConfiguration.KERNEL_BPMN, IPlatformConfiguration.KERNEL_BDIV3);
		config.setLoggingLevel(Level.INFO);
		// config.setDebugFutures(true);
		return config;
	}

	/**
	 * Returns a minimal platform configuration without any network
	 * connectivity.
	 */
	public static IPlatformConfiguration getMinimal()
	{
		IPlatformConfiguration config = getDefault();
		config.setWelcome(false);
		config.setGui(false);
		config.getExtendedPlatformConfiguration().setCli(false);
		config.getExtendedPlatformConfiguration().setCliConsole(false);

		config.getExtendedPlatformConfiguration().setChat(false);

		config.setAwareness(false);
		config.getExtendedPlatformConfiguration().setAwaMechanisms();

//		config.setLocalTransport(true); // needed by message
		config.getExtendedPlatformConfiguration().setTcpTransport(false);
		config.getExtendedPlatformConfiguration().setWsTransport(false);
		config.getExtendedPlatformConfiguration().setRelayTransport(false);
		// rootConfig.setSslTcpTransport(false);

		config.setKernels(IPlatformConfiguration.KERNEL_MICRO);
		// rootConfig.setThreadpoolClass(null);
		// rootConfig.setContextServiceClass(null);

		config.getExtendedPlatformConfiguration().setMonitoringComp(false);
		config.getExtendedPlatformConfiguration().setDf(false);
		config.getExtendedPlatformConfiguration().setClock(true);
		config.getExtendedPlatformConfiguration().setSimul(false);
		config.getExtendedPlatformConfiguration().setFiletransfer(false);
		config.getExtendedPlatformConfiguration().setSecurity(true);
		config.getExtendedPlatformConfiguration().setLibrary(true); // needed by micro
		config.getExtendedPlatformConfiguration().setSettings(true);
		config.getExtendedPlatformConfiguration().setContext(true);
		config.getExtendedPlatformConfiguration().setAddress(true);
		config.setValue("compregistry", Boolean.TRUE);
		
		config.setSuperpeer(false);
		config.setSuperpeerClient(false);
		config.setSupersuperpeer(false);
		config.setValue("acr", false);

		return config;
	}

	/**
	 * Returns a minimal platform configuration that allows communication.
	 */
	public static IPlatformConfiguration getMinimalComm()
	{
		IPlatformConfiguration config = getMinimal();
		
		// Security & Transports
		config.getExtendedPlatformConfiguration().setSecurity(true); // enable security when remote comm.
		config.getExtendedPlatformConfiguration().setTcpTransport(true);
		config.getExtendedPlatformConfiguration().setWsTransport(true);
		config.getExtendedPlatformConfiguration().setWsPort(-1);
		config.getExtendedPlatformConfiguration().setRelayTransport(true);
//		config.setValue("rtdebug", true);

		// Registry & Awareness
		config.setSuperpeerClient(true);
//		config.addComponent("jadex.platform.service.pawareness.PassiveAwarenessMulticastAgent.class");
//		config.addComponent("jadex.platform.service.pawareness.PassiveAwarenessIntraVMAgent.class");
		config.setAwareness(false);	// disable old awareness

		return config;
	}
}