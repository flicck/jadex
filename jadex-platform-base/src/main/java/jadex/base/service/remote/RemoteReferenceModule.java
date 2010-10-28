package jadex.base.service.remote;

import jadex.base.service.remote.commands.RemoteDGCAddReferenceCommand;
import jadex.base.service.remote.commands.RemoteDGCRemoveReferenceCommand;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IExternalAccess;
import jadex.commons.Future;
import jadex.commons.ICommand;
import jadex.commons.IFuture;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.collection.LRU;
import jadex.commons.concurrent.DefaultResultListener;
import jadex.commons.concurrent.IResultListener;
import jadex.commons.service.IService;
import jadex.commons.service.IServiceIdentifier;
import jadex.commons.service.SServiceProvider;
import jadex.commons.service.clock.IClockService;
import jadex.micro.ExternalAccess;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 *  This class implements the rmi handling. It mainly supports:
 *  - remote reference management
 *  - creation of proxy references for transferring IProxyable objects
 *  - creation of proxies on the remote side of a target object
 *  - distributed garbage collection for target (remote) objects using reference counting
 *  - management of interfaceproperties for metadata such as exclusion or replacement of methods
 */
public class RemoteReferenceModule
{
	public static boolean debug = true;

	public static long DEFAULT_LEASETIME = 15000;
	
	public static long DEFAULT_BUFFERTIME = 3000;
	
	//-------- attributes --------

	/** The remote interface properties. */
	protected Map interfaceproperties;

	/** The remote management service. */
	protected RemoteServiceManagementService rsms;
	
	/** The cache of proxy infos (class -> proxy info). */
	protected Map proxyinfos;
	
	/** The map of target objects (rr  -> target object). */
	protected Map targetobjects;
	
	/** The inverse map of target object to remote references (target objects -> rr). */
	protected Map remoterefs;
	
	/** The id counter. */
	protected long idcnt;

	
	/** The proxycount count map. (rr -> number of proxies created for rr). */
	protected Map proxycount;
	
	/** The remote reference holders of a object (rr -> holder (rms cid)). */
	protected Map holders;
	
	
	/** The clock. */
	protected IClockService clock;
	
	/** The renew behaviour id. */
	protected long renewid;
	
	/** The remove behaviour id. */
	protected long removeid;
	
	//-------- constructors --------
	
	/**
	 *  Create a new remote reference module.
	 */
	public RemoteReferenceModule(RemoteServiceManagementService rsms, IClockService clock)
	{
		this.rsms = rsms;
		this.clock = clock;
		
		this.interfaceproperties = new HashMap();
		this.proxyinfos = new LRU(200);
		this.targetobjects = new HashMap();
		this.remoterefs = new HashMap();
		
		this.proxycount = new TreeMap();
		this.holders = new HashMap();
	}
	
	//-------- methods --------
	
	/**
	 *  Get a remote reference for a component for transport. 
	 *  (Called during marshalling from writer).
	 */
	public ProxyReference getProxyReference(Object target, Class[] remoteinterfaces, IComponentIdentifier tmpholder)
	{
		checkThread();
		
		// todo: should all ids of remote objects be saved in table?
		
		// Note: currently agents use model information e.g. componentviewer.viewerclass
		// to add specific properties, so that proxies are cached per agent model type due
		// to cached method call getPropertyMap().
		
		RemoteReference rr = getRemoteReference(target);
		
		// Remember that this rr is send to some other process (until the addRef message arrives).
		if(rr.isObjectReference())
			addTemporaryRemoteReference(rr, tmpholder);
		
		// This construct ensures
		// a) fast access to existing proxyinfos in the map
		// b) creation is performed only once by ordering threads 
		// via synchronized block and rechecking if proxy was already created.
		// -> not necessary due to only single threaded access via agent thread
		
		Object tcid = target instanceof IExternalAccess? (Object)((IExternalAccess)target).getModel().getFullName(): target.getClass();
		ProxyInfo pi = (ProxyInfo)proxyinfos.get(tcid);
		if(pi==null)
		{
//			synchronized(proxyinfos)
//			{
//				pi = (ProxyInfo)proxyinfos.get(tcid);
//				if(pi==null)
//				{
					pi = createProxyInfo(target, remoteinterfaces);
					proxyinfos.put(tcid, pi);
//					System.out.println("add: "+tcid+" "+ret);
//				}
//			}
		}
		
		return new ProxyReference(pi, rr);
	}
	
