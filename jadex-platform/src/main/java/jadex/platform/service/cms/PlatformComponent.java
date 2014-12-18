package jadex.platform.service.cms;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeature;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.IPropertiesFeature;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.modelinfo.ModelInfo;
import jadex.bridge.modelinfo.SubcomponentTypeInfo;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.LocalServiceRegistry;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.factory.IPlatformComponentAccess;
import jadex.bridge.service.types.monitoring.IMonitoringEvent;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishTarget;
import jadex.commons.IFilter;
import jadex.commons.IValueFetcher;
import jadex.commons.SReflect;
import jadex.commons.future.CollectionResultListener;
import jadex.commons.future.CounterResultListener;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.kernelbase.ExternalAccess;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *  Standalone platform component implementation.
 */
public class PlatformComponent implements IPlatformComponentAccess, IInternalAccess
{
	//-------- attributes --------
	
	/** The creation info. */
	protected ComponentCreationInfo	info;
	
	/** The features. */
	protected Map<Class<?>, IComponentFeature>	features;
	
	/** The feature instances as list (for reverse execution, cached for speed). */
	protected List<IComponentFeature>	lfeatures;
	
	/** The inited feature instances as list (for shutdown after failed init). */
	protected List<IComponentFeature>	ifeatures;
	
	/** The logger. */
	protected Logger	logger;
	
	/** The failure reason (if any). */
	protected Exception	exception;
	
	//-------- IPlatformComponentAccess interface --------
	
