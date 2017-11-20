package jadex.transformation.jsonserializer.processors.write;

import java.lang.reflect.Type;
import java.security.cert.Certificate;
import java.util.List;

import jadex.commons.Base64;
import jadex.commons.SReflect;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;
import jadex.commons.transformation.traverser.Traverser.MODE;

/**
 * 
 */
public class JsonCertificateProcessor implements ITraverseProcessor
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
		return SReflect.isSupertype(Certificate.class, clazz);
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 * @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	public Object process(Object object, Type type, Traverser traverser, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, MODE mode, ClassLoader targetcl, Object context)
	{
		try
		{
			JsonWriteContext wr = (JsonWriteContext)context;
			wr.addObject(wr.getCurrentInputObject());
	
			Certificate ste = (Certificate)object;
			byte[] bytes = Base64.encode(ste.getEncoded());
			String enc = new String(bytes);
			
			wr.write("{");
			wr.writeNameString("type", ste.getType());
			wr.write(",");
			wr.writeNameString("encoded", enc);
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
