package jadex.bdiv3.features.impl;

import jadex.bdiv3.IBDIClassGenerator;
import jadex.bdiv3.annotation.Capability;
import jadex.bdiv3.annotation.PlanContextCondition;
import jadex.bdiv3.annotation.RawEvent;
import jadex.bdiv3.features.IBDIAgentFeature;
import jadex.bdiv3.model.BDIModel;
import jadex.bdiv3.model.MBelief;
import jadex.bdiv3.model.MCapability;
import jadex.bdiv3.model.MCondition;
import jadex.bdiv3.model.MConfiguration;
import jadex.bdiv3.model.MDeliberation;
import jadex.bdiv3.model.MElement;
import jadex.bdiv3.model.MGoal;
import jadex.bdiv3.model.MParameter;
import jadex.bdiv3.model.MPlan;
import jadex.bdiv3.model.MTrigger;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bdiv3.runtime.IBeliefListener;
import jadex.bdiv3.runtime.ICapability;
import jadex.bdiv3.runtime.IGoal;
import jadex.bdiv3.runtime.IGoal.GoalLifecycleState;
import jadex.bdiv3.runtime.IPlanListener;
import jadex.bdiv3.runtime.impl.BeliefInfo;
import jadex.bdiv3.runtime.impl.BodyAborted;
import jadex.bdiv3.runtime.impl.CapabilityWrapper;
import jadex.bdiv3.runtime.impl.GoalFailureException;
import jadex.bdiv3.runtime.impl.GoalInfo;
import jadex.bdiv3.runtime.impl.InvocationInfo;
import jadex.bdiv3.runtime.impl.PlanInfo;
import jadex.bdiv3.runtime.impl.RCapability;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RPlan;
import jadex.bdiv3.runtime.impl.RPlan.PlanLifecycleState;
import jadex.bdiv3.runtime.impl.RProcessableElement;
import jadex.bdiv3.runtime.wrappers.ListWrapper;
import jadex.bdiv3.runtime.wrappers.MapWrapper;
import jadex.bdiv3.runtime.wrappers.SetWrapper;
import jadex.bridge.ComponentTerminatedException;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.impl.AbstractComponentFeature;
import jadex.bridge.component.impl.ComponentFeatureFactory;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.CheckNotNull;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.bridge.service.types.monitoring.IMonitoringEvent;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishTarget;
import jadex.bridge.service.types.monitoring.MonitoringEvent;
import jadex.commons.FieldInfo;
import jadex.commons.IResultCommand;
import jadex.commons.MethodInfo;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.SimpleParameterGuesser;
import jadex.commons.Tuple2;
import jadex.commons.beans.PropertyChangeEvent;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.javaparser.SJavaParser;
import jadex.micro.IPojoMicroAgent;
import jadex.micro.MicroModel;
import jadex.micro.annotation.Agent;
import jadex.micro.features.IMicroLifecycleFeature;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.MethodCondition;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleSystem;
import jadex.rules.eca.annotations.CombinedCondition;
import jadex.rules.eca.annotations.Event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * 
 */
public class BDIAgentFeature extends AbstractComponentFeature implements IBDIAgentFeature
{
	public static final IComponentFeatureFactory FACTORY = new ComponentFeatureFactory(IBDIAgentFeature.class, BDIAgentFeature.class, new Class[]{IMicroLifecycleFeature.class}, null);
	
	/** The bdi model. */
	protected BDIModel bdimodel;
	
	/** The rule system. */
	protected RuleSystem rulesystem;
	
	/** The bdi state. */
	protected RCapability capa;
	
	/** Is the agent inited and allowed to execute rules? */
	protected boolean	inited;
	
	//-------- constructors --------
	
	/**
	 *  Factory method constructor for instance level.
	 */
	public BDIAgentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
		