	/**
	 *  Create the component, i.e. instantiate its features.
	 *  
	 *  @param info The component creation info.
	 *  @param facs The component feature factories to be instantiated for this component.
	 */
	public void	create(ComponentCreationInfo info, Collection<IComponentFeatureFactory> facs)
	{
		this.info	= info;		
		this.features	= new LinkedHashMap<Class<?>, IComponentFeature>();
		this.lfeatures	= new ArrayList<IComponentFeature>();

		for(IComponentFeatureFactory fac: facs)
		{
//			System.out.println("fac: "+fac);
			IComponentFeature	instance	= fac.createInstance(getInternalAccess(), info);
			features.put((Class<?>)fac.getType(), instance);
			lfeatures.add(instance);
		}
	}
	
	
	/**
	 *  Perform the initialization of the component.
	 *  Tries to switch to a separate thread for the component as soon as possible.
	 *  
	 *  @return A future to indicate when the initialization is done.
	 */
	public IFuture<Void>	init()
	{
		// Run init on component thread (hack!!! requires that execution feature works before its init)
		IExecutionFeature exe	= getComponentFeature(IExecutionFeature.class);
		return exe.scheduleImmediate(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				ifeatures	= new ArrayList<IComponentFeature>();
				return executeInitOnFeatures(lfeatures.iterator());
			}
		});
	}
	
	/**
	 *  Perform the main execution of the component (if any).
	 *  
	 *  @return A future to indicate when the body is done.
	 */
	public IFuture<Void>	body()
	{
		IExecutionFeature exe	= getComponentFeature(IExecutionFeature.class);
		return exe.scheduleImmediate(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				return executeBodyOnFeatures(lfeatures.iterator());
			}
		});
	}
	
	/**
	 *  Perform the shutdown of the component (if any).
	 *  
	 *  @return A future to indicate when the shutdown is done.
	 */
	public IFuture<Void>	shutdown()
	{
		IExecutionFeature exe	= getComponentFeature(IExecutionFeature.class);
		return exe.scheduleImmediate(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				return executeShutdownOnFeatures(ifeatures!=null ? ifeatures : lfeatures, 0);
			}
		});
	}
	
	/**
	 *  Recursively init the features.
	 */
	protected IFuture<Void>	executeInitOnFeatures(final Iterator<IComponentFeature> features)
	{
		IFuture<Void>	fut	= IFuture.DONE;
		while(fut.isDone() && fut.getException()==null && features.hasNext())
		{
			IComponentFeature	cf	= features.next();
//			if(getComponentIdentifier().getName().indexOf("Interceptor")!=-1)
//				System.out.println("Initing "+cf+" of "+getComponentIdentifier());
			ifeatures.add(cf);
			fut	= cf.init();
		}
		
		if(!fut.isDone())
		{
			final Future<Void>	ret	= new Future<Void>();
			fut.addResultListener(new DelegationResultListener<Void>(ret)
			{
				public void customResultAvailable(Void result)
				{
					executeInitOnFeatures(features).addResultListener(new DelegationResultListener<Void>(ret));
				}
			});
			return ret;
		}
		else
		{
			if(fut.getException()!=null)
			{
//				System.out.println("Initing of "+getComponentIdentifier()+" failed due to "+fut.getException());
				
				// Init failed: remove failed feature.
				ifeatures.remove(ifeatures.size()-1);
			}
			else
			{
//				System.out.println("Initing of "+getComponentIdentifier()+" done.");
				
				// Init succeeded: list no longer needed.
				ifeatures	= null;
			}
			
			return fut;
		}
	}
	
	/**
	 *  Execute feature bodies in parallel.
	 */
	protected IFuture<Void>	executeBodyOnFeatures(final Iterator<IComponentFeature> features)
	{
		List<IFuture<Void>>	undones	= new ArrayList<IFuture<Void>>();
		IFuture<Void>	fut	= IFuture.DONE;
		while(fut.getException()==null && features.hasNext())
		{
			IComponentFeature	cf	= features.next();
//			if(getComponentIdentifier().getName().indexOf("Interceptor")!=-1)
//				System.out.println("Starting "+cf+" of "+getComponentIdentifier());
			fut	= cf.body();
			
			if(!fut.isDone())
			{
				undones.add(fut);
			}
		}
		
		if(fut.getException()==null && !undones.isEmpty())
		{
			final Future<Void>	ret	= new Future<Void>();
			IResultListener<Void>	crl	= new CounterResultListener<Void>(undones.size(), new DelegationResultListener<Void>(ret)
			{
				public void customResultAvailable(Void result)
				{
					Boolean	keepalive	= getModel().getKeepalive(getConfiguration());
					if(keepalive!=null && !keepalive.booleanValue())
					{
						killComponent();
					}
					ret.setResult(null);
				}
			});
			
			for(IFuture<Void> undone: undones)
			{
				undone.addResultListener(crl);
			}
			
			return ret;
		}
		else
		{
			if(fut.getException()==null)
			{
				Boolean	keepalive	= getModel().getKeepalive(getConfiguration());
				if(keepalive!=null && !keepalive.booleanValue())
				{
					killComponent();
				}
			}
			
			return fut;
		}
	}
	
	/**
	 *  Recursively shutdown the features in inverse order.
	 */
	protected IFuture<Void>	executeShutdownOnFeatures(final List<IComponentFeature> features, int cnt)
	{
		IFuture<Void>	fut	= IFuture.DONE;
		while(fut.isDone() && cnt<features.size())
		{
			if(fut.getException()!=null)
			{
				StringWriter	sw	= new StringWriter();
				fut.getException().printStackTrace(new PrintWriter(sw));
				getLogger().warning("Exception during component cleanup of "+getComponentIdentifier()+": "+fut.getException());
				getLogger().info(sw.toString());
			}
			fut	= features.get(features.size()-cnt-1).shutdown();
			cnt++;
		}
		
		if(!fut.isDone())
		{
			final int	fcnt	= cnt;
			final Future<Void>	ret	= new Future<Void>();
			fut.addResultListener(new IResultListener<Void>()
			{
				public void resultAvailable(Void result)
				{
					executeShutdownOnFeatures(features, fcnt+1).addResultListener(new DelegationResultListener<Void>(ret));
				}
				
				public void exceptionOccurred(Exception exception)
				{
					StringWriter	sw	= new StringWriter();
					exception.printStackTrace(new PrintWriter(sw));
					getLogger().warning("Exception during component cleanup of "+getComponentIdentifier()+": "+exception);
					getLogger().info(sw.toString());
					
					executeShutdownOnFeatures(features, fcnt+1).addResultListener(new DelegationResultListener<Void>(ret));
				}
			});
			return ret;
		}
		else
		{
			return IFuture.DONE;
		}
	}
	
	/**
	 *  Get the user view of this platform component.
	 *  
	 *  @return An internal access exposing user operations of the component.
	 */
	public IInternalAccess	getInternalAccess()
	{
		return this;
	}
	
	/**
	 *  Get the exception, if any.
	 *  
	 *  @return The failure reason for use during cleanup, if any.
	 */
	public Exception	getException()
	{
		return exception;
	}
	
	/**
	 *  Get the local platform service registry.
	 *  
	 *  @return The local platform service registry.
	 */
	public LocalServiceRegistry	getServiceRegistry()
	{
		return info.getServiceRegistry();
	}
	
	//-------- IInternalAccess interface --------
	
	/**
	 *  Get the model of the component.
	 *  @return	The model.
	 */
	public IModelInfo getModel()
	{
		return info.getModel();
	}

	/**
	 *  Get the configuration.
	 *  @return	The configuration.
	 */
	public String getConfiguration()
	{
		return info.getConfiguration();
	}
	
	/**
	 *  Get the id of the component.
	 *  @return	The component id.
	 */
	public IComponentIdentifier	getComponentIdentifier()
	{
		return info.getComponentDescription().getName();
	}
	
	/**
	 *  Get the component description.
	 *  @return	The component description.
	 */
	// Todo: hack??? should be internal to CMS!?
	public IComponentDescription	getComponentDescription()
	{
		return info.getComponentDescription();
	}

	/**
	 *  Get the realtime setting.
	 *  @return If is realtime.
	 */
	public boolean isRealtime()
	{
		return info.isRealtime();
	}
	
	/**
	 *  Get the copy setting.
	 *  @return If is copy.
	 */
	public boolean isCopy()
	{
		return info.isCopy();
	}

	/**
	 *  Get a feature of the component.
	 *  @param feature	The type of the feature.
	 *  @return The feature instance.
	 */
	public <T> T	getComponentFeature(Class<? extends T> type)
	{
		if(!features.containsKey(type))
		{
			throw new RuntimeException("No such feature: "+type);
		}
		else
		{
			return type.cast(features.get(type));
		}
	}

	/**
	 *  Kill the component.
	 */
	public IFuture<Map<String, Object>> killComponent()
	{
		return killComponent(null);
	}
	
	/**
	 *  Kill the component.
	 *  @param e The failure reason, if any.
	 */
	public IFuture<Map<String, Object>> killComponent(Exception e)
	{
		// Only remember first exception.
		if(exception==null)
		{
			this.exception	= e;
		}
		IComponentManagementService cms = SServiceProvider.getLocalService(this, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		return cms.destroyComponent(getComponentIdentifier());		
	}
	
	/**
	 *  Get the external access.
	 *  @return The external access.
	 */
	public IExternalAccess getExternalAccess()
	{
		// Todo: shadow access and invalidation
		return new ExternalAccess(this);
	}
	
	/**
	 *  Get the logger.
	 *  @return The logger.
	 */
	public Logger getLogger()
	{
		if(logger==null)
		{
			// todo: problem: loggers can cause memory leaks
			// http://bugs.sun.com/view_bug.do;jsessionid=bbdb212815ddc52fcd1384b468b?bug_id=4811930
			String name = getLoggerName(getComponentIdentifier());
			logger = LogManager.getLogManager().getLogger(name);
			
			// if logger does not already exist, create it
			if(logger==null)
			{
				// Hack!!! Might throw exception in applet / webstart.
				try
				{
					logger = Logger.getLogger(name);
					initLogger(logger);
					logger = new LoggerWrapper(logger, null);	// Todo: clock
					//System.out.println(logger.getParent().getLevel());
				}
				catch(SecurityException e)
				{
					// Hack!!! For applets / webstart use anonymous logger.
					logger = Logger.getAnonymousLogger();
					initLogger(logger);
					logger = new LoggerWrapper(logger, null);	// Todo: clock
				}
			}
		}
		
		return logger;
	}

	/**
	 *  Get the logger name.
	 *  @param cid The component identifier.
	 *  @return The name.
	 */
	public static String getLoggerName(IComponentIdentifier cid)
	{
		// Prepend parent names for nested loggers.
		String	name	= null;
		for(; cid!=null; cid=cid.getParent())
		{
			name	= name==null ? cid.getLocalName() : cid.getLocalName() + "." +name;
		}
		return name;
	}
	
	/**
	 *  Init the logger with capability settings.
	 *  @param logger The logger.
	 */
	protected void initLogger(Logger logger)
	{
		// get logging properties (from ADF)
		// the level of the logger
		// can be Integer or Level
		Object prop = getComponentFeature(IPropertiesFeature.class).getProperty("logging.level");
		Level level = prop!=null? (Level)prop : logger.getParent()!=null && logger.getParent().getLevel()!=null ? logger.getParent().getLevel() : Level.SEVERE;
		logger.setLevel(level);
		
		// if logger should use Handlers of parent (global) logger
		// the global logger has a ConsoleHandler(Level:INFO) by default
		prop = getComponentFeature(IPropertiesFeature.class).getProperty("logging.useParentHandlers");
		if(prop!=null)
		{
			logger.setUseParentHandlers(((Boolean)prop).booleanValue());
		}
			
		// add a ConsoleHandler to the logger to print out
        // logs to the console. Set Level to given property value
		prop = getComponentFeature(IPropertiesFeature.class).getProperty("logging.addConsoleHandler");
		if(prop!=null)
		{
			Handler console;
			/*if[android]
			console = new jadex.commons.android.AndroidHandler();
			 else[android]*/
			console = new ConsoleHandler();
			/* end[android]*/
			
            console.setLevel(Level.parse(prop.toString()));
            logger.addHandler(console);
        }
		
		// Code adapted from code by Ed Komp: http://sourceforge.net/forum/message.php?msg_id=6442905
		// if logger should add a filehandler to capture log data in a file. 
		// The user specifies the directory to contain the log file.
		// $scope.getAgentName() can be used to have agent-specific log files 
		//
		// The directory name can use special patterns defined in the
		// class, java.util.logging.FileHandler, 
		// such as "%h" for the user's home directory.
		// 
		String logfile =	(String)getComponentFeature(IPropertiesFeature.class).getProperty("logging.file");
		if(logfile!=null)
		{
		    try
		    {
			    Handler fh	= new FileHandler(logfile);
		    	fh.setFormatter(new SimpleFormatter());
		    	logger.addHandler(fh);
		    }
		    catch (IOException e)
		    {
		    	System.err.println("I/O Error attempting to create logfile: "
		    		+ logfile + "\n" + e.getMessage());
		    }
		}
		
		// Add further custom log handlers.
		prop = getComponentFeature(IPropertiesFeature.class).getProperty("logging.handlers");
		if(prop!=null)
		{
			if(prop instanceof Handler)
			{
				logger.addHandler((Handler)prop);
			}
			else if(SReflect.isIterable(prop))
			{
				for(Iterator<?> it=SReflect.getIterator(prop); it.hasNext(); )
				{
					Object obj = it.next();
					if(obj instanceof Handler)
					{
						logger.addHandler((Handler)obj);
					}
					else
					{
						logger.warning("Property is not a logging handler: "+obj);
					}
				}
			}
			else
			{
				logger.warning("Property 'logging.handlers' must be Handler or list of handlers: "+prop);
			}
		}
	}
	
	/**
	 *  Get the fetcher.
	 *  @return The fetcher.
	 */
	public IValueFetcher getFetcher()
	{
		// Return a fetcher that tries features first.
		// Todo: better (faster) way than throwing exceptions?
		return new IValueFetcher()
		{
			public Object fetchValue(String name)
			{
				Object	ret	= null;
				boolean	found	= false;
				
				for(int i=lfeatures.size()-1; !found && i>=0; i--)
				{
					try
					{
						ret	= lfeatures.get(i).fetchValue(name);
						found	= true;
					}
					catch(Exception e)
					{
					}
				}
				
				if(ret==null && "$component".equals(name))
				{
					ret	= getInternalAccess();
					found	= true;
				}
				else if(ret==null && "$config".equals(name))
				{
					ret	= getConfiguration();
					found	= true;
				}
				
				if(!found)
				{
					throw new UnsupportedOperationException("Value not found: "+name);
				}
				
				return ret;
			}
			
			public Object fetchValue(String name, Object object)
			{
				Object	ret	= null;
				boolean	found	= false;
				
				for(int i=lfeatures.size()-1; !found && i>=0; i--)
				{
					try
					{
						ret	= lfeatures.get(i).fetchValue(name, object);
						found	= true;
					}
					catch(Exception e)
					{
					}
				}
				
				if(!found)
				{
					throw new UnsupportedOperationException("Value not found: "+name+", "+object);
				}
				
				return ret;
			}
		};
	}
		
	/**
	 *  Get the class loader of the component.
	 */
	public ClassLoader	getClassLoader()
	{
		return ((ModelInfo)getModel()).getClassLoader();
	}
	
	/**
	 *  Subscribe to component events.
	 *  @param filter An optional filter.
	 *  @param initial True, for receiving the current state.
	 */
	public ISubscriptionIntermediateFuture<IMonitoringEvent> subscribeToEvents(IFilter<IMonitoringEvent> filter, boolean initial, PublishEventLevel elm)
	{
		throw new UnsupportedOperationException();
	}
	

	/**
	 *  Publish a monitoring event. This event is automatically send
	 *  to the monitoring service of the platform (if any). 
	 */
	public IFuture<Void> publishEvent(IMonitoringEvent event, PublishTarget pt)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Check if event targets exist.
	 */
	public boolean hasEventTargets(PublishTarget pt, PublishEventLevel pi)
	{
		// Todo!!!
		return false;
//		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Get the children (if any).
	 *  @return The children.
	 */
	public IFuture<IComponentIdentifier[]> getChildren(final String type)
	{
		final Future<IComponentIdentifier[]> ret = new Future<IComponentIdentifier[]>();
		final String filename = getComponentFilename(type);
		
		if(filename==null)
		{
			ret.setException(new IllegalArgumentException("Unknown type: "+type));
		}
		else
		{
			SServiceProvider.getService(this, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM)
				.addResultListener(getComponentFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<IComponentManagementService, IComponentIdentifier[]>(ret)
			{
				public void customResultAvailable(final IComponentManagementService cms)
				{
					// Can use the parent resource identifier as child must depend on parent
					cms.loadComponentModel(filename, getModel().getResourceIdentifier()).addResultListener(getComponentFeature(IExecutionFeature.class).createResultListener(
						new ExceptionDelegationResultListener<IModelInfo, IComponentIdentifier[]>(ret)
					{
						public void customResultAvailable(IModelInfo model)
						{
							final String modelname = model.getFullName();
						
							final Future<Collection<IExternalAccess>>	childaccesses	= new Future<Collection<IExternalAccess>>();
							cms.getChildren(getComponentIdentifier()).addResultListener(new DelegationResultListener<IComponentIdentifier[]>(ret)
							{
								public void customResultAvailable(IComponentIdentifier[] children)
								{
									IResultListener<IExternalAccess>	crl	= new CollectionResultListener<IExternalAccess>(children.length, true,
										new DelegationResultListener<Collection<IExternalAccess>>(childaccesses));
									for(int i=0; !ret.isDone() && i<children.length; i++)
									{
										cms.getExternalAccess(children[i]).addResultListener(crl);
									}
								}
							});
							childaccesses.addResultListener(getComponentFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<Collection<IExternalAccess>, IComponentIdentifier[]>(ret)
							{
								public void customResultAvailable(Collection<IExternalAccess> col)
								{
									List<IComponentIdentifier> res = new ArrayList<IComponentIdentifier>();
									for(Iterator<IExternalAccess> it=col.iterator(); it.hasNext(); )
									{
										IExternalAccess subcomp = it.next();
										if(modelname.equals(subcomp.getModel().getFullName()))
										{
											res.add(subcomp.getComponentIdentifier());
										}
									}
									ret.setResult((IComponentIdentifier[])res.toArray(new IComponentIdentifier[0]));
								}
							}));
						}
					}));
				}	
			}));
		}
		
		return ret;
	}
	
	/**
	 *  Get the file name for a logical type name of a subcomponent of this application.
	 */
	public String getComponentFilename(String type)
	{
		String ret = null;
		SubcomponentTypeInfo[] subcomps = getModel().getSubcomponentTypes();
		for(int i=0; ret==null && i<subcomps.length; i++)
		{
			SubcomponentTypeInfo subct = (SubcomponentTypeInfo)subcomps[i];
			if(subct.getName().equals(type))
				ret = subct.getFilename();
		}
		return ret;
	}
	
	/**
	 *  Get a string representation.
	 */
	public String	toString()
	{
		return getComponentIdentifier().getName();
	}
}