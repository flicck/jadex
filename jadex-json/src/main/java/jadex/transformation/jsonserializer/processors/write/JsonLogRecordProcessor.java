package jadex.transformation.jsonserializer.processors.write;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jadex.commons.SReflect;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;

/**
 * 
 */
public class JsonLogRecordProcessor implements ITraverseProcessor
{	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Type type, boolean clone, ClassLoader targetcl)
	{
		Class<?> clazz = SReflect.getClass(type);
		return SReflect.isSupertype(LogRecord.class, clazz);
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	public Object process(Object object, Type type, List<ITraverseProcessor> processors, 
		Traverser traverser, Map<Object, Object> traversed, boolean clone, ClassLoader targetcl, Object context)
	{
		JsonWriteContext wr = (JsonWriteContext)context;
	
		LogRecord rec = (LogRecord)object;
		
//		traversed.put(object, ret);
		
		wr.write("{");
		wr.write("\"level\":");
		Level level = rec.getLevel();
		traverser.doTraverse(level, level.getClass(), traversed, processors, clone, targetcl, context);
		wr.write(",");
		wr.writeNameString("msg", rec.getMessage());
		wr.write(",");
		wr.writeNameValue("millis", rec.getMillis());
//		wr.write(",\"msg\":\"").write(rec.getMessage()).write("\"");
//		wr.write(",\"millis\":").write(""+rec.getMillis());
		if(wr.isWriteClass())
			wr.write(",").writeClass(object.getClass());
		wr.write("}");
		
		return object;
	}
}