		Object pojo = getComponent().getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent();
		this.bdimodel = (BDIModel)getComponent().getModel().getRawModel();
		this.capa = new RCapability(bdimodel.getCapability());
		this.rulesystem = new RuleSystem(pojo);
		injectAgent(getComponent(), pojo, bdimodel, null);
	}

	/**
	 *  Initialize the feature.
	 *  Empty implementation that can be overridden.
	 */
	public IFuture<Void> init()
	{
		startBehavior();
		return IFuture.DONE;
	}
	
	//-------- internal method used for rewriting field access -------- 
	
	/**
	 *  Add an entry to the init calls.
	 *  
	 *  @param obj object instance that owns the field __initargs
	 *  @param clazz Class definition of the obj object
	 *  @param argtypes Signature of the init method
	 *  @param args Actual argument values for the init method
	 */
	public static void	addInitArgs(Object obj, Class<?> clazz, Class<?>[] argtypes, Object[] args)
	{
		try
		{
			Field f	= clazz.getDeclaredField("__initargs");
//				System.out.println(f+", "+SUtil.arrayToString(args));
			f.setAccessible(true);
			List<Tuple2<Class<?>[], Object[]>> initcalls	= (List<Tuple2<Class<?>[], Object[]>>)f.get(obj);
			if(initcalls==null)
			{
				initcalls	= new ArrayList<Tuple2<Class<?>[], Object[]>>();
				f.set(obj, initcalls);
			}
			initcalls.add(new Tuple2<Class<?>[], Object[]>(argtypes, args));
		}
		catch(Exception e)
		{
			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
		}
	}
	
	/**
	 *  Get the init calls.
	 *  Cleans the initargs field on return.
	 */
	public static List<Tuple2<Class<?>[], Object[]>>	getInitCalls(Object obj, Class<?> clazz)
	{
		try
		{
			Field f	= clazz.getDeclaredField("__initargs");
			f.setAccessible(true);
			List<Tuple2<Class<?>[], Object[]>> initcalls	= (List<Tuple2<Class<?>[], Object[]>>)f.get(obj);
			f.set(obj, null);
			return initcalls;
		}
		catch(Exception e)
		{
			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
		}
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as field access.
	 */
	protected void writeField(Object val, String belname, String fieldname, Object obj)
	{
		writeField(val, belname, fieldname, obj, new EventType(ChangeEvent.BELIEFCHANGED+"."+belname), new EventType(ChangeEvent.FACTCHANGED+"."+belname));
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as field access.
	 */
	protected void writeField(Object val, String belname, String fieldname, Object obj, EventType ev1, EventType ev2)
	{
		assert isComponentThread();
		
		// todo: support for belief sets (un/observe values? insert mappers when setting value etc.
		
		try
		{
//				System.out.println("write: "+val+" "+fieldname+" "+obj);
//			BDIAgentInterpreter ip = (BDIAgentInterpreter)getInterpreter();
			RuleSystem rs = getComponent().getComponentFeature(IBDIAgentFeature.class).getRuleSystem();

			Object oldval = setFieldValue(obj, fieldname, val);
			
			// unobserve old value for property changes
			rs.unobserveObject(oldval);

			MBelief	mbel = ((MCapability)getComponent().getComponentFeature(IBDIAgentFeature.class).getCapability().getModelElement()).getBelief(belname);
		
			if(!SUtil.equals(val, oldval))
			{
				publishToolBeliefEvent(getComponent(), mbel);
//					rs.addEvent(new Event(ChangeEvent.BELIEFCHANGED+"."+belname, val));
				rs.addEvent(new jadex.rules.eca.Event(ev1, new ChangeInfo<Object>(val, oldval, null)));
				// execute rulesystem immediately to ensure that variable values are not changed afterwards
				rs.processAllEvents(); 
			}
			
			// observe new value for property changes
//				observeValue(rs, val, ip, ChangeEvent.FACTCHANGED+"."+belname, mbel);
			observeValue(rs, val, getComponent(), ev2, mbel);
			
			// initiate a step to reevaluate the conditions
			getComponent().getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep()
			{
				public IFuture execute(IInternalAccess ia)
				{
					return IFuture.DONE;
				}
			});
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Set the value of a field.
	 *  @param obj The object.
	 *  @param fieldname The name of the field.
	 *  @return The old field value.
	 */
	protected static Object setFieldValue(Object obj, String fieldname, Object val) throws IllegalAccessException
	{
		Tuple2<Field, Object> res = findFieldWithOuterClass(obj, fieldname);
		Field f = res.getFirstEntity();
		if(f==null)
			throw new RuntimeException("Field not found: "+fieldname);
		
		Object tmp = res.getSecondEntity();
		f.setAccessible(true);
		Object oldval = f.get(tmp);
		f.set(tmp, val);
	
		return oldval;
	}
	
	/**
	 * 
	 * @param obj
	 * @param fieldname
	 * @return
	 */
	protected static Tuple2<Field, Object> findFieldWithOuterClass(Object obj, String fieldname)
	{
		Field f = null;
		Object tmp = obj;
		while(f==null && tmp!=null)
		{
			f = findFieldWithSuperclass(tmp.getClass(), fieldname);
			if(f==null)
			{
				try
				{
					Field fi = tmp.getClass().getDeclaredField("this$0");
					fi.setAccessible(true);
					tmp = fi.get(tmp);
				}
				catch(Exception e)
				{
//						e.printStackTrace();
					tmp=null;
				}
			}
		}
		return new Tuple2<Field, Object>(f, tmp);
	}
	
	/**
	 * 
	 * @param cl
	 * @param fieldname
	 * @return
	 */
	protected static Field findFieldWithSuperclass(Class<?> cl, String fieldname)
	{
		Field ret = null;
		while(ret==null && !Object.class.equals(cl))
		{
			try
			{
				ret = cl.getDeclaredField(fieldname);
			}
			catch(Exception e)
			{
				cl = cl.getSuperclass();
			}
		}
		return ret;
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as field access.
	 */
	public static void writeField(Object val, String fieldname, Object obj, IInternalAccess agent)
	{
		System.out.println("write: "+val+" "+fieldname+" "+obj+" "+agent);
		
		// This is the case in inner classes
		if(agent==null)
		{
			try
			{
				Tuple2<Field, Object> res = findFieldWithOuterClass(obj, "__agent");
//					System.out.println("res: "+res);
				agent = (IInternalAccess)res.getFirstEntity().get(res.getSecondEntity());
			}
			catch(RuntimeException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		String belname	= getBeliefName(obj, fieldname);

//		BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		MBelief mbel = agent.getComponentFeature(IBDIAgentFeature.class).getBDIModel().getCapability().getBelief(belname);
		
		// Wrap collections of multi beliefs (if not already a wrapper)
		if(mbel.isMulti(agent.getClassLoader()))
		{
			String addev = ChangeEvent.FACTADDED+"."+belname;
			String remev = ChangeEvent.FACTREMOVED+"."+belname;
			String chev = ChangeEvent.FACTCHANGED+"."+belname;
			if(val instanceof List && !(val instanceof jadex.commons.collection.wrappers.ListWrapper))
			{
				val = new ListWrapper((List<?>)val, agent, addev, remev, chev, mbel);
			}
			else if(val instanceof Set && !(val instanceof jadex.commons.collection.wrappers.SetWrapper))
			{
				val = new SetWrapper((Set<?>)val, agent, addev, remev, chev, mbel);
			}
			else if(val instanceof Map && !(val instanceof jadex.commons.collection.wrappers.MapWrapper))
			{
				val = new MapWrapper((Map<?,?>)val, agent, addev, remev, chev, mbel);
			}
		}
		
		// agent is not null any more due to deferred exe of init expressions but rules are
		// available only after startBehavior
		if(((BDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class)).isInited())
		{
			((BDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class)).writeField(val, belname, fieldname, obj);
		}
		else
		{
			// In init set field immediately but throw events later, when agent is available.
			
			try
			{
				setFieldValue(obj, fieldname, val);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			synchronized(initwrites)
			{
				List<Object[]> inits = initwrites.get(agent);
				if(inits==null)
				{
					inits = new ArrayList<Object[]>();
					initwrites.put(agent, inits);
				}
				inits.add(new Object[]{val, belname});
			}
		}
	}
	
	/** Saved init writes. */
	protected final static Map<Object, List<Object[]>> initwrites = new HashMap<Object, List<Object[]>>();
	
	/**
	 * 
	 */
	public static void performInitWrites(IInternalAccess agent)
	{
		synchronized(initwrites)
		{
			List<Object[]> writes = initwrites.remove(agent);
			if(writes!=null)
			{
				for(Object[] write: writes)
				{
					System.out.println("initwrite: "+write[0]+" "+write[1]+" "+write[2]);
//					agent.writeField(write[0], (String)write[1], write[2]);
//					BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
					RuleSystem rs = agent.getComponentFeature(IBDIAgentFeature.class).getRuleSystem();
					final String belname = (String)write[1];
					Object val = write[0];
//						rs.addEvent(new Event(ChangeEvent.BELIEFCHANGED+"."+belname, val));
					rs.addEvent(new jadex.rules.eca.Event(ChangeEvent.BELIEFCHANGED+"."+belname, new ChangeInfo<Object>(val, null, null)));
					MBelief	mbel = ((MCapability)agent.getComponentFeature(IBDIAgentFeature.class).getCapability().getModelElement()).getBelief(belname);
					observeValue(rs, val, agent, ChangeEvent.FACTCHANGED+"."+belname, mbel);
				}
			}
		}
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as array access.
	 */
	// todo: allow init writes in constructor also for arrays
	public static void writeArrayField(Object array, final int index, Object val, Object agentobj, String fieldname)
	{
		// This is the case in inner classes
		IInternalAccess agent = null;
		if(agentobj instanceof IInternalAccess)
		{
			agent = (IInternalAccess)agentobj;
		}
		else
		{
			try
			{
				Tuple2<Field, Object> res = findFieldWithOuterClass(agentobj, "__agent");
//					System.out.println("res: "+res);
				agent = (IInternalAccess)res.getFirstEntity().get(res.getSecondEntity());
			}
			catch(RuntimeException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
//		final BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		
		assert agent.getComponentFeature(IExecutionFeature.class).isComponentThread();

		// Test if array store is really a belief store instruction by
		// looking up the current belief value and comparing it with the
		// array that is written
		
		String belname	= getBeliefName(agentobj, fieldname);
		MBelief	mbel = ((MCapability)agent.getComponentFeature(IBDIAgentFeature.class).getCapability().getModelElement()).getBelief(belname);
		
		Object curval = mbel.getValue(agent);
		boolean isbeliefwrite = curval==array;
		
		RuleSystem rs = agent.getComponentFeature(IBDIAgentFeature.class).getRuleSystem();
//			System.out.println("write array index: "+val+" "+index+" "+array+" "+agent+" "+fieldname);
		
		Object oldval = null;
		if(isbeliefwrite)
		{
			oldval = Array.get(array, index);
			rs.unobserveObject(oldval);	
		}
		
		Class<?> ct = array.getClass().getComponentType();
		if(boolean.class.equals(ct))
		{
			val = ((Integer)val)==1? Boolean.TRUE: Boolean.FALSE;
		}
		else if(byte.class.equals(ct))
		{
//				val = new Byte(((Integer)val).byteValue());
			val = Byte.valueOf(((Integer)val).byteValue());
		}
		Array.set(array, index, val);
		
		if(isbeliefwrite)
		{
			observeValue(rs, val, agent, new EventType(new String[]{ChangeEvent.FACTCHANGED, belname}), mbel);
			
			if(!SUtil.equals(val, oldval))
			{
				publishToolBeliefEvent(agent, mbel);

				jadex.rules.eca.Event ev = new jadex.rules.eca.Event(new EventType(new String[]{ChangeEvent.FACTCHANGED, belname}), new ChangeInfo<Object>(val, oldval, Integer.valueOf(index))); // todo: index
				rs.addEvent(ev);
				// execute rulesystem immediately to ensure that variable values are not changed afterwards
				rs.processAllEvents(); 
			}
		}
	}
	
	/**
	 *  Unobserving an old belief value.
	 *  @param agent The agent.
	 *  @param belname The belief name.
	 */
	public static void unobserveValue(IInternalAccess agent, final String belname)
	{
//			System.out.println("unobserve: "+agent+" "+belname);
		
		try
		{
			Object pojo = ((IPojoMicroAgent)agent).getPojoAgent();
		
			Method getter = pojo.getClass().getMethod("get"+belname.substring(0,1).toUpperCase()+belname.substring(1), new Class[0]);
			Object oldval = getter.invoke(pojo, new Object[0]);
		
			RuleSystem rs = agent.getComponentFeature(IBDIAgentFeature.class).getRuleSystem();
			rs.unobserveObject(oldval);	
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public static void observeValue(RuleSystem rs, Object val, final IInternalAccess agent, final String etype, final MBelief mbel)
	{
		observeValue(rs, val, agent, new EventType(etype), mbel);
	}
	
	/**
	 * 
	 */
	public static void observeValue(final RuleSystem rs, final Object val, final IInternalAccess agent, final EventType etype, final MBelief mbel)
	{
		assert agent.getComponentFeature(IExecutionFeature.class).isComponentThread();

		if(val!=null)
		{
			rs.observeObject(val, true, false, new IResultCommand<IFuture<Void>, PropertyChangeEvent>()
			{
				public IFuture<Void> execute(final PropertyChangeEvent event)
				{
					final Future<Void> ret = new Future<Void>();
					try
					{
						IFuture<Void> fut = agent.getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
						{
							public IFuture<Void> execute(IInternalAccess ia)
							{
								publishToolBeliefEvent(agent, mbel);
								
		//						Event ev = new Event(ChangeEvent.FACTCHANGED+"."+fieldname+"."+event.getPropertyName(), event.getNewValue());
		//						Event ev = new Event(ChangeEvent.FACTCHANGED+"."+fieldname, event.getNewValue());
								jadex.rules.eca.Event ev = new jadex.rules.eca.Event(etype, new ChangeInfo<Object>(event.getNewValue(), event.getOldValue(), null));
								rs.addEvent(ev);
								return IFuture.DONE;
//									return new Future<IEvent>(ev);
							}
						});
						fut.addResultListener(new DelegationResultListener<Void>(ret)
						{
							public void exceptionOccurred(Exception exception)
							{
								if(exception instanceof ComponentTerminatedException)
								{
//										System.out.println("Ex in observe: "+exception.getMessage());
									rs.unobserveObject(val);
									ret.setResult(null);
								}
								else
								{
									super.exceptionOccurred(exception);
								}
							}
						});
					}
					catch(Exception e)
					{
						if(!(e instanceof ComponentTerminatedException))
							System.out.println("Ex in observe: "+e.getMessage());
						rs.unobserveObject(val);
						ret.setResult(null);
					}
					return ret;
				}
			});
		}
	}

	/**
	 *  Get the value of an abstract belief.
	 */
	public Object	getAbstractBeliefValue(String capa, String name, Class<?> type)
	{
//			System.out.println("getAbstractBeliefValue(): "+capa+BDIAgentInterpreter.CAPABILITY_SEPARATOR+name+", "+type);
		BDIModel bdimodel = (BDIModel)getComponent().getComponentFeature(IBDIAgentFeature.class).getBDIModel();
		String	belname	= bdimodel.getBeliefMappings().get(capa+MElement.CAPABILITY_SEPARATOR+name);
		if(belname==null)
		{
			throw new RuntimeException("No mapping for abstract belief: "+capa+MElement.CAPABILITY_SEPARATOR+name);
		}
		MBelief	bel	= bdimodel.getCapability().getBelief(belname);
		Object	ret	= bel.getValue(getComponent());
		
		if(ret==null)
		{
			if(type.equals(boolean.class))
			{
				ret	= Boolean.FALSE;
			}
			else if(type.equals(char.class))
			{
				ret	= Character.valueOf((char)0);
			}
			else if(SReflect.getWrappedType(type)!=type)	// Number type
			{
				ret	= Integer.valueOf(0);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Set the value of an abstract belief.
	 */
	public void	setAbstractBeliefValue(String capa, String name, Object value)
	{
//			System.out.println("setAbstractBeliefValue(): "+capa+BDIAgentInterpreter.CAPABILITY_SEPARATOR+name);
		BDIModel bdimodel = (BDIModel)getComponent().getComponentFeature(IBDIAgentFeature.class).getBDIModel();
		String	belname	= bdimodel.getBeliefMappings().get(capa+MElement.CAPABILITY_SEPARATOR+name);
		if(belname==null)
		{
			throw new RuntimeException("No mapping for abstract belief: "+capa+MElement.CAPABILITY_SEPARATOR+name);
		}
		MBelief	mbel = bdimodel.getCapability().getBelief(belname);

		// Maybe unobserve old value
		Object	old	= mbel.getValue(getComponent());

		boolean	field = mbel.setValue(getComponent(), value);
		
		if(field)
		{
//			BDIAgentInterpreter ip = (BDIAgentInterpreter)getInterpreter();
			RuleSystem rs = getComponent().getComponentFeature(IBDIAgentFeature.class).getRuleSystem();
			rs.unobserveObject(old);	
			createChangeEvent(value, old, null, getComponent(), mbel.getName());
			observeValue(rs, value, getComponent(), ChangeEvent.FACTCHANGED+"."+mbel.getName(), mbel);
		}
	}
	
//		public static void createChangeEvent(Object val, final BDIAgent agent, final String belname)
//		{
//			createChangeEvent(val, null, null, agent, belname);
//		}
	
	/**
	 *  Caution: this method is used from byte engineered code, change signature with caution
	 * 
	 *  Create a belief changed event.
	 *  @param val The new value.
	 *  @param agent The agent.
	 *  @param belname The belief name.
	 */
	public static void createChangeEvent(Object val, Object oldval, Object info, final IInternalAccess agent, final String belname)
//		public static void createChangeEvent(Object val, final BDIAgent agent, MBelief mbel)
	{
//			System.out.println("createEv: "+val+" "+agent+" "+belname);
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		
		MBelief mbel = agent.getComponentFeature(IBDIAgentFeature.class).getBDIModel().getCapability().getBelief(belname);
		
		RuleSystem rs = agent.getComponentFeature(IBDIAgentFeature.class).getRuleSystem();
		rs.addEvent(new jadex.rules.eca.Event(ChangeEvent.BELIEFCHANGED+"."+belname, new ChangeInfo<Object>(val, oldval, info)));
		
		publishToolBeliefEvent(agent, mbel);
	}
	
	/**
	 * 
	 */
	public static void publishToolBeliefEvent(IInternalAccess ia, MBelief mbel)//, String evtype)
	{
		if(mbel!=null && ia.hasEventTargets(PublishTarget.TOSUBSCRIBERS, PublishEventLevel.FINE))
		{
			long time = System.currentTimeMillis();//getClockService().getTime();
			MonitoringEvent mev = new MonitoringEvent();
			mev.setSourceIdentifier(ia.getComponentIdentifier());
			mev.setTime(time);
			
			BeliefInfo info = BeliefInfo.createBeliefInfo(ia, mbel, ia.getClassLoader());
//				mev.setType(evtype+"."+IMonitoringEvent.SOURCE_CATEGORY_FACT);
			mev.setType(IMonitoringEvent.EVENT_TYPE_MODIFICATION+"."+IMonitoringEvent.SOURCE_CATEGORY_FACT);
//				mev.setProperty("sourcename", element.toString());
			mev.setProperty("sourcetype", info.getType());
			mev.setProperty("details", info);
			mev.setLevel(PublishEventLevel.FINE);
			
			ia.publishEvent(mev, PublishTarget.TOSUBSCRIBERS);
		}
	}
	
	/**
	 * 
	 */
	protected static String getBeliefName(Object obj, String fieldname)
	{
		String	gn	= null;
		try
		{
			Field	gnf	= obj.getClass().getField("__globalname");
			gnf.setAccessible(true);
			gn	= (String)gnf.get(obj);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		String belname	= gn!=null ? gn + MElement.CAPABILITY_SEPARATOR + fieldname : fieldname;
		return belname;
	}
	
	//-------- methods for goal/plan parameter rewrites --------
	
	/**
	 *  Method that is called automatically when a parameter 
	 *  is written as field access.
	 */
	public static void writeParameterField(Object val, String fieldname, Object obj, IInternalAccess agent)
	{
//			System.out.println("write: "+val+" "+fieldname+" "+obj+" "+agent);
		
		// This is the case in inner classes
		if(agent==null)
		{
			try
			{
				Tuple2<Field, Object> res = findFieldWithOuterClass(obj, "__agent");
//						System.out.println("res: "+res);
				agent = (IInternalAccess)res.getFirstEntity().get(res.getSecondEntity());
			}
			catch(RuntimeException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}

//		BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		String elemname = obj.getClass().getName();
		MGoal mgoal = agent.getComponentFeature(IBDIAgentFeature.class).getBDIModel().getCapability().getGoal(elemname);
		
//			String paramname = elemname+"."+fieldname; // ?

		if(mgoal!=null)
		{
			MParameter mparam = mgoal.getParameter(fieldname);
			if(mparam!=null)
			{
				// Wrap collections of multi beliefs (if not already a wrapper)
				if(mparam.isMulti(agent.getClassLoader()))
				{
					EventType addev = new EventType(new String[]{ChangeEvent.VALUEADDED, elemname, fieldname});
					EventType remev = new EventType(new String[]{ChangeEvent.VALUEREMOVED, elemname, fieldname});
					EventType chev = new EventType(new String[]{ChangeEvent.VALUECHANGED, elemname, fieldname});
					if(val instanceof List && !(val instanceof ListWrapper))
					{
						val = new ListWrapper((List<?>)val, agent, addev, remev, chev, null);
					}
					else if(val instanceof Set && !(val instanceof SetWrapper))
					{
						val = new SetWrapper((Set<?>)val, agent, addev, remev, chev, null);
					}
					else if(val instanceof Map && !(val instanceof MapWrapper))
					{
						val = new MapWrapper((Map<?,?>)val, agent, addev, remev, chev, null);
					}
				}
			}
		}
		
		// agent is not null any more due to deferred exe of init expressions but rules are
		// available only after startBehavior
		if(((BDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class)).isInited())
		{
			EventType chev1 = new EventType(new String[]{ChangeEvent.PARAMETERCHANGED, elemname, fieldname});
			EventType chev2 = new EventType(new String[]{ChangeEvent.VALUECHANGED, elemname, fieldname});
			((BDIAgentFeature)agent.getComponentFeature(IBDIAgentFeature.class)).writeField(val, null, fieldname, obj, chev1, chev2);
		}
//				else
//				{
//					// In init set field immediately but throw events later, when agent is available.
//					
//					try
//					{
//						setFieldValue(obj, fieldname, val);
//					}
//					catch(Exception e)
//					{
//						e.printStackTrace();
//						throw new RuntimeException(e);
//					}
//					synchronized(initwrites)
//					{
//						List<Object[]> inits = initwrites.get(agent);
//						if(inits==null)
//						{
//							inits = new ArrayList<Object[]>();
//							initwrites.put(agent, inits);
//						}
//						inits.add(new Object[]{val, belname});
//					}
//				}
	}
	
	/**
	 *  Method that is called automatically when a belief 
	 *  is written as array access.
	 */
	// todo: allow init writes in constructor also for arrays
	public static void writeArrayParameterField(Object array, final int index, Object val, Object agentobj, String fieldname)
	{
		// This is the case in inner classes
		IInternalAccess agent = null;
		if(agentobj instanceof IInternalAccess)
		{
			agent = (IInternalAccess)agentobj;
		}
		else
		{
			try
			{
				Tuple2<Field, Object> res = findFieldWithOuterClass(agentobj, "__agent");
//					System.out.println("res: "+res);
				agent = (IInternalAccess)res.getFirstEntity().get(res.getSecondEntity());
			}
			catch(RuntimeException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
//		final BDIAgentInterpreter ip = (BDIAgentInterpreter)agent.getInterpreter();
		
		assert agent.getComponentFeature(IExecutionFeature.class).isComponentThread();

		// Test if array store is really a belief store instruction by
		// looking up the current belief value and comparing it with the
		// array that is written
		
		boolean isparamwrite = false;
		
		MGoal mgoal = ((MCapability)agent.getComponentFeature(IBDIAgentFeature.class).getCapability().getModelElement()).getGoal(agentobj.getClass().getName());
		if(mgoal!=null)
		{
			MParameter mparam = mgoal.getParameter(fieldname);
			if(mparam!=null)
			{
				Object curval = mparam.getValue(agentobj, agent.getClassLoader());
				isparamwrite = curval==array;
			}
		}
		RuleSystem rs = agent.getComponentFeature(IBDIAgentFeature.class).getRuleSystem();
//			System.out.println("write array index: "+val+" "+index+" "+array+" "+agent+" "+fieldname);
		
		Object oldval = null;
		if(isparamwrite)
		{
			oldval = Array.get(array, index);
			rs.unobserveObject(oldval);	
		}
		
		Class<?> ct = array.getClass().getComponentType();
		if(boolean.class.equals(ct))
		{
			val = ((Integer)val)==1? Boolean.TRUE: Boolean.FALSE;
		}
		else if(byte.class.equals(ct))
		{
//				val = new Byte(((Integer)val).byteValue());
			val = Byte.valueOf(((Integer)val).byteValue());
		}
		Array.set(array, index, val);
		
		if(isparamwrite)
		{
			
			if(!SUtil.equals(val, oldval))
			{
				jadex.rules.eca.Event ev = new jadex.rules.eca.Event(new EventType(new String[]{ChangeEvent.VALUECHANGED, mgoal.getName(), fieldname}), new ChangeInfo<Object>(val, oldval, Integer.valueOf(index)));
				rs.addEvent(ev);
				// execute rulesystem immediately to ensure that variable values are not changed afterwards
				rs.processAllEvents(); 
			}
		}
	}
	
	/**
	 * 
	 */
	protected boolean isComponentThread()
	{
		return getComponent().getComponentFeature(IExecutionFeature.class).isComponentThread();
	}
	
//	/**
//	 *  Get the inited.
//	 *  @return The inited.
//	 */
//	public boolean isInited()
//	{
//		return inited;
//	}
	
//			this.bdimodel = model;
//			this.capa = new RCapability(bdimodel.getCapability());
		
//	/**
//	 *  Create the agent.
//	 */
//	protected MicroAgent createAgent(Class<?> agentclass, MicroModel model, IPersistInfo pinfo) throws Exception
//	{
//		ASMBDIClassGenerator.checkEnhanced(agentclass);
//		
//		MicroAgent ret;
//		final Object agent = agentclass.newInstance();
//		if(agent instanceof MicroAgent)
//		{
//			ret = (MicroAgent)agent;
//			ret.init(BDIAgentInterpreter.this);
//		}
//		else // if pojoagent
//		{
//			PojoBDIAgent pa = new PojoBDIAgent();
//			pa.init(this, agent);
//			ret = pa;
//
//			injectAgent(pa, agent, model, null);
//		}
//		
//		// Init rule system
//		this.rulesystem = new RuleSystem(agent);
//		
//		return ret;
//	}
		
	/**
	 *  Inject the agent into annotated fields.
	 */
	protected void	injectAgent(IInternalAccess pa, Object agent, MicroModel model, String globalname)
	{
		FieldInfo[] fields = model.getAgentInjections();
		for(int i=0; i<fields.length; i++)
		{
			try
			{
				Field f = fields[i].getField(getComponent().getClassLoader());
				if(SReflect.isSupertype(f.getType(), ICapability.class))
				{
					f.setAccessible(true);
					f.set(agent, new CapabilityWrapper(pa, agent, globalname));						
				}
				else
				{
					f.setAccessible(true);
					f.set(agent, pa);
				}
			}
			catch(Exception e)
			{
				pa.getLogger().warning("Agent injection failed: "+e);
			}
		}
	
		// Additionally inject hidden agent fields
		Class<?> agcl = agent.getClass();
		while(agcl.isAnnotationPresent(Agent.class)
			|| agcl.isAnnotationPresent(Capability.class))
		{
			try
			{
				Field field = agcl.getDeclaredField("__agent");
				field.setAccessible(true);
				field.set(agent, pa);
				
				field = agcl.getDeclaredField("__globalname");
				field.setAccessible(true);
				field.set(agent, globalname);
				agcl = agcl.getSuperclass();

			}
			catch(Exception e)
			{
				pa.getLogger().warning("Hidden agent injection failed: "+e);
				break;
			}
		}
		// Add hidden agent field also to contained inner classes (goals, plans)
		// Does not work as would have to be inserted in each object of that type :-(
//			Class<?>[] inners = agent.getClass().getDeclaredClasses();
//			if(inners!=null)
//			{
//				for(Class<?> icl: inners)
//				{
//					try
//					{
//						Field field = icl.getDeclaredField("__agent");
//						field.setAccessible(true);
//						field.set(icl, pa);
//					}
//					catch(Exception e)
//					{
//						e.printStackTrace();
//					}
//				}
//			}
	}
		
	/**
	 *  Get a capability pojo object.
	 */
	public Object	getCapabilityObject(String name)
	{
//		Object	ret	= ((PojoBDIAgent)microagent).getPojoAgent();
		Object ret = getComponent().getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent();
		if(name!=null)
		{
			StringTokenizer	stok	= new StringTokenizer(name, MElement.CAPABILITY_SEPARATOR);
			while(stok.hasMoreTokens())
			{
				name	= stok.nextToken();
				
				boolean found = false;
				Class<?> cl = ret.getClass();
				while(!found && !Object.class.equals(cl))
				{
					try
					{
						Field	f	= cl.getDeclaredField(name);
						f.setAccessible(true);
						ret	= f.get(ret);
						found = true;
						break;
					}
					catch(Exception e)
					{
						cl	= cl.getSuperclass();
					}
				}
				if(!found)
					throw new RuntimeException("Could not fetch capability object: "+name);
			}
		}
		return ret;
	}
		
	/**
	 * 	Adapt element for use in inner capabilities.
	 *  @param obj	The object to adapt (e.g. a change event)
	 *  @param capa	The capability name or null for agent.
	 */
	protected Object adaptToCapability(Object obj, String capa)
	{
		if(obj instanceof ChangeEvent && capa!=null)
		{
			ChangeEvent	ce	= (ChangeEvent)obj;
			String	source	= (String)ce.getSource();
			if(source!=null)
			{
				// For concrete belief just strip capability prefix.
				if(source.startsWith(capa))
				{
					source	= source.substring(capa.length()+1);
				}
				// For abstract belief find corresponding mapping.
				else
				{
					Map<String, String>	map	= getBDIModel().getBeliefMappings();
					for(String target: map.keySet())
					{
						if(source.equals(map.get(target)))
						{
							int	idx2	= target.lastIndexOf(MElement.CAPABILITY_SEPARATOR);
							String	capa2	= target.substring(0, idx2);
							if(capa.equals(capa2))
							{
								source	= target.substring(capa.length()+1);
								break;
							}
						}
					}
				}
			}
			
			ChangeEvent	ce2	= new ChangeEvent();
			ce2.setType(ce.getType());
			ce2.setSource(source);
			ce2.setValue(ce.getValue());
			obj	= ce2;
		}
		return obj;
	}
		
//	/**
//	 *  Get the component fetcher.
//	 */
//	protected IResultCommand<Object, Class<?>>	getComponentFetcher()
//	{
//		return new IResultCommand<Object, Class<?>>()
//		{
//			public Object execute(Class<?> type)
//			{
//				Object ret	= null;
//				if(SReflect.isSupertype(type, microagent.getClass()))
//				{
//					ret	= microagent;
//				}
//				else if(microagent instanceof IPojoMicroAgent
//					&& SReflect.isSupertype(type, ((IPojoMicroAgent)microagent).getPojoAgent().getClass()))
//				{
//					ret	= ((IPojoMicroAgent)microagent).getPojoAgent();
//				}
//				return ret;
//			}
//		};
//	}
		
//	/**
//	 *  Create a service implementation from description.
//	 */
//	protected Object createServiceImplementation(ProvidedServiceInfo info, IModelInfo model)
//	{
//		// Support special case that BDI should implement provided service with plans.
//		Object ret = null;
//		ProvidedServiceImplementation impl = info.getImplementation();
//		if(impl!=null && impl.getClazz()!=null && impl.getClazz().getType(getClassLoader()).equals(BDIAgent.class))
//		{
//			Class<?> iface = info.getType().getType(getComponent().getClassLoader());
//			ret = Proxy.newProxyInstance(getComponent().getClassLoader(), new Class[]{iface}, 
//				new BDIServiceInvocationHandler(getComponent(), iface));
//		}
//		else
//		{
//			ret = super.createServiceImplementation(info, model);
//		}
//		return ret;
//	}
		
//	/**
//	 *  Init a service.
//	 */
//	protected IFuture<Void> initService(ProvidedServiceInfo info, IModelInfo model, IResultCommand<Object, Class<?>> componentfetcher)
//	{
//		Future<Void>	ret	= new Future<Void>();
//		
//		int i	= info.getName()!=null ? info.getName().indexOf(MElement.CAPABILITY_SEPARATOR) : -1;
////		Object	ocapa	= ((PojoBDIAgent)microagent).getPojoAgent();
//		Object ocapa = getComponent().getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent();
//		String	capa	= null;
//		final IValueFetcher	oldfetcher	= getFetcher();
//		if(i!=-1)
//		{
//			capa	= info.getName().substring(0, i); 
//			SimpleValueFetcher fetcher = new SimpleValueFetcher(oldfetcher);
////			if(microagent instanceof IPojoMicroAgent)
////			{
//				ocapa	= getCapabilityObject(capa);
//				fetcher.setValue("$pojocapa", ocapa);
////			}
//			this.fetcher = fetcher;
//			final Object	oocapa	= ocapa;
//			final String	scapa	= capa;
//			componentfetcher	= componentfetcher!=null ? componentfetcher :
//				new IResultCommand<Object, Class<?>>()
//			{
//				public Object execute(Class<?> type)
//				{
//					Object ret	= null;
////					if(SReflect.isSupertype(type, microagent.getClass()))
////					{
////						ret	= microagent;
////					}
////					else 
//					if(SReflect.isSupertype(type, oocapa.getClass()))
//					{
//						ret	= oocapa;
//					}
//					else if(SReflect.isSupertype(type, ICapability.class))
//					{
//						ret	= new CapabilityWrapper(getComponent(), oocapa, scapa);
//					}
//					return ret;
//				}
//			};
//		}
//		super.initService(info, model, componentfetcher).addResultListener(new DelegationResultListener<Void>(ret)
//		{
//			public void customResultAvailable(Void result)
//			{
//				BDIAgentInterpreter.this.fetcher	= oldfetcher;
//				super.customResultAvailable(result);
//			}
//		});
//		
//		return ret;
//	}
		
//	/**
//	 *  Add init code after parent injection.
//	 */
//	protected IFuture<Void> injectParent(final Object agent, final MicroModel model)
//	{
//		final Future<Void>	ret	= new Future<Void>();
//		super.injectParent(agent, model).addResultListener(new DelegationResultListener<Void>(ret)
//		{
//			public void customResultAvailable(Void result)
//			{
//				// Find classes with generated init methods.
//				List<Class<?>>	inits	= new ArrayList<Class<?>>();
//				inits.add(agent.getClass());
//				for(int i=0; i<inits.size(); i++)
//				{
//					Class<?>	clazz	= inits.get(i);
//					if(clazz.getSuperclass().isAnnotationPresent(Agent.class)
//						|| clazz.getSuperclass().isAnnotationPresent(Capability.class))
//					{
//						inits.add(clazz.getSuperclass());
//					}
//				}
//				
//				// Call init methods of superclasses first.
//				for(int i=inits.size()-1; i>=0; i--)
//				{
//					Class<?>	clazz	= inits.get(i);
//					List<Tuple2<Class<?>[], Object[]>>	initcalls	= BDIAgent.getInitCalls(agent, clazz);
//					for(Tuple2<Class<?>[], Object[]> initcall: initcalls)
//					{					
//						try
//						{
//							String name	= IBDIClassGenerator.INIT_EXPRESSIONS_METHOD_PREFIX+"_"+clazz.getName().replace("/", "_").replace(".", "_");
//							Method um = agent.getClass().getMethod(name, initcall.getFirstEntity());
////								System.out.println("Init: "+um);
//							um.invoke(agent, initcall.getSecondEntity());
//						}
//						catch(InvocationTargetException e)
//						{
//							e.getTargetException().printStackTrace();
//						}
//						catch(Exception e)
//						{
//							e.printStackTrace();
//						}
//					}
//				}
//				
//				initCapabilities(agent, ((BDIModel)model).getSubcapabilities(), 0).addResultListener(new DelegationResultListener<Void>(ret));
//			}
//		});
//		return ret;
//	}
		
//	/**
//	 *  Init the capability pojo objects.
//	 */
//	protected IFuture<Void>	initCapabilities(final Object agent, final Tuple2<FieldInfo, BDIModel>[] caps, final int i)
//	{
//		final Future<Void>	ret	= new Future<Void>();
//		
//		if(i<caps.length)
//		{
//			try
//			{
//				Field	f	= caps[i].getFirstEntity().getField(getComponent().getClassLoader());
//				f.setAccessible(true);
//				final Object	capa	= f.get(agent);
//				
//				String globalname;
//				try
//				{
//					Field	g	= agent.getClass().getDeclaredField("__globalname");
//					g.setAccessible(true);
//					globalname	= (String)g.get(agent);
//					globalname	= globalname==null ? f.getName() : globalname+MElement.CAPABILITY_SEPARATOR+f.getName();
//				}
//				catch(Exception e)
//				{
//					throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
//				}
//				
//				injectAgent((BDIAgent)microagent, capa, caps[i].getSecondEntity(), globalname);
//				
//				injectServices(capa, caps[i].getSecondEntity())
//					.addResultListener(new DelegationResultListener<Void>(ret)
//				{
//					public void customResultAvailable(Void result)
//					{
//						injectParent(capa, caps[i].getSecondEntity())
//							.addResultListener(new DelegationResultListener<Void>(ret)
//						{
//							public void customResultAvailable(Void result)
//							{
//								initCapabilities(agent, caps, i+1)
//									.addResultListener(new DelegationResultListener<Void>(ret));
//							}
//						});
//					}
//				});				
//			}
//			catch(Exception e)
//			{
//				ret.setException(e);
//			}
//		}
//		else
//		{
//			ret.setResult(null);
//		}
//		
//		return ret;
//	}
		
//		/**
//		 *  Add extra init code after components.
//		 */
//		public IFuture<Void> initComponents(final IModelInfo model, String config)
//		{
//			final Future<Void>	ret	= new Future<Void>();
//			super.initComponents(model, config).addResultListener(new DelegationResultListener<Void>(ret)
//			{
//				public void customResultAvailable(Void result)
//				{
//					Object agent = microagent instanceof IPojoMicroAgent? ((IPojoMicroAgent)microagent).getPojoAgent(): microagent;
////					wrapCollections(bdimodel.getCapability(), agent);
//					ret.setResult(null);
//				}
//			});
//			return ret;
//		}
		
	/**
	 * 
	 */
	protected void wrapCollections(MCapability mcapa, Object agent)
	{
		// Inject belief collections.
		List<MBelief> mbels = mcapa.getBeliefs();
		for(MBelief mbel: mbels)
		{
			try
			{
				Object val = mbel.getValue(getComponent());
				if(val==null)
				{
					String impl = mbel.getImplClassName();
					if(impl!=null)
					{
						Class<?> implcl = SReflect.findClass(impl, null, getComponent().getClassLoader());
						val = implcl.newInstance();
					}
					else
					{
						Class<?> cl = mbel.getType(getComponent().getClassLoader());//f.getType();
						if(SReflect.isSupertype(List.class, cl))
						{
							val = new ArrayList();
						}
						else if(SReflect.isSupertype(Set.class, cl))
						{
							val = new HashSet();
						}
						else if(SReflect.isSupertype(Map.class, cl))
						{
							val = new HashMap();
						}
					}
				}
				if(val instanceof List)
				{
					String bname = mbel.getName();
					mbel.setValue(getComponent(), new ListWrapper((List<?>)val, getComponent(), ChangeEvent.FACTADDED+"."+bname, ChangeEvent.FACTREMOVED+"."+bname, ChangeEvent.FACTCHANGED+"."+bname, mbel));
				}
				else if(val instanceof Set)
				{
					String bname = mbel.getName();
					mbel.setValue(getComponent(), new SetWrapper((Set<?>)val, getComponent(), ChangeEvent.FACTADDED+"."+bname, ChangeEvent.FACTREMOVED+"."+bname, ChangeEvent.FACTCHANGED+"."+bname, mbel));
				}
				else if(val instanceof Map)
				{
					String bname = mbel.getName();
					mbel.setValue(getComponent(), new MapWrapper((Map<?,?>)val, getComponent(), ChangeEvent.FACTADDED+"."+bname, ChangeEvent.FACTREMOVED+"."+bname, ChangeEvent.FACTCHANGED+"."+bname, mbel));
				}
			}
			catch(RuntimeException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
		
	/**
	 *  Get the goals of a given type as pojos.
	 *  @param clazz The pojo goal class.
	 *  @return The currently instantiated goals of that type.
	 */
	public <T> Collection<T> getGoals(Class<T> clazz)
	{
		Collection<RGoal>	rgoals	= getCapability().getGoals(clazz);
		List<T>	ret	= new ArrayList<T>();
		for(RProcessableElement rgoal: rgoals)
		{
			ret.add((T)rgoal.getPojoElement());
		}
		return ret;
	}
	
	/**
	 *  Get the current goals as api representation.
	 *  @return All currently instantiated goals.
	 */
	public Collection<IGoal> getGoals()
	{
		return (Collection)getCapability().getGoals();
	}
	
	/**
	 *  Get the goal api representation for a pojo goal.
	 *  @param goal The pojo goal.
	 *  @return The api goal.
	 */
	public IGoal getGoal(Object goal)
	{
		return getCapability().getRGoal(goal);
	}

	/**
	 *  Dispatch a pojo goal wait for its result.
	 *  @param goal The pojo goal.
	 *  @return The goal result.
	 */
	public <T, E> IFuture<E> dispatchTopLevelGoal(final T goal)
	{
		final Future<E> ret = new Future<E>();
		
		final MGoal mgoal = ((MCapability)capa.getModelElement()).getGoal(goal.getClass().getName());
		if(mgoal==null)
			throw new RuntimeException("Unknown goal type: "+goal);
		final RGoal rgoal = new RGoal(getComponent(), mgoal, goal, (RPlan)null);
		rgoal.addListener(new ExceptionDelegationResultListener<Void, E>(ret)
		{
			public void customResultAvailable(Void result)
			{
				Object res = RGoal.getGoalResult(goal, mgoal, bdimodel.getClassloader());
				ret.setResult((E)res);
			}
		});

//		System.out.println("adopt goal");
		RGoal.adoptGoal(rgoal, getComponent());
		
		return ret;
	}
	
	/**
	 *  Drop a pojo goal.
	 *  @param goal The pojo goal.
	 */
	public void dropGoal(Object goal)
	{
		for(RGoal rgoal: getCapability().getGoals(goal.getClass()))
		{
			if(goal.equals(rgoal.getPojoElement()))
			{
				rgoal.drop();
			}
		}
	}

	/**
	 *  Dispatch a pojo plan and wait for its result.
	 *  @param plan The pojo plan or plan name.
	 *  @return The plan result.
	 */
	public <T, E> IFuture<E> adoptPlan(T plan)
	{
		return adoptPlan(plan, null);
	}
	
	/**
	 *  Dispatch a goal wait for its result.
	 *  @param plan The pojo plan or plan name.
	 *  @param args The plan arguments.
	 *  @return The plan result.
	 */
	public <T, E> IFuture<E> adoptPlan(T plan, Object[] args)
	{
		final Future<E> ret = new Future<E>();
		MPlan mplan = bdimodel.getCapability().getPlan(plan instanceof String? (String)plan: plan.getClass().getName());
		if(mplan==null)
			throw new RuntimeException("Plan model not found for: "+plan);
		
		final RPlan rplan = RPlan.createRPlan(mplan, plan, null, getComponent());
		rplan.addPlanListener(new IPlanListener<E>()
		{
			public void planFinished(E result)
			{
				if(rplan.getException()!=null)
				{
					ret.setException(rplan.getException());
				}
				else
				{
					ret.setResult(result);
				}
			}
		});
		rplan.setReason(new ChangeEvent(null, null, args, null));
		RPlan.executePlan(rplan, getComponent(), null);
		return ret;
	}
	
	/**
	 *  Add a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public void addBeliefListener(String name, final IBeliefListener listener)
	{
		String fname = bdimodel.getBeliefMappings().containsKey(name) ? bdimodel.getBeliefMappings().get(name) : name;
		
		List<EventType> events = new ArrayList<EventType>();
		addBeliefEvents(getComponent(), events, fname);

		final boolean multi = ((MCapability)getCapability().getModelElement())
			.getBelief(fname).isMulti(bdimodel.getClassloader());
		
		String rulename = fname+"_belief_listener_"+System.identityHashCode(listener);
		Rule<Void> rule = new Rule<Void>(rulename, 
			ICondition.TRUE_CONDITION, new IAction<Void>()
		{
			public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
			{
				if(!multi)
				{
					listener.beliefChanged((ChangeInfo)event.getContent());
				}
				else
				{
					if(ChangeEvent.FACTADDED.equals(event.getType().getType(0)))
					{
						listener.factAdded((ChangeInfo)event.getContent());
					}
					else if(ChangeEvent.FACTREMOVED.equals(event.getType().getType(0)))
					{
						listener.factAdded((ChangeInfo)event.getContent());
					}
					else if(ChangeEvent.FACTCHANGED.equals(event.getType().getType(0)))
					{
//						Object[] vals = (Object[])event.getContent();
						listener.factChanged((ChangeInfo)event.getContent());
					}
					else if(ChangeEvent.BELIEFCHANGED.equals(event.getType().getType(0)))
					{
						listener.beliefChanged((ChangeInfo)event.getContent());
					}
				}
				return IFuture.DONE;
			}
		});
		rule.setEvents(events);
		getRuleSystem().getRulebase().addRule(rule);
	}
	
	/**
	 *  Remove a belief listener.
	 *  @param name The belief name.
	 *  @param listener The belief listener.
	 */
	public void removeBeliefListener(String name, IBeliefListener listener)
	{
		name	= bdimodel.getBeliefMappings().containsKey(name) ? bdimodel.getBeliefMappings().get(name) : name;
		String rulename = name+"_belief_listener_"+System.identityHashCode(listener);
		getRuleSystem().getRulebase().removeRule(rulename);
	}

	/**
	 *  Start the component behavior.
	 */
	public void startBehavior()
	{
//		super.startBehavior();
		
//		final Object agent = microagent instanceof PojoBDIAgent? ((PojoBDIAgent)microagent).getPojoAgent(): microagent;
		final Object agent = getComponent().getComponentFeature(IMicroLifecycleFeature.class).getPojoAgent();
				
		// Init bdi configuration
		String confname = getComponent().getConfiguration();
		if(confname!=null)
		{
			MConfiguration mconf = bdimodel.getCapability().getConfiguration(confname);
			
			if(mconf!=null)
			{
				// Set initial belief values
				List<UnparsedExpression> ibels = mconf.getInitialBeliefs();
				if(ibels!=null)
				{
					for(UnparsedExpression uexp: ibels)
					{
						try
						{
							MBelief mbel = bdimodel.getCapability().getBelief(uexp.getName());
							Object val = SJavaParser.parseExpression(uexp, getComponent().getModel().getAllImports(), getComponent().getClassLoader()).getValue(null);
	//						Field f = mbel.getTarget().getField(getClassLoader());
	//						f.setAccessible(true);
	//						f.set(agent, val);
							mbel.setValue(getComponent(), val);
						}
						catch(RuntimeException e)
						{
							throw e;
						}
						catch(Exception e)
						{
							throw new RuntimeException(e);
						}
					}
				}
				
				// Create initial goals
				List<UnparsedExpression> igoals = mconf.getInitialGoals();
				if(igoals!=null)
				{
					for(UnparsedExpression uexp: igoals)
					{
						MGoal mgoal = null;
						Object goal = null;
						Class<?> gcl = null;
						
						// Create goal if expression available
						if(uexp.getValue()!=null && uexp.getValue().length()>0)
						{
							Object o = SJavaParser.parseExpression(uexp, getComponent().getModel().getAllImports(), getComponent().getClassLoader()).getValue(getComponent().getFetcher());
							if(o instanceof Class)
							{
								gcl = (Class<?>)o;
							}
							else
							{
								goal = o;
								gcl = o.getClass();
							}
						}
						
						if(gcl==null && uexp.getClazz()!=null)
						{
							gcl = uexp.getClazz().getType(getComponent().getClassLoader(), getComponent().getModel().getAllImports());
						}
						if(gcl==null)
						{
							// try to fetch via name
							mgoal = bdimodel.getCapability().getGoal(uexp.getName());
							if(mgoal==null && uexp.getName().indexOf(".")==-1)
							{
								// try with package
								mgoal = bdimodel.getCapability().getGoal(getComponent().getModel().getPackage()+"."+uexp.getName());
							}
							if(mgoal!=null)
							{
								gcl = mgoal.getTargetClass(getComponent().getClassLoader());
							}
						}
						if(mgoal==null)
						{
							mgoal = bdimodel.getCapability().getGoal(gcl.getName());
						}
						if(goal==null)
						{
							try
							{
								Class<?> agcl = agent.getClass();
								Constructor<?>[] cons = gcl.getDeclaredConstructors();
								for(Constructor<?> c: cons)
								{
									Class<?>[] params = c.getParameterTypes();
									if(params.length==0)
									{
										// perfect found empty con
										goal = gcl.newInstance();
										break;
									}
									else if(params.length==1 && params[0].equals(agcl))
									{
										// found (first level) inner class constructor
										goal = c.newInstance(new Object[]{agent});
										break;
									}
								}
							}
							catch(RuntimeException e)
							{
								throw e;
							}
							catch(Exception e)
							{
								throw new RuntimeException(e);
							}
						}
						
						if(mgoal==null || goal==null)
						{
							throw new RuntimeException("Could not create initial goal: "+uexp);
						}
						
						RGoal rgoal = new RGoal(getComponent(), mgoal, goal, (RPlan)null);
						RGoal.adoptGoal(rgoal, getComponent());
					}
				}
				
				// Create initial plans
				List<UnparsedExpression> iplans = mconf.getInitialPlans();
				if(iplans!=null)
				{
					for(UnparsedExpression uexp: iplans)
					{
						MPlan mplan = bdimodel.getCapability().getPlan(uexp.getName());
						// todo: allow Java plan constructor calls
	//						Object val = SJavaParser.parseExpression(uexp, model.getModelInfo().getAllImports(), getClassLoader());
					
						RPlan rplan = RPlan.createRPlan(mplan, mplan, null, getComponent());
						RPlan.executePlan(rplan, getComponent(), null);
					}
				}
			}
		}
		
		// Observe dynamic beliefs
		List<MBelief> beliefs = bdimodel.getCapability().getBeliefs();
		
		for(final MBelief mbel: beliefs)
		{
			List<EventType> events = new ArrayList<EventType>();
			
			Collection<String> evs = mbel.getEvents();
			Object	cap = null;
			if(evs!=null && !evs.isEmpty())
			{
				Object	ocapa	= agent;
				int	i	= mbel.getName().indexOf(MElement.CAPABILITY_SEPARATOR);
				if(i!=-1)
				{
					ocapa	= getCapabilityObject(mbel.getName().substring(0, mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)));
				}
				cap	= ocapa;

				for(String ev: evs)
				{
					addBeliefEvents(getComponent(), events, ev);
				}
			}
			
			Collection<EventType> rawevents = mbel.getRawEvents();
			if(rawevents!=null)
			{
				Collection<EventType> revs = mbel.getRawEvents();
				if(revs!=null)
					events.addAll(revs);
			}
		
			if(!events.isEmpty())
			{
				final Object fcapa = cap;
				Rule<Void> rule = new Rule<Void>(mbel.getName()+"_belief_update", 
					ICondition.TRUE_CONDITION, new IAction<Void>()
				{
					Object oldval = null;
					
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
//							System.out.println("belief update: "+event);
						if(mbel.isFieldBelief())
						{
							try
							{
								Method um = fcapa.getClass().getMethod(IBDIClassGenerator.DYNAMIC_BELIEF_UPDATEMETHOD_PREFIX+SUtil.firstToUpperCase(mbel.getName()), new Class[0]);
								um.invoke(fcapa, new Object[0]);
							}
							catch(Exception e)
							{
								e.printStackTrace();
							}
						}
						else
						{
							Object value = mbel.getValue(getComponent());
							// todo: save old value?!
							createChangeEvent(value, oldval, null, getComponent(), mbel.getName());
							oldval = value;
						}
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				getRuleSystem().getRulebase().addRule(rule);
			}
			
			if(mbel.getUpdaterate()>0)
			{
				int	i	= mbel.getName().indexOf(MElement.CAPABILITY_SEPARATOR);
				final String	name;
				final Object	capa;
				if(i!=-1)
				{
					capa	= getCapabilityObject(mbel.getName().substring(0, mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)));
					name	= mbel.getName().substring(mbel.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR)+1); 
				}
				else
				{
					capa	= agent;
					name	= mbel.getName();
				}

				final IClockService cs = SServiceProvider.getLocalService(getComponent(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM);
				cs.createTimer(mbel.getUpdaterate(), new ITimedObject()
				{
					ITimedObject	self	= this;
					Object oldval = null;
					
					public void timeEventOccurred(long currenttime)
					{
						try
						{
							getComponent().getComponentFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
							{
								public IFuture<Void> execute(IInternalAccess ia)
								{
									try
									{
										// Invoke dynamic update method if field belief
										if(mbel.isFieldBelief())
										{
											Method um = capa.getClass().getMethod(IBDIClassGenerator.DYNAMIC_BELIEF_UPDATEMETHOD_PREFIX+SUtil.firstToUpperCase(name), new Class[0]);
											um.invoke(capa, new Object[0]);
										}
										// Otherwise just call getValue and throw event
										else
										{
											Object value = mbel.getValue(capa, getComponent().getClassLoader());
											createChangeEvent(value, oldval, null, getComponent(), mbel.getName());
											oldval = value;
										}
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
									
									cs.createTimer(mbel.getUpdaterate(), self);
									return IFuture.DONE;
								}
							});
						}
						catch(ComponentTerminatedException cte)
						{
							
						}
					}
				
					public void exceptionOccurred(Exception exception)
					{
						getComponent().getLogger().severe("Cannot update belief "+mbel.getName()+": "+exception);
					}
				});
			}
		}
		
		// Observe goal types
		List<MGoal> goals = bdimodel.getCapability().getGoals();
		for(final MGoal mgoal: goals)
		{
//				 todo: explicit bdi creation rule
//				rulesystem.observeObject(goals.get(i).getTargetClass(getClassLoader()));
		
//				boolean fin = false;
			
			final Class<?> gcl = mgoal.getTargetClass(getComponent().getClassLoader());
//				boolean declarative = false;
//				boolean maintain = false;
			
			List<MCondition> conds = mgoal.getConditions(MGoal.CONDITION_CREATION);
			if(conds!=null)
			{
				for(MCondition cond: conds)
				{
					if(cond.getConstructorTarget()!=null)
					{
						final Constructor<?> c = cond.getConstructorTarget().getConstructor(getComponent().getClassLoader());
						
						Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
							ICondition.TRUE_CONDITION, new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
	//							System.out.println("create: "+context);
								
								Object pojogoal = null;
								try
								{
									boolean ok = true;
									Class<?>[] ptypes = c.getParameterTypes();
									Object[] pvals = new Object[ptypes.length];
									
									Annotation[][] anns = c.getParameterAnnotations();
									int skip = ptypes.length - anns.length;
									
									for(int i=0; i<ptypes.length; i++)
									{
										Object	o	= event.getContent();
										if(o!=null && SReflect.isSupertype(ptypes[i], o.getClass()))
										{
											pvals[i] = o;
										}
										else if(o instanceof ChangeInfo<?> && ((ChangeInfo)o).getValue()!=null && SReflect.isSupertype(ptypes[i], ((ChangeInfo)o).getValue().getClass()))
										{
											pvals[i] = ((ChangeInfo)o).getValue();
										}
										else if(SReflect.isSupertype(agent.getClass(), ptypes[i]))
										{
											pvals[i] = agent;
										}
										
										// ignore implicit parameters of inner class constructor
										if(pvals[i]==null && i>=skip)
										{
											for(int j=0; anns!=null && j<anns[i-skip].length; j++)
											{
												if(anns[i-skip][j] instanceof CheckNotNull)
												{
													ok = false;
													break;
												}
											}
										}
									}
									
									if(ok)
									{
										pojogoal = c.newInstance(pvals);
									}
								}
								catch(RuntimeException e)
								{
									throw e;
								}
								catch(Exception e)
								{
									throw new RuntimeException(e);
								}
								
								if(pojogoal!=null && !getCapability().containsGoal(pojogoal))
								{
									final Object fpojogoal = pojogoal;
									dispatchTopLevelGoal(pojogoal)
										.addResultListener(new IResultListener<Object>()
									{
										public void resultAvailable(Object result)
										{
											getComponent().getLogger().info("Goal succeeded: "+result);
										}
										
										public void exceptionOccurred(Exception exception)
										{
											getComponent().getLogger().info("Goal failed: "+fpojogoal+" "+exception);
										}
									});
								}
//									else
//									{
//										System.out.println("new goal not adopted, already contained: "+pojogoal);
//									}
								
								return IFuture.DONE;
							}
						});
						rule.setEvents(cond.getEvents());
						getRuleSystem().getRulebase().addRule(rule);
					}
					else if(cond.getMethodTarget()!=null)
					{
						final Method m = cond.getMethodTarget().getMethod(getComponent().getClassLoader());
						
						Rule<Void> rule = new Rule<Void>(mgoal.getName()+"_goal_create", 
							new MethodCondition(null, m)
						{
							protected Object invokeMethod(IEvent event) throws Exception
							{
								m.setAccessible(true);
								Object[] pvals = getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(),
									mgoal, new ChangeEvent(event), null, null);
								return pvals!=null? m.invoke(null, pvals): null;
							}
														
//								public Tuple2<Boolean, Object> prepareResult(Object res)
//								{
//									Tuple2<Boolean, Object> ret = null;
//									if(res instanceof Boolean)
//									{
//										ret = new Tuple2<Boolean, Object>((Boolean)res, null);
//									}
//									else if(res!=null)
//									{
//										ret = new Tuple2<Boolean, Object>(Boolean.TRUE, res);
//									}
//									else
//									{
//										ret = new Tuple2<Boolean, Object>(Boolean.FALSE, null);
//									}
//									return ret;
//								}
						}, new IAction<Void>()
						{
							public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
							{
		//						System.out.println("create: "+context);
								
								if(condresult!=null)
								{
									if(SReflect.isIterable(condresult))
									{
										for(Iterator<Object> it = SReflect.getIterator(condresult); it.hasNext(); )
										{
											Object pojogoal = it.next();
											dispatchTopLevelGoal(pojogoal);
										}
									}
									else
									{
										dispatchTopLevelGoal(condresult);
									}
								}
								else
								{
//										Object pojogoal = null;
//										if(event.getContent()!=null)
//										{
//											try
//											{
//												Class<?> evcl = event.getContent().getClass();
//												Constructor<?> c = gcl.getConstructor(new Class[]{evcl});
//												pojogoal = c.newInstance(new Object[]{event.getContent()});
//												dispatchTopLevelGoal(pojogoal);
//											}
//											catch(Exception e)
//											{
//												e.printStackTrace();
//											}
//										}
//										else
//										{
										Constructor<?>[] cons = gcl.getConstructors();
										Object pojogoal = null;
										boolean ok = false;
										for(Constructor<?> c: cons)
										{
											try
											{
												Object[] vals = getInjectionValues(c.getParameterTypes(), c.getParameterAnnotations(),
													mgoal, new ChangeEvent(event), null, null);
												if(vals!=null)
												{
													pojogoal = c.newInstance(vals);
													dispatchTopLevelGoal(pojogoal);
													break;
												}
												else
												{
													ok = true;
												}
											}
											catch(Exception e)
											{
											}
										}
										if(pojogoal==null && !ok)
											throw new RuntimeException("Unknown how to create goal: "+gcl);
									}
//									}
								
								return IFuture.DONE;
							}
						});
						rule.setEvents(cond.getEvents());
						getRuleSystem().getRulebase().addRule(rule);
					}
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_DROP);
			if(conds!=null)
			{
				for(MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget().getMethod(getComponent().getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_drop", 
						new GoalsExistCondition(mgoal, capa), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: getCapability().getGoals(mgoal))
							{
								if(!RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
									 && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
								{
									executeGoalMethod(m, goal, event)
										.addResultListener(new IResultListener<Boolean>()
									{
										public void resultAvailable(Boolean result)
										{
											if(result.booleanValue())
											{
//													System.out.println("Goal dropping triggered: "+goal);
				//								rgoal.setLifecycleState(BDIAgent.this, rgoal.GOALLIFECYCLESTATE_DROPPING);
												if(!goal.isFinished())
												{
													goal.setException(new GoalFailureException("drop condition: "+m.getName()));
//														{
//															public void printStackTrace() 
//															{
//																super.printStackTrace();
//															}
//														});
													goal.setProcessingState(getComponent(), RGoal.GoalProcessingState.FAILED);
												}
											}
										}
										
										public void exceptionOccurred(Exception exception)
										{
										}
									});
								}
							}
							
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					getRuleSystem().getRulebase().addRule(rule);
//						rule.setEvents(cond.getEvents());
//						getRuleSystem().getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_CONTEXT);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget().getMethod(getComponent().getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_suspend", 
						new GoalsExistCondition(mgoal, capa), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: getCapability().getGoals(mgoal))
							{
								if(!RGoal.GoalLifecycleState.SUSPENDED.equals(goal.getLifecycleState())
								  && !RGoal.GoalLifecycleState.DROPPING.equals(goal.getLifecycleState())
								  && !RGoal.GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()))
								{	
									executeGoalMethod(m, goal, event)
										.addResultListener(new IResultListener<Boolean>()
									{
										public void resultAvailable(Boolean result)
										{
											if(!result.booleanValue())
											{
//													if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
//														System.out.println("Goal suspended: "+goal);
												goal.setLifecycleState(getComponent(), RGoal.GoalLifecycleState.SUSPENDED);
												goal.setState(RProcessableElement.State.INITIAL);
											}
										}
										
										public void exceptionOccurred(Exception exception)
										{
										}
									});
								}
							}
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					getRuleSystem().getRulebase().addRule(rule);
					
//						rule.setEvents(cond.getEvents());
//						getRuleSystem().getRulebase().addRule(rule);
					
					rule = new Rule<Void>(mgoal.getName()+"_goal_option", 
						new GoalsExistCondition(mgoal, capa), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: getCapability().getGoals(mgoal))
							{
								if(RGoal.GoalLifecycleState.SUSPENDED.equals(goal.getLifecycleState()))
								{	
									executeGoalMethod(m, goal, event)
										.addResultListener(new IResultListener<Boolean>()
									{
										public void resultAvailable(Boolean result)
										{
											if(result.booleanValue())
											{
//													if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
//														System.out.println("Goal made option: "+goal);
												goal.setLifecycleState(getComponent(), RGoal.GoalLifecycleState.OPTION);
//													setState(ia, PROCESSABLEELEMENT_INITIAL);
											}
										}
										
										public void exceptionOccurred(Exception exception)
										{
										}
									});
								}
							}
							
							return IFuture.DONE;
						}
					});
					rule.setEvents(events);
					getRuleSystem().getRulebase().addRule(rule);
					
//						rule.setEvents(cond.getEvents());
//						getRuleSystem().getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_TARGET);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget().getMethod(getComponent().getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_target", 
						new CombinedCondition(new ICondition[]{
							new GoalsExistCondition(mgoal, capa)
		//							, new LifecycleStateCondition(SUtil.createHashSet(new String[]
		//							{
		//								RGoal.GOALLIFECYCLESTATE_ACTIVE,
		//								RGoal.GOALLIFECYCLESTATE_ADOPTED,
		//								RGoal.GOALLIFECYCLESTATE_OPTION,
		//								RGoal.GOALLIFECYCLESTATE_SUSPENDED
		//							}))
						}),
						new IAction<Void>()
					{
						public IFuture<Void> execute(final IEvent event, final IRule<Void> rule, final Object context, Object condresult)
						{
							for(final RGoal goal: getCapability().getGoals(mgoal))
							{
								executeGoalMethod(m, goal, event)
									.addResultListener(new IResultListener<Boolean>()
								{
									public void resultAvailable(Boolean result)
									{
										if(result.booleanValue())
										{
											goal.targetConditionTriggered(getComponent(), event, rule, context);
										}
									}
									
									public void exceptionOccurred(Exception exception)
									{
									}
								});
							}
						
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					getRuleSystem().getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_RECUR);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget().getMethod(getComponent().getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_recur",
						new GoalsExistCondition(mgoal, capa), new IAction<Void>()
	//						new CombinedCondition(new ICondition[]{
	//							new LifecycleStateCondition(GOALLIFECYCLESTATE_ACTIVE),
	//							new ProcessingStateCondition(GOALPROCESSINGSTATE_PAUSED),
	//							new MethodCondition(getPojoElement(), m),
	//						}), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: getCapability().getGoals(mgoal))
							{
								if(RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState())
									&& RGoal.GoalProcessingState.PAUSED.equals(goal.getProcessingState()))
								{	
									executeGoalMethod(m, goal, event)
										.addResultListener(new IResultListener<Boolean>()
									{
										public void resultAvailable(Boolean result)
										{
											if(result.booleanValue())
											{
												goal.setTriedPlans(null);
												goal.setApplicablePlanList(null);
												goal.setProcessingState(getComponent(), RGoal.GoalProcessingState.INPROCESS);
											}
										}
										
										public void exceptionOccurred(Exception exception)
										{
										}
									});
								}
							}
							return IFuture.DONE;
						}
					});
					rule.setEvents(cond.getEvents());
					getRuleSystem().getRulebase().addRule(rule);
				}
			}
			
			conds = mgoal.getConditions(MGoal.CONDITION_MAINTAIN);
			if(conds!=null)
			{
				for(final MCondition cond: conds)
				{
					final Method m = cond.getMethodTarget().getMethod(getComponent().getClassLoader());
					
					Rule<?> rule = new Rule<Void>(mgoal.getName()+"_goal_maintain", 
						new GoalsExistCondition(mgoal, capa), new IAction<Void>()
	//						new CombinedCondition(new ICondition[]{
	//							new LifecycleStateCondition(GOALLIFECYCLESTATE_ACTIVE),
	//							new ProcessingStateCondition(GOALPROCESSINGSTATE_IDLE),
	//							new MethodCondition(getPojoElement(), mcond, true),
	//						}), new IAction<Void>()
					{
						public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
						{
							for(final RGoal goal: getCapability().getGoals(mgoal))
							{
								if(RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState())
									&& RGoal.GoalProcessingState.IDLE.equals(goal.getProcessingState()))
								{	
									executeGoalMethod(m, goal, event)
										.addResultListener(new IResultListener<Boolean>()
									{
										public void resultAvailable(Boolean result)
										{
											if(!result.booleanValue())
											{
//													System.out.println("Goal maintain triggered: "+goal);
//													System.out.println("state was: "+goal.getProcessingState());
												goal.setProcessingState(getComponent(), RGoal.GoalProcessingState.INPROCESS);
											}
										}
										
										public void exceptionOccurred(Exception exception)
										{
										}
									});
								}
							}
							return IFuture.DONE;
						}
					});
					List<EventType> events = new ArrayList<EventType>(cond.getEvents());
					events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, mgoal.getName()}));
					rule.setEvents(events);
					getRuleSystem().getRulebase().addRule(rule);
					
					// if has no own target condition
					if(mgoal.getConditions(MGoal.CONDITION_TARGET)==null)
					{
						// if not has own target condition use the maintain cond
						rule = new Rule<Void>(mgoal.getName()+"_goal_target", 
							new GoalsExistCondition(mgoal, capa), new IAction<Void>()
	//							new MethodCondition(getPojoElement(), mcond), new IAction<Void>()
						{
							public IFuture<Void> execute(final IEvent event, final IRule<Void> rule, final Object context, Object condresult)
							{
								for(final RGoal goal: getCapability().getGoals(mgoal))
								{
									executeGoalMethod(m, goal, event)
										.addResultListener(new IResultListener<Boolean>()
									{
										public void resultAvailable(Boolean result)
										{
											if(result.booleanValue())
											{
												goal.targetConditionTriggered(getComponent(), event, rule, context);
											}
										}
										
										public void exceptionOccurred(Exception exception)
										{
										}
									});
								}
								
								return IFuture.DONE;
							}
						});
						rule.setEvents(cond.getEvents());
						getRuleSystem().getRulebase().addRule(rule);
					}
				}
			}
		}
		
		// Observe plan types
		List<MPlan> mplans = bdimodel.getCapability().getPlans();
		for(int i=0; i<mplans.size(); i++)
		{
			final MPlan mplan = mplans.get(i);
			
			IAction<Void> createplan = new IAction<Void>()
			{
				public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
				{
					RPlan rplan = RPlan.createRPlan(mplan, mplan, new ChangeEvent(event), getComponent());
					RPlan.executePlan(rplan, getComponent(), null);
					return IFuture.DONE;
				}
			};
			
			MTrigger trigger = mplan.getTrigger();
			
			if(trigger!=null)
			{
				List<String> fas = trigger.getFactAddeds();
				if(fas!=null && fas.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_factadded_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
					for(String fa: fas)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.FACTADDED, fa}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
	
				List<String> frs = trigger.getFactRemoveds();
				if(frs!=null && frs.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_factremoved_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
					for(String fr: frs)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.FACTREMOVED, fr}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
				
				List<String> fcs = trigger.getFactChangeds();
				if(fcs!=null && fcs.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_factchanged_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
					for(String fc: fcs)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.FACTCHANGED, fc}));
						rule.addEvent(new EventType(new String[]{ChangeEvent.BELIEFCHANGED, fc}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
				
				List<MGoal> gfs = trigger.getGoalFinisheds();
				if(gfs!=null && gfs.size()>0)
				{
					Rule<Void> rule = new Rule<Void>("create_plan_goalfinished_"+mplan.getName(), new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
							return new Future<Tuple2<Boolean, Object>>(TRUE);
						}
					}, createplan);
					for(MGoal gf: gfs)
					{
						rule.addEvent(new EventType(new String[]{ChangeEvent.GOALDROPPED, gf.getName()}));
					}
					rulesystem.getRulebase().addRule(rule);
				}
			}
			
			// context condition
			
			final MethodInfo mi = mplan.getBody().getContextConditionMethod(getComponent().getClassLoader());
			if(mi!=null)
			{
				PlanContextCondition pcc = mi.getMethod(getComponent().getClassLoader()).getAnnotation(PlanContextCondition.class);
				String[] evs = pcc.beliefs();
				RawEvent[] rawevs = pcc.rawevents();
				List<EventType> events = new ArrayList<EventType>();
				for(String ev: evs)
				{
					addBeliefEvents(getComponent(), events, ev);
				}
				for(RawEvent rawev: rawevs)
				{
					events.add(createEventType(rawev));
				}
				
				IAction<Void> abortplans = new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						Collection<RPlan> coll = capa.getPlans(mplan);
						
						for(final RPlan plan: coll)
						{
							invokeBooleanMethod(plan.getBody().getBody(agent), mi.getMethod(getComponent().getClassLoader()), plan.getModelElement(), event, plan)
								.addResultListener(new IResultListener<Boolean>()
							{
								public void resultAvailable(Boolean result)
								{
									if(!result.booleanValue())
									{
										plan.abort();
									}
								}
								
								public void exceptionOccurred(Exception exception)
								{
								}
							});
						}
						return IFuture.DONE;
					}
				};
				
				Rule<Void> rule = new Rule<Void>("plan_context_abort_"+mplan.getName(), 
					new PlansExistCondition(mplan, capa), abortplans);
				rule.setEvents(events);
				rulesystem.getRulebase().addRule(rule);
			}
		}
		
		// add/rem goal inhibitor rules
		if(!goals.isEmpty())
		{
			boolean	usedelib	= false;
			for(int i=0; !usedelib && i<goals.size(); i++)
			{
				usedelib	= goals.get(i).getDeliberation()!=null;
			}
			
			if(usedelib)
			{
				List<EventType> events = new ArrayList<EventType>();
				events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, EventType.MATCHALL}));
				Rule<Void> rule = new Rule<Void>("goal_addinitialinhibitors", 
					ICondition.TRUE_CONDITION, new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						// create the complete inhibitorset for a newly adopted goal
						
						RGoal goal = (RGoal)event.getContent();
						for(RGoal other: getCapability().getGoals())
						{
	//						if(other.getLifecycleState().equals(RGoal.GOALLIFECYCLESTATE_ACTIVE) 
	//							&& other.getProcessingState().equals(RGoal.GOALPROCESSINGSTATE_INPROCESS)
							if(!other.isInhibitedBy(goal) && other.inhibits(goal, getComponent()))
							{
								goal.addInhibitor(other, getComponent());
							}
						}
						
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				getRuleSystem().getRulebase().addRule(rule);
				
				events = getGoalEvents(null);
				rule = new Rule<Void>("goal_addinhibitor", 
					new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
	//						if(((RGoal)event.getContent()).getId().indexOf("Battery")!=-1)
	//							System.out.println("maintain");
//								if(getComponentIdentifier().getName().indexOf("Ambu")!=-1)
//									System.out.println("addin");
							
							// return true when other goal is active and inprocess
							boolean ret = false;
							EventType type = event.getType();
							RGoal goal = (RGoal)event.getContent();
							ret = ChangeEvent.GOALACTIVE.equals(type.getType(0)) && RGoal.GoalProcessingState.INPROCESS.equals(goal.getProcessingState())
								|| (ChangeEvent.GOALINPROCESS.equals(type.getType(0)) && RGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState()));
//								return ret? ICondition.TRUE: ICondition.FALSE;
							return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
						}
					}, new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						RGoal goal = (RGoal)event.getContent();
//							if(goal.getId().indexOf("PerformPatrol")!=-1)
//								System.out.println("addinh: "+goal);
						MDeliberation delib = goal.getMGoal().getDeliberation();
						if(delib!=null)
						{
							Set<MGoal> inhs = delib.getInhibitions();
							if(inhs!=null)
							{
								for(MGoal inh: inhs)
								{
									Collection<RGoal> goals = getCapability().getGoals(inh);
									for(RGoal other: goals)
									{
	//									if(!other.isInhibitedBy(goal) && goal.inhibits(other, getInternalAccess()))
										if(!goal.isInhibitedBy(other) && goal.inhibits(other, getComponent()))
										{
											other.addInhibitor(goal, getComponent());
										}
									}
								}
							}
						}
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				getRuleSystem().getRulebase().addRule(rule);
				
				rule = new Rule<Void>("goal_removeinhibitor", 
					new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
//								if(getComponentIdentifier().getName().indexOf("Ambu")!=-1)
//									System.out.println("remin");
							
							// return true when other goal is active and inprocess
							boolean ret = false;
							EventType type = event.getType();
							if(event.getContent() instanceof RGoal)
							{
								RGoal goal = (RGoal)event.getContent();
								ret = ChangeEvent.GOALSUSPENDED.equals(type.getType(0)) || ChangeEvent.GOALOPTION.equals(type.getType(0))
									|| !RGoal.GoalProcessingState.INPROCESS.equals(goal.getProcessingState());
							}
//								return ret? ICondition.TRUE: ICondition.FALSE;
							return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
						}
					}, new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						// Remove inhibitions of this goal 
						RGoal goal = (RGoal)event.getContent();
						MDeliberation delib = goal.getMGoal().getDeliberation();
						if(delib!=null)
						{
							Set<MGoal> inhs = delib.getInhibitions();
							if(inhs!=null)
							{
								for(MGoal inh: inhs)
								{
		//							if(goal.getId().indexOf("AchieveCleanup")!=-1)
		//								System.out.println("reminh: "+goal);
									Collection<RGoal> goals = getCapability().getGoals(inh);
									for(RGoal other: goals)
									{
										if(goal.equals(other))
											continue;
										
										if(other.isInhibitedBy(goal))
											other.removeInhibitor(goal, getComponent());
									}
								}
							}
							
							// Remove inhibitor from goals of same type if cardinality is used
							if(delib.isCardinalityOne())
							{
								Collection<RGoal> goals = getCapability().getGoals(goal.getMGoal());
								if(goals!=null)
								{
									for(RGoal other: goals)
									{
										if(goal.equals(other))
											continue;
										
										if(other.isInhibitedBy(goal))
											other.removeInhibitor(goal, getComponent());
									}
								}
							}
						}
					
						return IFuture.DONE;
					}
				});
				rule.setEvents(events);
				getRuleSystem().getRulebase().addRule(rule);
				
				
				rule = new Rule<Void>("goal_inhibit", 
					new LifecycleStateCondition(RGoal.GoalLifecycleState.ACTIVE), new IAction<Void>()
				{
					public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
					{
						RGoal goal = (RGoal)event.getContent();
	//					System.out.println("optionizing: "+goal+" "+goal.inhibitors);
						goal.setLifecycleState(getComponent(), RGoal.GoalLifecycleState.OPTION);
						return IFuture.DONE;
					}
				});
				rule.addEvent(new EventType(new String[]{ChangeEvent.GOALINHIBITED, EventType.MATCHALL}));
				getRuleSystem().getRulebase().addRule(rule);
			}
			
			Rule<Void> rule = new Rule<Void>("goal_activate", 
				new CombinedCondition(new ICondition[]{
					new LifecycleStateCondition(RGoal.GoalLifecycleState.OPTION),
					new ICondition()
					{
						public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
						{
							RGoal goal = (RGoal)event.getContent();
//								return !goal.isInhibited()? ICondition.TRUE: ICondition.FALSE;
							return new Future<Tuple2<Boolean,Object>>(!goal.isInhibited()? ICondition.TRUE: ICondition.FALSE);
						}
					}
				}), new IAction<Void>()
			{
				public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context, Object condresult)
				{
					RGoal goal = (RGoal)event.getContent();
//						if(goal.getMGoal().getName().indexOf("AchieveCleanup")!=-1)
//							System.out.println("reactivating: "+goal);
					goal.setLifecycleState(getComponent(), RGoal.GoalLifecycleState.ACTIVE);
					return IFuture.DONE;
				}
			});
			rule.addEvent(new EventType(new String[]{ChangeEvent.GOALNOTINHIBITED, EventType.MATCHALL}));
			rule.addEvent(new EventType(new String[]{ChangeEvent.GOALOPTION, EventType.MATCHALL}));
