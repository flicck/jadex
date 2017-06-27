package jadex.bridge.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jadex.bridge.ClassInfo;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.ServiceIdentifier;
import jadex.commons.IAsyncFilter;
import jadex.commons.IFilter;
import jadex.commons.Tuple2;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

/**
 *  Service query definition.
 */
public class ServiceQuery<T>
{
	/** The return type. */
	protected ClassInfo returntype;
	
	/** The service type. */
	protected ClassInfo servicetype;
	
	/** Tags of the service. */
	protected String[] servicetags;
	
	/** The search scope. */
	protected String scope;
	
	/** The query owner. */
	protected IComponentIdentifier owner;
	
	/** The service provider. */
	protected IComponentIdentifier provider;
	
	/** The service platform. */
	protected IComponentIdentifier platform;
	
	/** Flag, if service by the owner should be excluded. */
	protected boolean excludeowner;
	
	/** 
	 *  Filter for checking further service attributes.
	 *  Either IAsyncFilter<T> or IFilter<T>.
	 */
	protected Object filter;
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery()
	{
	}
	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(ClassInfo servicetype, String scope, IComponentIdentifier owner)
//	{
//		this(servicetype, scope, (IFilter<T>) null, null, owner);
//	}
//	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(ClassInfo servicetype, String scope, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
//		this(servicetype, scope, (IFilter<T>) null, provider, owner);
//	}
//	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(Class<T> servicetype, String scope, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
//		this(servicetype, scope, (IFilter<T>) null, provider, owner);
//	}
//	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(Class<T> servicetype, String scope, IAsyncFilter<T> filter, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
//		this(new ClassInfo(servicetype), scope, filter, provider, owner);
//	}
//	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(Class<T> servicetype, String scope, IFilter<T> filter, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
////		this(new ClassInfo(servicetype), scope, filter, provider, owner);this.returntype = servicetype;
//		this.servicetype = new ClassInfo(servicetype);
//		this.returntype = this.servicetype;
//		// todo: what is the best place for this?
//		this.scope = scope==null && ServiceIdentifier.isSystemService(servicetype)? RequiredServiceInfo.SCOPE_PLATFORM: scope;
//		this.filter = filter;
//		this.provider = provider;
//		this.owner = owner;
//		
//	}
//	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(ClassInfo servicetype, String scope, IFilter<T> filter, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
//		this.returntype = servicetype;
//		this.servicetype = servicetype;
//		this.scope = scope;
//		this.filter = filter;
//		this.provider = provider;
//		this.owner = owner;
//	}
//	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(Class<T> returntype, Class<?> servicetype, String scope, IAsyncFilter<T> filter, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
//		this(new ClassInfo(returntype), new ClassInfo(servicetype), scope, filter, provider, owner);
//	}
	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(ClassInfo returntype, ClassInfo servicetype, String scope, IAsyncFilter<T> filter, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
//		this.returntype = returntype;
//		this.servicetype = servicetype;
//		this.scope = scope;
//		this.filter = filter;
//		this.provider = provider;
//		this.owner = owner;
//	}
//	
//	/**
//	 *  Create a new service query.
//	 */
//	public ServiceQuery(ClassInfo servicetype, String scope, IAsyncFilter<T> filter, IComponentIdentifier provider, IComponentIdentifier owner)
//	{
//		this(servicetype, servicetype, scope, filter, provider, owner);
////		this.returntype = servicetype;
////		this.servicetype = servicetype;
////		this.scope = scope;
////		this.filter = filter;
////		this.provider = provider;
////		this.owner = owner;
//	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(Class<?> servicetype, String scope, IComponentIdentifier provider, IComponentIdentifier owner, Object filter)
	{
		this(servicetype, scope, provider, owner, filter, null);
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(Class<?> servicetype, String scope, IComponentIdentifier provider, IComponentIdentifier owner, Object filter, Class<?> returntype)
	{
		this(servicetype!=null? new ClassInfo(servicetype): null, scope==null && ServiceIdentifier.isSystemService(servicetype)? RequiredServiceInfo.SCOPE_PLATFORM: scope,
			provider, owner, filter, returntype!=null? new ClassInfo(returntype): null);
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(ClassInfo servicetype, String scope, IComponentIdentifier provider, IComponentIdentifier owner, Object filter)
	{
		this(servicetype, scope, provider, owner, filter, servicetype);
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(ClassInfo servicetype, String scope, IComponentIdentifier provider, IComponentIdentifier owner, Object filter, ClassInfo returntype)
	{
		if(owner==null)
			throw new IllegalArgumentException("Owner must not null");
		
		this.servicetype = servicetype;
		this.scope = scope;
		this.provider = provider;
		this.owner = owner;
		this.filter = filter;
		this.returntype = returntype;
	}
	
	/**
	 *  Shallow copy constructor.
	 *  @param original Original query.
	 */
	public ServiceQuery(ServiceQuery<T> original)
	{
		this.returntype = original.returntype;
		this.servicetype = original.servicetype;
		this.scope = original.scope;
		this.servicetags = original.servicetags;
		this.filter = original.filter;
		this.owner = original.owner;
	}

	/**
	 *  Get the service type.
	 *  @return The service type
	 */
	public ClassInfo getServiceType()
	{
		return servicetype;
	}

	/**
	 *  Set the service type.
	 *  @param type The service type to set
	 */
	public void setServiceType(ClassInfo servicetype)
	{
		this.servicetype = servicetype;
	}
	
	/**
	 *  Get the return type.
	 *  @return The return type
	 */
	public ClassInfo getReturnType()
	{
		return returntype;
	}

	/**
	 *  Set the return type.
	 *  @param type The return type to set
	 */
	public void setReturnType(ClassInfo returntype)
	{
		this.returntype = returntype;
	}
	
	/**
	 *  Get the scope.
	 *  @return The scope
	 */
	public String getScope()
	{
		return scope;
	}

	/**
	 *  Set the scope.
	 *  @param scope The scope to set
	 */
	public void setScope(String scope)
	{
		this.scope = scope;
	}
	
	/**
	 *  Get the filter.
	 *  @return The filter
	 */
	public Object getFilter()
	{
		return filter;
	}
	
	/**
	 *  Set the filter.
	 *  @param filter The filter to set
	 */
	public void setFilter(Object filter)
	{
		this.filter = filter;
	}
	
	/**
	 *  Set the filter.
	 *  @param filter The filter to set
	 */
	public <ST> void setSyncFilter(IFilter<ST> filter)
	{
		this.filter = filter;
	}

	/**
	 *  Set the filter.
	 *  @param filter The filter to set
	 */
	public <ST> void setAsyncFilter(IAsyncFilter<ST> filter)
	{
		this.filter = filter;
	}
	
	/**
	 *  Gets the service tags.
	 *  
	 *  @return The service tags. 
	 */
	public String[] getServiceTags()
	{
		return servicetags;
	}
	
	/**
	 *  Sets the service tags.
	 *  
	 *  @param servicetags The service tags. 
	 */
	public void setServiceTags(String[] servicetags)
	{
		this.servicetags = servicetags;
	}
	
	/**
	 *  Get the provider.
	 *  @return The provider
	 */
	public IComponentIdentifier getProvider()
	{
		return provider;
	}

	/**
	 *  Set the provider.
	 *  @param provider The provider to set
	 */
	public void setProvider(IComponentIdentifier provider)
	{
		this.provider = provider;
	}
	
	/**
	 *  Get the platform.
	 *  @return The platform
	 */
	public IComponentIdentifier getPlatform()
	{
		return platform;
	}
	
	/**
	 *  Get the platform.
	 *  @param platform The platform
	 */
	public void setPlatform(IComponentIdentifier platform)
	{
		this.platform = platform;
	}
	
	/**
	 *  Get the owner.
	 *  @return The owner
	 */
	public IComponentIdentifier getOwner()
	{
		return owner;
	}

	/**
	 *  Set the owner.
	 *  @param owner The owner to set
	 */
	public void setOwner(IComponentIdentifier owner)
	{
		this.owner = owner;
	}
	
	/**
	 *  Checks if service of the query owner should be excluded.
	 *  
	 *  @return True, if the services should be excluded.
	 */
	public boolean isExcludeOwner()
	{
		return excludeowner;
	}
	
	/**
	 *  Sets if service of the query owner should be excluded.
	 *  
	 *  @param excludeowner True, if the services should be excluded.
	 */
	public void setExcludeOwner(boolean excludeowner)
	{
		this.excludeowner = excludeowner;
	}
	
	/**
	 *  Gets the specification for the indexer.
	 *  
	 *  @return The specification for the indexer.
	 */
	public List<Tuple2<String, String[]>> getIndexerSearchSpec()
	{
		List<Tuple2<String, String[]>> ret = new ArrayList<Tuple2<String,String[]>>();
		
		if(platform != null)
			ret.add(new Tuple2<String, String[]>(JadexServiceKeyExtractor.KEY_TYPE_PLATFORM, new String[] { platform.toString() }));
		
		if(provider != null)
			ret.add(new Tuple2<String, String[]>(JadexServiceKeyExtractor.KEY_TYPE_PROVIDER, new String[] { provider.toString() }));
		
		if(servicetype != null)
			ret.add(new Tuple2<String, String[]>(JadexServiceKeyExtractor.KEY_TYPE_INTERFACE, new String[] { servicetype.getGenericTypeName() }));
		
		if(servicetags != null && servicetags.length > 0)
			ret.add(new Tuple2<String, String[]>(JadexServiceKeyExtractor.KEY_TYPE_TAGS, servicetags));
		
		return ret;
	}
	
	/**
	 *  Tests if the query matches a service.
	 *  
	 *  @param service The service.
	 *  @return True, if the service matches the keys.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected boolean matchesSync(IService service)
	{
		return (matchesKeys(service) && ((IFilter) filter).filter(service));
	}
	
	/**
	 *  Tests if the query matches a service.
	 *  
	 *  @param service The service.
	 *  @return True, if the service matches the keys.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected IFuture<Boolean> matchesAsync(IService service)
	{
		IFuture<Boolean> ret = null;
		if (matchesKeys(service))
		{
			ret = ((IAsyncFilter) filter).filter(service);
		}
		else
			ret = new Future<Boolean>(false);
		return ret;
	}
	
	/**
	 *  Tests if the query keys matches a service.
	 *  
	 *  @param service The service.
	 *  @return True, if the service matches the keys.
	 */
	protected boolean matchesKeys(IService service)
	{
		if (servicetype != null && !service.getServiceIdentifier().getServiceType().getGenericTypeName().equals(servicetype))
			return false;
		
		if (servicetags != null)
		{
			Set<String> tagsset = JadexServiceKeyExtractor.getKeysStatic(JadexServiceKeyExtractor.KEY_TYPE_TAGS, service);
			if (tagsset == null)
				return false;
			
			for (String tag : servicetags)
			{
				if (!tagsset.contains(tag))
				{
					return false;
				}
			}
		}
		
		if (provider != null && !provider.equals(service.getServiceIdentifier().getProviderId()))
			return false;
		
		if (platform != null && !platform.equals(service.getServiceIdentifier().getProviderId().getRoot()))
			return false;
		
		return true;
	}
	
	
}
