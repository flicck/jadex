package jadex.transformation.jsonserializer.processors.write;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import jadex.commons.SReflect;
import jadex.commons.collection.MultiCollection;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;
import jadex.commons.transformation.traverser.Traverser.MODE;

/**
 * 
 */
public class JsonMultiCollectionProcessor implements ITraverseProcessor
{
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Type type, ClassLoader targetcl, Object context)
	{
		Class<?> clazz = SReflect.getClass(type);
		return SReflect.isSupertype(MultiCollection.class, clazz);
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 * @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	@SuppressWarnings("rawtypes")
	public Object process(Object object, Type type, Traverser traverser, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, MODE mode, ClassLoader targetcl, Object context)
	{
		try
		{
			JsonWriteContext wr = (JsonWriteContext)context;
			wr.addObject(wr.getCurrentInputObject());
			
			MultiCollection<?,?> mc = (MultiCollection<?,?>)object;
			
			wr.write("{");
			
			Field typefield = MultiCollection.class.getDeclaredField("type");
			typefield.setAccessible(true);
			Class<?> ctype = (Class)typefield.get(mc);
			wr.writeNameValue("type", ctype);
			
			wr.write(",\"map\":");
			Field mapfield = MultiCollection.class.getDeclaredField("map");
			mapfield.setAccessible(true);
			Map<?,?> map = (Map<?,?>)mapfield.get(mc);
			traverser.doTraverse(map, map.getClass(), conversionprocessors, processors, mode, targetcl, context);
			
			if(wr.isWriteClass())
				wr.write(",").writeClass(object.getClass());
			if(wr.isWriteId())
				wr.write(",").writeId();
			
			wr.write("}");
			
			return object;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
