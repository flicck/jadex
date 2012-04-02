package jadex.commons.transformation.binaryserializer;

import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *  Codec for encoding and decoding Date objects.
 *
 */
public class DateCodec extends AbstractCodec
{
	/**
	 *  Tests if the decoder can decode the class.
	 *  @param clazz The class.
	 *  @return True, if the decoder can decode this class.
	 */
	public boolean isApplicable(Class clazz)
	{
		return Date.class.equals(clazz);
	}
	
	/**
	 *  Creates the object during decoding.
	 *  
	 *  @param clazz The class of the object.
	 *  @param context The decoding context.
	 *  @return The created object.
	 */
	public Object createObject(Class clazz, DecodingContext context)
	{
		ByteBuffer buf = context.getByteBuffer(8);
		buf.order(ByteOrder.BIG_ENDIAN);
		Date ret = new Date(buf.getLong());
		return ret;
	}
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Class<?> clazz, boolean clone)
	{
		return isApplicable(clazz);
	}
	
	/**
	 *  Encode the object.
	 */
	public Object encode(Object object, Class<?> clazz, List<ITraverseProcessor> processors, 
			Traverser traverser, Map<Object, Object> traversed, boolean clone, EncodingContext ec)
	{
		long time = ((Date) object).getTime();
		ByteBuffer buf = ec.getByteBuffer(8);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.putLong(time);
		
		return object;
	}
}