//				rule.setEvents(SUtil.createArrayList(new String[]{ChangeEvent.GOALNOTINHIBITED, ChangeEvent.GOALOPTION}));
			getRuleSystem().getRulebase().addRule(rule);
		}
		
		// perform init write fields (after injection of bdiagent)
		performInitWrites(getComponent());
		
		// Start rule system
		inited	= true;
//			if(getComponentIdentifier().getName().indexOf("Cleaner")!=-1)// && getComponentIdentifier().getName().indexOf("Burner")==-1)
//				getCapability().dumpPlansPeriodically(getInternalAccess());
//			if(getComponentIdentifier().getName().indexOf("Ambulance")!=-1)
//			{
//				getCapability().dumpGoalsPeriodically(getInternalAccess());
//				getCapability().dumpPlansPeriodically(getInternalAccess());
//			}
		
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
		
//			throw new RuntimeException();
	}
		
//	/**
//	 *  Called before blocking the component thread.
//	 */
//	public void	beforeBlock()
//	{
//		RPlan	rplan	= ExecutePlanStepAction.RPLANS.get();
//		testBodyAborted(rplan);
//		ComponentSuspendable sus = ComponentSuspendable.COMSUPS.get();
//		if(rplan!=null && sus!=null && !RPlan.PlanProcessingState.WAITING.equals(rplan.getProcessingState()))
//		{
//			final ResumeCommand<Void> rescom = rplan.new ResumeCommand<Void>(sus, false);
//			rplan.setProcessingState(PlanProcessingState.WAITING);
//			rplan.resumecommand = rescom;
//		}
//	}
//		
//	/**
//	 *  Called after unblocking the component thread.
//	 */
//	public void	afterBlock()
//	{
//		RPlan rplan = ExecutePlanStepAction.RPLANS.get();
//		testBodyAborted(rplan);
//		if(rplan!=null)
//		{
//			rplan.setProcessingState(PlanProcessingState.RUNNING);
//			if(rplan.resumecommand!=null)
//			{
//				// performs only cleanup without setting future
//				rplan.resumecommand.execute(Boolean.FALSE);
//				rplan.resumecommand = null;
//			}
//		}
//	}
		
	/**
	 *  Check if plan is already aborted.
	 */
	protected void testBodyAborted(RPlan rplan)
	{
		// Throw error to exit body method of aborted plan.
		if(rplan!=null && rplan.aborted && rplan.getLifecycleState()==PlanLifecycleState.BODY)
		{
//				System.out.println("aborting after block: "+rplan);
			throw new BodyAborted();
		}
	}
		
	/**
	 *  Execute a goal method.
	 */
	protected IFuture<Boolean> executeGoalMethod(Method m, RProcessableElement goal, IEvent event)
	{
		return invokeBooleanMethod(goal.getPojoElement(), m, goal.getModelElement(), event, null);
	}
	
	/**
	 *  Get parameter values for injection into method and constructor calls.
	 */
	public Object[] getInjectionValues(Class<?>[] ptypes, Annotation[][] anns, MElement melement, ChangeEvent event, RPlan rplan, RProcessableElement rpe)
	{
		return getInjectionValues(ptypes, anns, melement, event, rplan, rpe, null);
	}
		
	// todo: support parameter names via annotation in guesser (guesser with meta information)
	/**
	 *  Get parameter values for injection into method and constructor calls.
	 *  @return A valid assigment or null if no assignment could be found.
	 */
	public Object[]	getInjectionValues(Class<?>[] ptypes, Annotation[][] anns, MElement melement, ChangeEvent event, RPlan rplan, RProcessableElement rpe, Collection<Object> vs)
	{
		Collection<Object> vals = new LinkedHashSet<Object>();
		if(vs!=null)
			vals.addAll(vs);
		
		// Find capability based on model element (or use agent).
		String	capaname	= null;
		if(melement!=null)
		{
			int idx = melement.getName().lastIndexOf(MElement.CAPABILITY_SEPARATOR);
			if(idx!=-1)
			{
				capaname = melement.getName().substring(0, idx);
			}
		}
		Object capa = capaname!=null ? getCapabilityObject(capaname): getComponent().getComponentFeature(IBDIAgentFeature.class).getCapability();
//			: getAgent() instanceof PojoBDIAgent? ((PojoBDIAgent)getAgent()).getPojoAgent(): getAgent();
		vals.add(capa);
		vals.add(new CapabilityWrapper(getComponent(), capa, capaname));
		vals.add(getComponent());
		vals.add(getComponent().getExternalAccess());

		// Add plan values if any.
		if(rplan!=null)
		{
			Object reason = rplan.getReason();
			if(reason instanceof RProcessableElement && rpe==null)
			{
				rpe	= (RProcessableElement)reason;
			}
			vals.add(reason);
			vals.add(rplan);
			
			if(rplan.getException()!=null)
			{
				vals.add(rplan.getException());
			}
			
			Object delem = rplan.getDispatchedElement();
			if(delem instanceof ChangeEvent && event==null)
			{
				event = (ChangeEvent)delem;
			}
		}
		// Todo: cond result!?
		
		// Add event values
		if(event!=null)
		{
			vals.add(event);
			vals.add(event.getValue());
			if(event.getValue() instanceof ChangeInfo)
			{
				vals.add(new ChangeInfoEntryMapper((ChangeInfo<?>)event.getValue()));
				vals.add(((ChangeInfo<?>)event.getValue()).getValue());
			}
		}

		// Add processable element values if any (for plan and APL).
		if(rpe!=null)
		{
			vals.add(rpe);
			if(rpe.getPojoElement()!=null)
			{
				vals.add(rpe.getPojoElement());
				if(rpe instanceof RGoal)
				{
					Object pojo = rpe.getPojoElement();
					MGoal mgoal = (MGoal)rpe.getModelElement();
					List<MParameter> params = mgoal.getParameters();
					for(MParameter param: params)
					{
						Object val = param.getValue(pojo, getComponent().getClassLoader());
						vals.add(val);
					}
				}
			}
			if(rpe.getPojoElement() instanceof InvocationInfo)
			{
				vals.add(((InvocationInfo)rpe.getPojoElement()).getParams());
			}
		}
		
		// Fill in values from annotated events or using parameter guesser.
		boolean[] notnulls = new boolean[ptypes.length];
		
		Object[]	ret	= new Object[ptypes.length];
		SimpleParameterGuesser	g	= new SimpleParameterGuesser(vals);
		for(int i=0; i<ptypes.length; i++)
		{
			boolean	done	= false;
			for(int j=0; !done && anns!=null && j<anns[i].length; j++)
			{
				if(anns[i][j] instanceof Event)
				{
					done	= true;
					String	source	= ((Event)anns[i][j]).value();
					if(capaname!=null)
					{
						source	= capaname + MElement.CAPABILITY_SEPARATOR + source;
					}
					if(getBDIModel().getBeliefMappings().containsKey(source))
					{
						source	= getBDIModel().getBeliefMappings().get(source);
					}
					
					if(event!=null && event.getSource()!=null && event.getSource().equals(source))
					{
						boolean set = false;
						if(SReflect.isSupertype(ptypes[i], ChangeEvent.class))
						{
							ret[i]	= event;
							set = true;
						}
						else
						{
							if(SReflect.getWrappedType(ptypes[i]).isInstance(event.getValue()))
							{
								ret[i]	= event.getValue();
								set = true;
							}
							else if(event.getValue() instanceof ChangeInfo)
							{
								final ChangeInfo<?> ci = (ChangeInfo<?>)event.getValue();
								if(ptypes[i].equals(ChangeInfo.class))
								{
									ret[i] = ci;
									set = true;
								}
								else if(SReflect.getWrappedType(ptypes[i]).isInstance(ci.getValue()))
								{
									ret[i] = ci.getValue();
									set = true;
								}
								else if(ptypes[i].equals(Map.Entry.class))
								{
									ret[i] = new ChangeInfoEntryMapper(ci);
									set = true;
								}
							}
						}
						if(!set)
						{
							throw new IllegalArgumentException("Unexpected type for event injection: "+event+", "+ptypes[i]);
						}
						
//							else if(SReflect.isSupertype(ptypes[i], ChangeEvent.class))
//							{
//								ret[i]	= event;
//							}
//							else
//							{
//								throw new IllegalArgumentException("Unexpected type for event injection: "+event+", "+ptypes[i]);
//							}
					}
					else
					{
						MBelief	mbel	= getBDIModel().getCapability().getBelief(source);
						ret[i]	= mbel.getValue(getComponent());

					}
				}
				else if(anns[i][j] instanceof CheckNotNull)
				{
					notnulls[i] = true;
				}
			}
			
			if(!done)
			{
				ret[i]	= g.guessParameter(ptypes[i], false);
			}
		}
		

		// Adapt values (e.g. change events) to capability.
		if(capaname!=null)
		{
			for(int i=0; i<ret.length; i++)
			{
				ret[i]	= adaptToCapability(ret[i], capaname);
			}
		}
		
		for(int i=0; i<ptypes.length; i++)
		{
			if(notnulls[i] && ret[i]==null)
			{
				ret = null;
				break;
			}
		}
		
		return ret;
	}
		