	/**
	 *  Create a proxy info for a service. 
	 */
	protected ProxyInfo createProxyInfo(Object target, Class[] remoteinterfaces)
	{
		checkThread();
		// todo: dgc, i.e. remember that target is a remote object (for which a proxyinfo is sent away).
		
		ProxyInfo ret = new ProxyInfo(remoteinterfaces);
		Map properties = null;
		
		// Hack! as long as registry is not there
		if(target instanceof IExternalAccess)
		{
			properties = ((IExternalAccess)target).getModel().getProperties();		
		}
		else if(properties==null && target instanceof IService)
		{
			properties = ((IService)target).getPropertyMap();
		}
		
		for(int i=0; i<remoteinterfaces.length+1; i++)
		{
			if(i>0)
				properties = (Map)interfaceproperties.get(remoteinterfaces[i-1]);
			
			Class targetclass = target.getClass();
			
			// Check for excluded and synchronous methods.
			if(properties!=null)
			{
				Object ex = properties.get(RemoteServiceManagementService.REMOTE_EXCLUDED);
				if(ex!=null)
				{
					for(Iterator it = SReflect.getIterator(ex); it.hasNext(); )
					{
						MethodInfo[] mis = getMethodInfo(it.next(), targetclass, false);
						for(int j=0; j<mis.length; j++)
						{
							ret.addExcludedMethod(mis[j]);
						}
					}
				}
				Object syn = properties.get(RemoteServiceManagementService.REMOTE_SYNCHRONOUS);
				if(syn!=null)
				{
					for(Iterator it = SReflect.getIterator(syn); it.hasNext(); )
					{
						MethodInfo[] mis = getMethodInfo(it.next(), targetclass, false);
						for(int j=0; j<mis.length; j++)
						{
							ret.addSynchronousMethod(mis[j]);
						}
					}
				}
				Object un = properties.get(RemoteServiceManagementService.REMOTE_UNCACHED);
				if(un!=null)
				{
					for(Iterator it = SReflect.getIterator(un); it.hasNext(); )
					{
						MethodInfo[] mis = getMethodInfo(it.next(), targetclass, false);
						for(int j=0; j<mis.length; j++)
						{
							ret.addUncachedMethod(mis[j]);
						}
					}
				}
				Object mr = properties.get(RemoteServiceManagementService.REMOTE_METHODREPLACEMENT);
				if(mr!=null)
				{
					for(Iterator it = SReflect.getIterator(mr); it.hasNext(); )
					{
						Object[] tmp = (Object[])it.next();
						MethodInfo[] mis = getMethodInfo(tmp[0], targetclass, false);
						for(int j=0; j<mis.length; j++)
						{
							ret.addMethodReplacement(mis[j], (IMethodReplacement)tmp[1]);
						}
					}
				}
				
				// Check methods and possibly cache constant calls.
				Method[] methods = remoteinterfaces[i].getMethods();
				methods	= (Method[])SUtil.joinArrays(methods, Object.class.getMethods());
				for(int j=0; j<methods.length; j++)
				{
					// only cache when not excluded, not cached and not replaced
					if(!ret.isUncached(methods[j]) && !ret.isExcluded(methods[j]) && !ret.isReplaced(methods[j])) 
					{
						Class rt = methods[j].getReturnType();
						Class[] ar = methods[j].getParameterTypes();
						
						if(void.class.equals(rt))
						{
		//					System.out.println("Warning, void method call will be executed asynchronously: "+type+" "+methods[i].getName());
						}
						else if(!(rt.isAssignableFrom(IFuture.class)))
						{
							if(ar.length>0)
							{
		//						System.out.println("Warning, service method is blocking: "+type+" "+methods[i].getName());
							}
							else
							{
								// Invoke method to get constant return value.
								try
								{
		//							System.out.println("Calling for caching: "+methods[i]);
									Object val = methods[j].invoke(target, new Object[0]);
									ret.putCache(methods[j].getName(), val);
								}
								catch(Exception e)
								{
									System.out.println("Warning, constant service method threw exception: "+remoteinterfaces[i]+" "+methods[j]);
			//						e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
		
		// Add default replacement for equals() and hashCode().
		Class targetclass = target.getClass();
		Method	equals	= SReflect.getMethod(Object.class, "equals", new Class[]{Object.class});
		if(ret.getMethodReplacement(equals)==null)
		{
			MethodInfo[] mis = getMethodInfo(equals, targetclass, false);
			for(int i=0; i<mis.length; i++)
			{
				ret.addMethodReplacement(mis[i], new DefaultEqualsMethodReplacement());
			}
		}
		Method	hashcode = SReflect.getMethod(Object.class, "hashCode", new Class[0]);
		if(ret.getMethodReplacement(hashcode)==null)
		{
			MethodInfo[] mis = getMethodInfo(hashcode, targetclass, true);
			for(int i=0; i<mis.length; i++)
			{
				ret.addMethodReplacement(mis[i], new DefaultHashcodeMethodReplacement());
			}
		}
		// Add getClass as excluded. Otherwise the target class must be present on
		// the computer which only uses the proxy.
		Method getclass = SReflect.getMethod(Object.class, "getClass", new Class[0]);
		if(ret.getMethodReplacement(getclass)==null)
		{
			ret.addExcludedMethod(new MethodInfo(getclass));
		}
		
		return ret;
	}
	
	/**
	 *  Get method info.
	 */
	public static MethodInfo[] getMethodInfo(Object iden, Class targetclass, boolean noargs)
	{
		MethodInfo[] ret;
		
		if(iden instanceof String)
		{
			if(noargs)
			{
				Method	method	= SReflect.getMethod(targetclass, (String)iden, new Class[0]);
				if(method==null)
					method	= SReflect.getMethod(Object.class, (String)iden, new Class[0]);
				
				if(method!=null)
				{
					ret = new MethodInfo[]{new MethodInfo(method)};
				}
				else
				{
					throw new RuntimeException("Method not found: "+iden);
				}
			}
			else
			{
				Method[] ms = SReflect.getMethods(targetclass, (String)iden);
				if(ms.length==0)
				{
					ms = SReflect.getMethods(Object.class, (String)iden);
				}
				
				if(ms.length==1)
				{
					ret = new MethodInfo[]{new MethodInfo(ms[0])};
				}
				else if(ms.length>1)
				{
					// Exclude all if more than one fits?!
					ret = new MethodInfo[ms.length];
					for(int i=0; i<ret.length; i++)
						ret[i] = new MethodInfo(ms[i]);
					
					// Check if the methods are equal = same signature (e.g. defined in different interfaces)
//					boolean eq = true;
//					Method m0 = ms[0];
//					for(int i=1; i<ms.length && eq; i++)
//					{
//						if(!hasEqualSignature(m0, ms[i]))
//							eq = false;
//					}
//					if(!eq)
//						throw new RuntimeException("More than one method with the name availble: "+tmp);
//					else
//						ret = new MethodInfo(m0);
				}
				else
				{
					throw new RuntimeException("Method not found: "+iden);
				}
			}
		}
		else
		{
			ret = new MethodInfo[]{new MethodInfo((Method)iden)};
		}
		
		return ret;
	}
	
	/**
	 *  Get a remote reference.
	 *  @param target The (local) remote object.
	 */
	protected RemoteReference getRemoteReference(Object target)
	{
		checkThread();
		RemoteReference ret = (RemoteReference)remoterefs.get(target);
		
		// Create a remote reference if not yet available.
		if(ret==null)
		{
			if(target instanceof IExternalAccess)
			{
				ret = new RemoteReference(rsms.getRMSComponentIdentifier(), ((IExternalAccess)target).getComponentIdentifier());
			}
			else if(target instanceof IService)
			{
				ret = new RemoteReference(rsms.getRMSComponentIdentifier(), ((IService)target).getServiceIdentifier());
			}
			else
			{
				ret = generateRemoteReference();
//				System.out.println("Adding rr: "+ret+" "+target);
				remoterefs.put(target, ret);
				targetobjects.put(ret, target);
			}
		}

		return ret;
	}
	
	/**
	 *  Delete a remote reference.
	 *  @param rr The remote reference.
	 */
	protected void deleteRemoteReference(RemoteReference rr)
	{
		checkThread();
		Object target = targetobjects.remove(rr);
		remoterefs.remove(target);
//		System.out.println("Removing rr: "+rr+" "+target);
	}
	
	/**
	 *  Shutdown the module.
	 *  Sends notifications to all 
	 */
	protected void shutdown()
	{
		checkThread();
		RemoteReference[] rrs = (RemoteReference[])proxycount.keySet().toArray(new RemoteReference[0]);
		for(int i=0; i<rrs.length; i++)
		{
			sendRemoveRemoteReference(rrs[i]);
		}
	}
	
	/**
	 *  Get a target object per remote reference.
	 *  @param rr The remote reference.
	 *  @return The target object.
	 */
	public IFuture getTargetObject(RemoteReference rr)
	{
		checkThread();
		final Future ret = new Future();
				
		if(rr.getTargetIdentifier() instanceof IServiceIdentifier)
		{
			IServiceIdentifier sid = (IServiceIdentifier)rr.getTargetIdentifier();
			
			// fetch service via its id
			SServiceProvider.getService(rsms.getComponent().getServiceProvider(), sid)
				.addResultListener(new IResultListener()
			{
				public void resultAvailable(Object source, Object result)
				{
					ret.setResult(result);
				}
				
				public void exceptionOccurred(Object source, Exception exception)
				{
					ret.setException(exception);
				}
			});
		}
		else if(rr.getTargetIdentifier() instanceof IComponentIdentifier)
		{
			final IComponentIdentifier cid = (IComponentIdentifier)rr.getTargetIdentifier();
			
			// fetch component via target component id
			SServiceProvider.getServiceUpwards(rsms.getComponent().getServiceProvider(), IComponentManagementService.class)
				.addResultListener(new IResultListener()
//					.addResultListener(component.createResultListener(new IResultListener()
			{
				public void resultAvailable(Object source, Object result)
				{
					IComponentManagementService cms = (IComponentManagementService)result;
					
					// fetch target component via component identifier.
					cms.getExternalAccess(cid).addResultListener(new IResultListener()
					{
						public void resultAvailable(Object source, Object result)
						{
							ret.setResult(result);
						}
						
						public void exceptionOccurred(Object source, Exception exception)
						{
							ret.setException(exception);
						}
					});
				}
				
				public void exceptionOccurred(Object source, Exception exception)
				{
					ret.setException(exception);
				}
			});
		}
		else //(rr.getTargetIdentifier() instanceof String)
		{
			Object o = targetobjects.get(rr);
			if(o!=null)
			{
				ret.setResult(o);
			}
			else
			{
				ret.setException(new RuntimeException("Remote object not found: "+rr));
			}
		}
		
		return ret;
	}
	
	/**
	 *  Remove a target object.
	 *  @param rr The remote reference.
	 *  @return The target object.
	 */
	protected Object removeTargetObject(RemoteReference rr)
	{
		checkThread();
		return targetobjects.remove(rr);
	}
	
	/**
	 *  Generate a remote reference.
	 *  @return The remote reference.
	 */
	protected RemoteReference generateRemoteReference()
	{
		checkThread();
		return new RemoteReference(rsms.getRMSComponentIdentifier(), ""+idcnt++);
	}
	
	//-------- management of proxies --------

	/**
	 *  Get a proxy for a proxy reference.
	 *  @param pr The proxy reference.
	 */
	public Object getProxy(ProxyReference pr)
	{
		checkThread();
		Object ret;
		
//		RemoteReference rr = pi.getRemoteReference();
		
		// Is is local return local target object.
		if(pr.getRemoteReference().getRemoteManagementServiceIdentifier().equals(rsms.getRMSComponentIdentifier()))
		{
			ret = targetobjects.get(pr.getRemoteReference());
		}
		// Else return new or old proxy.
		else
		{
//			System.out.println("interfaces of proxy: "+SUtil.arrayToString(pi.getTargetInterfaces()));
			
			Class[] tmp = pr.getProxyInfo().getTargetInterfaces();
			Class[] interfaces = new Class[tmp.length+1];
			System.arraycopy(tmp, 0, interfaces, 0, tmp.length);
			interfaces[tmp.length] = IFinalize.class;
			
			ret = Proxy.newProxyInstance(rsms.getComponent().getModel().getClassLoader(), 
				interfaces, new RemoteMethodInvocationHandler(rsms, pr));
			
			incProxyCount(pr.getRemoteReference());
			
//			ret = proxies.get(rr);
//			if(ret==null)
//			{
//				synchronized(this)
//				{
//					ret = proxies.get(rr);
//					if(ret==null)
//					{
//						ret = Proxy.newProxyInstance(rsms.getComponent().getModel().getClassLoader(), 
//							pi.getTargetInterfaces(), new RemoteMethodInvocationHandler(rsms, pi));
//						proxies.put(rr, ret);
//						
//						sendAddRemoteReference(rr);
//					}
//				}
//			}
		}
		
		return ret;
	}
	
	//-------- dgc --------
	
	/**
	 *  Increment the proxy count for a remote reference.
	 *  @param rr The remote reference for the proxy.
	 */
	protected void incProxyCount(RemoteReference rr)
	{
		checkThread();
		// Only keep track of proxies for java objects.
		// Components and services are not subject of gc.
		
		if(rr.isObjectReference())
		{
			boolean notify = false;
			Integer cnt = (Integer)proxycount.get(rr);
			if(cnt==null)
			{
				proxycount.put(rr, new Integer(1));
				notify = true;
				
				// todo: transfer lease time interval?!
				rr.setExpiryDate(clock.getTime()+DEFAULT_LEASETIME);
				
				// Initiate check procedure.
				startRenewalBehaviour();
			}
			else
			{
				proxycount.put(rr, new Integer(cnt.intValue()+1));
			}
				
	//		System.out.println("Add proxy: "+rr+" "+cnt);
			
			if(notify)
				sendAddRemoteReference(rr);
		}
	}
	
	/**
	 *  Start the removal behavior.
	 */
	protected void startRenewalBehaviour()
	{
		final long renewid = ++this.renewid;
		
		rsms.getComponent().scheduleStep(new ICommand()
		{
			public void execute(Object args)
			{
				if(renewid == RemoteReferenceModule.this.renewid)
				{
//					System.out.println("Starting renewal behavior: "+removeid);
					if(proxycount.size()>0)
					{
						System.out.println("Checking proxies: ");
						for(Iterator it=proxycount.keySet().iterator(); it.hasNext(); )
						{
							RemoteReference key = (RemoteReference)it.next();
							System.out.println("\t "+key+" "+key.getExpiryDate()+" "+System.currentTimeMillis()+" "+proxycount.get(key));
						}
					}
					
					long diff = 0;
					RemoteReference[] rrs = (RemoteReference[])proxycount.keySet().toArray(new RemoteReference[proxycount.size()]);
					for(int i=0; i<rrs.length; i++)
					{
						diff = rrs[i].getExpiryDate()-clock.getTime();
						if(diff<=0)
						{
//							System.out.println("renewal sent for: "+rrs[i]);
							sendAddRemoteReference(rrs[i]);
							
							// todo: use anwer of send for updating expiry date?!
							Object entry = proxycount.remove(rrs[i]);
							long expirydate = clock.getTime()+DEFAULT_LEASETIME;
							rrs[i].setExpiryDate(expirydate);
							proxycount.put(rrs[i], entry);
							
							diff = DEFAULT_LEASETIME;
						}
						else
						{
							break;
						}
					}
					if(proxycount.size()>0 && diff>0)
					{
//						System.out.println("renewal behaviour waiting: "+diff);
						rsms.getComponent().waitFor(diff, this);
					}
				}
				
//				System.out.println("renewal behaviour exit");
			}
		});
	}
	
	/**
	 *  Start removal behavior for expired holders.
	 */
	protected void startRemovalBehaviour()
	{
		final long removeid = ++this.removeid;
		
		rsms.getComponent().scheduleStep(new ICommand()
		{
			public void execute(Object args)
			{
				if(removeid == RemoteReferenceModule.this.removeid)
				{
//					System.out.println("Starting removal behavior: "+removeid);
//					if(holders.size()>0)
//					{
//						System.out.println("Checking holders: ");
//						for(Iterator it=holders.keySet().iterator(); it.hasNext(); )
//						{
//							Object key = it.next();
//							System.out.println("\t "+key+" "+((Map)holders.get(key)).keySet());
//						}
//					}
					
					for(Iterator it=holders.keySet().iterator(); it.hasNext(); )
					{
						RemoteReference rr = (RemoteReference)it.next();
						Map hds = (Map)holders.get(rr);
						for(Iterator it2=hds.keySet().iterator(); it2.hasNext(); )
						{
							RemoteReferenceHolder rrh = (RemoteReferenceHolder)it2.next();
							if(clock.getTime() > rrh.getExpiryDate()+DEFAULT_BUFFERTIME)
							{
								System.out.println("Removing expired holder: "+rr+" "+rrh+" "+rrh.getExpiryDate()+" "+System.currentTimeMillis());
								hds.remove(rrh);
								if(hds.size()==0)
								{
									holders.remove(rr);
									deleteRemoteReference(rr);
								}
							}
						}
					}
					
					if(holders.size()>0)
						rsms.getComponent().waitFor(5000, this);
				}
			}
		});
	}
	
	/**
	 *  Decrease the proxy count for a remote reference.
	 *  @param rr The remote reference for the proxy.
	 */
	protected void decProxyCount(RemoteReference rr)
	{
		checkThread();
		// Only keep track of proxies for java objects.
		// Components and services are not subject of gc.
		
		if(rr.isObjectReference())
		{
			boolean notify = false;
			Integer cnt = (Integer)proxycount.get(rr);
			int nv = cnt.intValue()-1;
			if(nv==0)
			{
				proxycount.remove(rr);
				notify = true;
//					System.out.println("Remove proxy: "+rr+" "+nv);
			}
			else
			{
				proxycount.put(rr, new Integer(nv));
			}
				
	//		System.out.println("Remove proxy: "+rr+" "+nv);
			if(notify)
				sendRemoveRemoteReference(rr);
		}
	}
	
	/**
	 *  Send addRef to the origin process of the remote reference.
	 *  @param rr The remote reference.
	 */
	protected void sendAddRemoteReference(RemoteReference rr)
	{
		checkThread();
		// DGC: notify rr origin that a new proxy of target object exists
		// todo: handle failures!
		Future future = new Future();
		future.addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
			}
		});
//		System.out.println("send add: "+rr);
		final String callid = SUtil.createUniqueId(rsms.getRMSComponentIdentifier().getLocalName());
		RemoteDGCAddReferenceCommand com = new RemoteDGCAddReferenceCommand(rr, rsms.getRMSComponentIdentifier(), callid);
		rsms.sendMessage(rr.getRemoteManagementServiceIdentifier(), com, callid, -1, future);
	}
	
	/**
	 *  Send removeRef to the origin process of the remote reference.
	 *  @param rr The remote reference.
	 */
	public void sendRemoveRemoteReference(RemoteReference rr)
	{
		checkThread();
		// DGC: notify rr origin that a new proxy of target object exists
		// todo: handle failures!
		Future future = new Future();
		future.addResultListener(new DefaultResultListener()
		{
			public void resultAvailable(Object source, Object result)
			{
			}
		});
//		System.out.println("send rem: "+rr);
		final String callid = SUtil.createUniqueId(rsms.getRMSComponentIdentifier().getLocalName());
		RemoteDGCRemoveReferenceCommand com = new RemoteDGCRemoveReferenceCommand(rr, rsms.getRMSComponentIdentifier(), callid);
		rsms.sendMessage(rr.getRemoteManagementServiceIdentifier(), com, callid, -1, future);
	}
	
	/**
	 *  Add a new temporary holder to a remote object.
	 *  @param rr The remote reference.
	 *  @param holder The cid of the holding rms.
	 */
	protected void addTemporaryRemoteReference(final RemoteReference rr, final IComponentIdentifier holder)
	{
		checkThread();
		Map hds = (Map)holders.get(rr);
		if(hds==null)
		{
			hds = new HashMap();
			holders.put(rr, hds);
			startRemovalBehaviour();
		}
		
		long expirydate = clock.getTime()+DEFAULT_LEASETIME;
		TemporaryRemoteReferenceHolder newth = new TemporaryRemoteReferenceHolder(holder, expirydate);
		TemporaryRemoteReferenceHolder oldth = (TemporaryRemoteReferenceHolder)hds.get(newth);
		if(oldth==null)
		{
			hds.put(newth, newth);
		}
		else
		{
			// Update existing holder.
			oldth.setNumber(oldth.getNumber()+1);
			oldth.setExpiryDate(expirydate);
		}
//		System.out.println("Holders for (temp add): "+rr+" add: "+holder+" "+hds.keySet());
	}
	
	/**
	 *  Add a new holder to a remote object.
	 *  @param rr The remote reference.
	 *  @param holder The cid of the holding rms.
	 */
	public void addRemoteReference(final RemoteReference rr, final IComponentIdentifier holder)
	{
		checkThread();
		Map hds = (Map)holders.get(rr);
		if(hds==null)
		{
			hds = new HashMap();
			holders.put(rr, hds);
			startRemovalBehaviour();
		}
		
		long expirydate = clock.getTime()+DEFAULT_LEASETIME;
		RemoteReferenceHolder newh = new RemoteReferenceHolder(holder, expirydate);
		RemoteReferenceHolder oldh = (RemoteReferenceHolder)hds.get(newh);
		
		if(oldh==null)
		{
//			throw new RuntimeException("Holder already contained: "+holder);
			hds.put(newh, newh);
		}
		else
		{
			// Renew expiry date of existing holder.
			oldh.setExpiryDate(expirydate);
			System.out.println("renewed lease for: "+rr+" "+oldh);
		}
		
		// Decrement number (and possibly remove) temporary holder.
		TemporaryRemoteReferenceHolder th = (TemporaryRemoteReferenceHolder)hds.get(new TemporaryRemoteReferenceHolder(holder, 0));
		if(th!=null)
		{
			th.setNumber(th.getNumber()-1);
			if(th.getNumber()==0)
			{
				hds.remove(th); // hds.size() != 0 
			}
		}

//		System.out.println("Holders for (add): "+rr+" add: "+holder+" "+hds.keySet());
	}
	
	/**
	 *  Remove a new holder from a remote object.
	 *  @param rr The remote reference.
	 *  @param holder The cid of the holding rms.
	 */
	public void removeRemoteReference(final RemoteReference rr, final IComponentIdentifier holder)
	{
		checkThread();
		Map hds = (Map)holders.get(rr);
//		if(hds==null || !hds.contains(holder))
//			throw new RuntimeException("Holder not contained: "+holder);

//		System.out.println("Holders for (rem): "+result+" rem: "+holder+" "+hds);
		
		if(hds!=null)
		{
			hds.remove(new RemoteReferenceHolder(holder, 0));
			if(hds.size()==0)
			{
				holders.remove(rr);
				deleteRemoteReference(rr);
			}
		}
	}
	
	/**
	 *  Check if correct thread access.
	 */
	protected void checkThread()
	{
		if(debug)
		{
			if(((ExternalAccess)rsms.getComponent()).getInterpreter().isExternalThread())
			{
				System.out.println("wrong thread: "+Thread.currentThread());
				Thread.dumpStack();
			}
		}
	}
}
