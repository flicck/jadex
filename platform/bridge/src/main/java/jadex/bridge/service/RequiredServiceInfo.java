package jadex.bridge.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.bridge.ClassInfo;
import jadex.bridge.modelinfo.NFRPropertyInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.commons.SReflect;

/**
 *  Struct for information about a required service.
 */
public class RequiredServiceInfo
{
	//-------- constants --------
	
	/** None component scope (nothing will be searched, forces required service creation). */
	public static final String SCOPE_NONE = "none";

	/** Parent scope. */
	public static final String SCOPE_PARENT = "parent";
	
	// todo: rename (COMPONENT_LOCAL)
	/** Local component scope (component only). */
	public static final String SCOPE_LOCAL = "local";
	
	/** Component scope (component and subcomponents). */
	public static final String SCOPE_COMPONENT = "component";
	
	// todo: rename (APPLICATION_PLATFORM) or remove
	/** Application scope (local application, i.e. second level component plus all subcomponents). */
	public static final String SCOPE_APPLICATION = "application";

	/** Platform scope (all components on the local platform). */
	public static final String SCOPE_PLATFORM = "platform";

	
	/** Application network scope (any platform with which a secret is shared and application tag must be shared). */
	public static final String SCOPE_APPLICATION_NETWORK = "application_network";
//	public static final String SCOPE_APPLICATION_CLOUD = "application_cloud";
	
	/** Network scope (any platform with which a secret is shared). */
	public static final String SCOPE_NETWORK = "network";
//	public static final String SCOPE_CLOUD = "cloud";
		
	// needed?!
	/** Global application scope. */
	public static final String SCOPE_APPLICATION_GLOBAL = "application_global";
	
	/** Global scope (any reachable platform including those with unrestricted services). */
	public static final String SCOPE_GLOBAL = "global";
	
	
//	/** Global application scope. */
//	public static final String SCOPE_GLOBAL_APPLICATION = "global_application";
	
//	/** Upwards scope. */
//	public static final String SCOPE_UPWARDS = "upwards";
	
	
	//-------- attributes --------

	// service description
	
	/** The component internal service name. */
	protected String name;
	
	/** The type. */
	protected ClassInfo type;
	
	/** The service tags to search for. */
	protected Collection<String> tags;
	
	/** Flag if multiple services should be returned. */
	protected boolean multiple;
	
//	/** The multiplex type. */
//	protected ClassInfo multiplextype;
	// Dropped support for v4

	// binding specification
	
	/** The default binding. */
	protected RequiredServiceBinding binding;
	
	/** The list of interceptors. */
	protected List<UnparsedExpression> interceptors;
	
	// nf props for required service
	
	/** The nf props. */
	protected List<NFRPropertyInfo> nfproperties;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo()
	{
		// bean constructor
		
		// Hack!!! Initialize with default values to resemble annotation behavior.
		this(null, null);
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, Class<?> type)
	{
		this(name, type, RequiredServiceInfo.SCOPE_APPLICATION);
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(Class<?> type)
	{
		this(null, type, RequiredServiceInfo.SCOPE_APPLICATION);
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, Class<?> type, String scope)
	{
		this(name, type, false, new RequiredServiceBinding(name, scope), null, null);
	}
	
	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, Class<?> type, boolean multiple, 
		RequiredServiceBinding binding, List<NFRPropertyInfo> nfprops, Collection<String> tags)
	{
		this(name, type!=null ? new ClassInfo(SReflect.getClassName(type)) : null,
			multiple, binding, nfprops, tags);
	}

	/**
	 *  Create a new service info.
	 */
	public RequiredServiceInfo(String name, ClassInfo type, boolean multiple, 
		RequiredServiceBinding binding, List<NFRPropertyInfo> nfprops, Collection<String> tags)
	{
		this.name = name;
		this.type	= type;
		this.multiple = multiple;
		this.binding = binding;
		this.nfproperties = nfprops;
		this.tags = tags;
	}

	//-------- methods --------

	/**
	 *  Get the name.
	 *  @return the name.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 *  Set the name.
	 *  @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 *  Get the type.
	 *  @return The type.
	 */
	public ClassInfo getType()
	{
		return type;
	}

	/**
	 *  Set the type.
	 *  @param type The type to set.
	 */
	public void setType(ClassInfo type)
	{
		this.type = type;
	}
	
	/**
	 *  Get the multiple.
	 *  @return the multiple.
	 */
	public boolean isMultiple()
	{
		return multiple;
	}

	/**
	 *  Set the multiple.
	 *  @param multiple The multiple to set.
	 */
	public void setMultiple(boolean multiple)
	{
		this.multiple = multiple;
	}
	
	/**
	 *  Get the binding.
	 *  @return the binding.
	 */
	public RequiredServiceBinding getDefaultBinding()
	{
		return binding;
	}

	/**
	 *  Set the binding.
	 *  @param binding The binding to set.
	 */
	public void setDefaultBinding(RequiredServiceBinding binding)
	{
		this.binding = binding;
	}
	
	/**
	 *  Add an interceptor.
	 *  @param interceptor The interceptor.
	 */
	public void addInterceptor(UnparsedExpression interceptor)
	{
		if(interceptors==null)
			interceptors = new ArrayList<UnparsedExpression>();
		interceptors.add(interceptor);
	}
	
	/**
	 *  Remove an interceptor.
	 *  @param interceptor The interceptor.
	 */
	public void removeInterceptor(UnparsedExpression interceptor)
	{
		interceptors.remove(interceptor);
	}
	
	/**
	 *  Get the interceptors.
	 *  @return All interceptors.
	 */
	public UnparsedExpression[] getInterceptors()
	{
		return interceptors==null? new UnparsedExpression[0]: 
			interceptors.toArray(new UnparsedExpression[interceptors.size()]);
	}
	
	/**
	 *  Get the nfproperties.
	 *  @return The nfproperties.
	 */
	public List<NFRPropertyInfo> getNFRProperties()
	{
		return nfproperties;
	}

	/**
	 *  Set the nfproperties.
	 *  @param nfproperties The nfproperties to set.
	 */
	public void setNFRProperties(List<NFRPropertyInfo> nfproperties)
	{
		this.nfproperties = nfproperties;
	}

	/**
	 *  Get the tags.
	 *  @return the tags
	 */
	public Collection<String> getTags()
	{
		return tags;
	}

	/**
	 *  Set the tags.
	 *  @param tags The tags to set
	 */
	public void setTags(Collection<String> tags)
	{
		this.tags = tags;
	}
	
	/**
	 *  Check if the scope not remote.
	 *  @return True, scope on the local platform.
	 */
	public static boolean isScopeOnLocalPlatform(String scope)
	{
		return SCOPE_NONE.equals(scope) || SCOPE_LOCAL.equals(scope) || SCOPE_COMPONENT.equals(scope)
			|| SCOPE_APPLICATION.equals(scope) || SCOPE_PLATFORM.equals(scope) || SCOPE_PARENT.equals(scope);
	}
}