//	/**
//	 *  Can be called on the agent thread only.
//	 * 
//	 *  Main method to perform agent execution.
//	 *  Whenever this method is called, the agent performs
//	 *  one of its scheduled actions.
//	 *  The platform can provide different execution models for agents
//	 *  (e.g. thread based, or synchronous).
//	 *  To avoid idle waiting, the return value can be checked.
//	 *  The platform guarantees that executeAction() will not be called in parallel. 
//	 *  @return True, when there are more actions waiting to be executed. 
//	 */
//	public boolean executeStep()
//	{
//		assert isComponentThread();
//		
//		// Evaluate condition before executing step.
////			boolean aborted = false;
////			if(rulesystem!=null)
////				aborted = rulesystem.processAllEvents(15);
////			if(aborted)
////				getCapability().dumpGoals();
//		
//		if(inited && rulesystem!=null)
//			rulesystem.processAllEvents();
//		
////			if(steps!=null && steps.size()>0)
////			{
////				System.out.println(getComponentIdentifier()+" steps: "+steps.size()+" "+steps.get(0).getStep().getClass());
////			}
//		boolean ret = super.executeStep();
//		
////			System.out.println(getComponentIdentifier()+" after step");
//
//		return ret || (inited && rulesystem!=null && rulesystem.isEventAvailable());
//	}
		
	/**
	 *  Get the rulesystem.
	 *  @return The rulesystem.
	 */
	public RuleSystem getRuleSystem()
	{
		return rulesystem;
	}
	
	/**
	 *  Get the bdimodel.
	 *  @return the bdimodel.
	 */
	public BDIModel getBDIModel()
	{
		return bdimodel;
	}
	
	/**
	 *  Get the state.
	 *  @return the state.
	 */
	public RCapability getCapability()
	{
		return capa;
	}
		
