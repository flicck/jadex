package jadex.transformation.jsonserializer.processors.write;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jadex.commons.SReflect;
import jadex.commons.transformation.binaryserializer.BeanIntrospectorFactory;
import jadex.commons.transformation.traverser.BeanProperty;
import jadex.commons.transformation.traverser.IBeanIntrospector;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;
import jadex.transformation.jsonserializer.JsonTraverser;

/**
 * 
 */
public class JsonBeanProcessor implements ITraverseProcessor
{
	/** Bean introspector for inspecting beans. */
	protected IBeanIntrospector intro = BeanIntrospectorFactory.getInstance().getBeanIntrospector(5000);
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Class<?> clazz, boolean clone, ClassLoader targetcl)
	{
		return true;
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	public Object process(Object object, Class<?> clazz, List<ITraverseProcessor> processors, 
		Traverser traverser, Map<Object, Object> traversed, boolean clone, ClassLoader targetcl, Object context)
	{
//		System.out.println("fp: "+object);
		
		JsonWriteContext wr = (JsonWriteContext)context;
		
		wr.write("{");
		
		if(wr.isWriteClass())
			wr.writeClass(object.getClass());
		
		try
		{
//			System.out.println("cloned: "+object.getClass());
//			ret = object.getClass().newInstance();
			
			traverseProperties(object, traversed, processors, traverser, clone, targetcl, context, intro, !wr.isWriteClass());
		}
		catch(Exception e)
		{
			throw (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
		}
		
		wr.write("}");
		
//		traversed.put(object, null);
		
		return object;
	}
	
	/**
	 *  Clone all properties of an object.
	 */
	protected static void traverseProperties(Object object, Map<Object, Object> cloned, 
		List<ITraverseProcessor> processors, Traverser traverser, boolean clone, 
		ClassLoader targetcl, Object context, IBeanIntrospector intro, boolean first)
	{
		Class<?> clazz = object.getClass();
		JsonWriteContext wr = (JsonWriteContext)context;
		
		Map<String, BeanProperty> props = intro.getBeanProperties(clazz, true, false);

		for(Iterator<String> it=props.keySet().iterator(); it.hasNext(); )
		{
			try
			{
				String name = (String)it.next();
				BeanProperty prop = (BeanProperty)props.get(name);
				if(prop.isReadable() && prop.isWritable())
				{
					Object val = prop.getPropertyValue(object);
					if(val!=null) 
					{
						if(!first)
							wr.write(",");
						first = false;
						wr.write("\"");
						wr.write(name);
						wr.write("\":");
						
						traverser.doTraverse(val, prop.getType(), cloned, processors, clone, targetcl, context);
					}
				}
			}
			catch(Exception e)
			{
				throw (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
			}
		}
	}
}