//		/**
//		 *  Method that tries to guess the parameters for the method call.
//		 */
//		public Object[] guessMethodParameters(Object pojo, Class<?>[] ptypes, Set<Object> values)
//		{
//			if(ptypes==null || values==null)
//				return null;
//			
//			Object[] params = new Object[ptypes.length];
//			
//			for(int i=0; i<ptypes.length; i++)
//			{
//				for(Object val: values)
//				{
//					if(SReflect.isSupertype(val.getClass(), ptypes[i]))
//					{
//						params[i] = val;
//						break;
//					}
//				}
//			}
//					
//			return params;
//		}
		
	/**
	 *  Get the inited.
	 *  @return The inited.
	 */
	public boolean isInited()
	{
		return inited;
	}
		
//		/**
//		 *  Create a result listener which is executed as an component step.
//		 *  @param The original listener to be called.
//		 *  @return The listener.
//		 */
//		public <T> IResultListener<T> createResultListener(IResultListener<T> listener)
//		{
//			// Must override method to ensure that plan steps are executed with planstepactions
//			if(ExecutePlanStepAction.RPLANS.get()!=null && !(listener instanceof BDIComponentResultListener))
//			{
//				return new BDIComponentResultListener(listener, this);
//			}
//			else
//			{
//				return super.createResultListener(listener);
//			}
//		}
		
	/**
	 * 
	 */
	protected IFuture<Boolean> invokeBooleanMethod(Object pojo, Method m, MElement modelelement, IEvent event, RPlan rplan)
	{
		final Future<Boolean> ret = new Future<Boolean>();
		try
		{
			m.setAccessible(true);
			
			Object[] vals = getInjectionValues(m.getParameterTypes(), m.getParameterAnnotations(),
				modelelement, event!=null ? new ChangeEvent(event) : null, rplan, null);
			if(vals==null)
				System.out.println("Invalid parameter assignment");
			Object app = m.invoke(pojo, vals);
			if(app instanceof Boolean)
			{
				ret.setResult((Boolean)app);
			}
			else if(app instanceof IFuture)
			{
				((IFuture<Boolean>)app).addResultListener(new DelegationResultListener<Boolean>(ret));
			}
		}
		catch(Exception e)
		{
			System.err.println("method: "+m);
			e.printStackTrace();
			ret.setException(e);
		}
		return ret;
	}
		
	/**
	 *  Create belief events from a belief name.
	 *  For normal beliefs 
	 *  beliefchanged.belname and factchanged.belname 
	 *  and for multi beliefs additionally
	 *  factadded.belname and factremoved 
	 *  are created.
	 */
	public static void addBeliefEvents(IInternalAccess ia, List<EventType> events, String belname)
	{
		events.add(new EventType(new String[]{ChangeEvent.BELIEFCHANGED, belname})); // the whole value was changed
		events.add(new EventType(new String[]{ChangeEvent.FACTCHANGED, belname})); // property change of a value
		
//		BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
		MBelief mbel = ((MCapability)ia.getComponentFeature(IBDIAgentFeature.class).getCapability().getModelElement()).getBelief(belname);
		if(mbel!=null && mbel.isMulti(ia.getClassLoader()))
		{
			events.add(new EventType(new String[]{ChangeEvent.FACTADDED, belname}));
			events.add(new EventType(new String[]{ChangeEvent.FACTREMOVED, belname}));
		}
	}
		
	/**
	 *  Create goal events for a goal name. creates
	 *  goaladopted, goaldropped
	 *  goaloption, goalactive, goalsuspended
	 *  goalinprocess, goalnotinprocess
	 *  events.
	 */
	public static List<EventType> getGoalEvents(MGoal mgoal)
	{
		List<EventType> events = new ArrayList<EventType>();
		if(mgoal==null)
		{
			events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALDROPPED, EventType.MATCHALL}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALOPTION, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALACTIVE, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALSUSPENDED, EventType.MATCHALL}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALINPROCESS, EventType.MATCHALL}));
			events.add(new EventType(new String[]{ChangeEvent.GOALNOTINPROCESS, EventType.MATCHALL}));
		}
		else
		{
			String name = mgoal.getName();
			events.add(new EventType(new String[]{ChangeEvent.GOALADOPTED, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALDROPPED, name}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALOPTION, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALACTIVE, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALSUSPENDED, name}));
			
			events.add(new EventType(new String[]{ChangeEvent.GOALINPROCESS, name}));
			events.add(new EventType(new String[]{ChangeEvent.GOALNOTINPROCESS, name}));
		}
		
		return events;
	}
		
	/**
	 *  Read the annotation events from method annotations.
	 */
	public static List<EventType> readAnnotationEvents(IInternalAccess ia, Annotation[][] annos)
	{
		List<EventType> events = new ArrayList<EventType>();
		for(Annotation[] ana: annos)
		{
			for(Annotation an: ana)
			{
				if(an instanceof jadex.rules.eca.annotations.Event)
				{
					jadex.rules.eca.annotations.Event ev = (jadex.rules.eca.annotations.Event)an;
					String name = ev.value();
					String type = ev.type();
					if(type.length()==0)
					{
						addBeliefEvents(ia, events, name);
					}
					else
					{
						events.add(new EventType(new String[]{type, name}));
					}
				}
			}
		}
		return events;
	}
		
	/**
	 *  Map a change info as Map:Entry.
	 */
	public static class ChangeInfoEntryMapper implements Map.Entry
	{
		protected ChangeInfo<?>	ci;

		public ChangeInfoEntryMapper(ChangeInfo<?> ci)
		{
			this.ci = ci;
		}

		public Object getKey()
		{
			return ci.getInfo();
		}

		public Object getValue()
		{
			return ci.getValue();
		}

		public Object setValue(Object value)
		{
			throw new UnsupportedOperationException();
		}

		public boolean equals(Object obj)
		{
			boolean	ret	= false;
			
			if(obj instanceof Map.Entry)
			{
				Map.Entry<?,?>	e1	= this;
				Map.Entry<?,?>	e2	= (Map.Entry<?,?>)obj;
				ret	= (e1.getKey()==null ? e2.getKey()==null : e1.getKey().equals(e2.getKey()))
					&& (e1.getValue()==null ? e2.getValue()==null : e1.getValue().equals(e2.getValue()));
			}
			
			return ret;
		}

		public int hashCode()
		{
			return (getKey()==null ? 0 : getKey().hashCode())
				^ (getValue()==null ? 0 : getValue().hashCode());
		}
	}

	/**
	 *  Condition for checking the lifecycle state of a goal.
	 */
	public static class LifecycleStateCondition implements ICondition
	{
		/** The allowed states. */
		protected Set<GoalLifecycleState> states;
		
		/** The flag if state is allowed or disallowed. */
		protected boolean allowed;
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(GoalLifecycleState state)
		{
			this(SUtil.createHashSet(new GoalLifecycleState[]{state}));
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(Set<GoalLifecycleState> states)
		{
			this(states, true);
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(GoalLifecycleState state, boolean allowed)
		{
			this(SUtil.createHashSet(new GoalLifecycleState[]{state}), allowed);
		}
		
		/**
		 *  Create a new condition.
		 */
		public LifecycleStateCondition(Set<GoalLifecycleState> states, boolean allowed)
		{
			this.states = states;
			this.allowed = allowed;
		}
		
		/**
		 *  Evaluate the condition.
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			RGoal goal = (RGoal)event.getContent();
			boolean ret = states.contains(goal.getLifecycleState());
			if(!allowed)
				ret = !ret;
//				return ret? ICondition.TRUE: ICondition.FALSE;
			return new Future<Tuple2<Boolean,Object>>(ret? ICondition.TRUE: ICondition.FALSE);
		}
	}
		
	/**
	 *  Condition that tests if goal instances of an mgoal exist.
	 */
	public static class GoalsExistCondition implements ICondition
	{
		protected MGoal mgoal;
		
		protected RCapability capa;
		
		public GoalsExistCondition(MGoal mgoal, RCapability capa)
		{
			this.mgoal = mgoal;
			this.capa = capa;
		}
		
		/**
		 * 
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			boolean res = !capa.getGoals(mgoal).isEmpty();
//				return res? ICondition.TRUE: ICondition.FALSE;
			return new Future<Tuple2<Boolean,Object>>(res? ICondition.TRUE: ICondition.FALSE);
		}
	}
		
	/**
	 *  Condition that tests if goal instances of an mplan exist.
	 */
	public static class PlansExistCondition implements ICondition
	{
		protected MPlan mplan;
		
		protected RCapability capa;
		
		public PlansExistCondition(MPlan mplan, RCapability capa)
		{
			this.mplan = mplan;
			this.capa = capa;
		}
		
		/**
		 * 
		 */
		public IFuture<Tuple2<Boolean, Object>> evaluate(IEvent event)
		{
			return new Future<Tuple2<Boolean,Object>>(!capa.getPlans(mplan).isEmpty()? ICondition.TRUE: ICondition.FALSE);
		}
	}
		
	/**
	 *  Get the current state as events.
	 */
	public List<IMonitoringEvent> getCurrentStateEvents()
	{
		List<IMonitoringEvent> ret = new ArrayList<IMonitoringEvent>();
		
		// Already gets merged beliefs (including subcapas).
		List<MBelief> mbels = getBDIModel().getCapability().getBeliefs();
		
		if(mbels!=null)
		{
			for(MBelief mbel: mbels)
			{
				BeliefInfo info = BeliefInfo.createBeliefInfo(getComponent(), mbel, getComponent().getClassLoader());
				MonitoringEvent ev = new MonitoringEvent(getComponent().getComponentIdentifier(), getComponent().getComponentDescription().getCreationTime(), IMonitoringEvent.EVENT_TYPE_CREATION+"."+IMonitoringEvent.SOURCE_CATEGORY_FACT, System.currentTimeMillis(), PublishEventLevel.FINE);
				ev.setSourceDescription(mbel.toString());
				ev.setProperty("details", info);
				ret.add(ev);
			}
		}
		
		// Goals of this capability.
		Collection<RGoal> goals = getCapability().getGoals();
		if(goals!=null)
		{
			for(RGoal goal: goals)
			{
				GoalInfo info = GoalInfo.createGoalInfo(goal);
				MonitoringEvent ev = new MonitoringEvent(getComponent().getComponentIdentifier(), getComponent().getComponentDescription().getCreationTime(), IMonitoringEvent.EVENT_TYPE_CREATION+"."+IMonitoringEvent.SOURCE_CATEGORY_GOAL, System.currentTimeMillis(), PublishEventLevel.FINE);
				ev.setSourceDescription(goal.toString());
				ev.setProperty("details", info);
				ret.add(ev);
			}
		}
		
		// Plans of this capability.
		Collection<RPlan> plans	= getCapability().getPlans();
		if(plans!=null)
		{
			for(RPlan plan: plans)
			{
				PlanInfo info = PlanInfo.createPlanInfo(plan);
				MonitoringEvent ev = new MonitoringEvent(getComponent().getComponentIdentifier(), getComponent().getComponentDescription().getCreationTime(), IMonitoringEvent.EVENT_TYPE_CREATION+"."+IMonitoringEvent.SOURCE_CATEGORY_PLAN, System.currentTimeMillis(), PublishEventLevel.FINE);
				ev.setSourceDescription(plan.toString());
				ev.setProperty("details", info);
				ret.add(ev);
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 */
	public static EventType createEventType(RawEvent rawev)
	{
		String[] p = new String[2];
		p[0] = rawev.value();
		p[1] = Object.class.equals(rawev.secondc())? rawev.second(): rawev.secondc().getName();
//		System.out.println("eveve: "+p[0]+" "+p[1]);
		return new EventType(p);
	}
}