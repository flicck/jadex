package jadex.extension.rs.publish;

import jadex.xml.bean.JavaReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

/**
 *  Body reader for jersey. Allows to use the Jadex XML Codec
 *  for reading XML for parameters. 
 */
@Provider
public class JadexXMLBodyReader implements MessageBodyReader<Object>
{
	/**
	 *  Test if the object is readable.
	 */
	public boolean isReadable(Class<?> type, Type generictype, 
		Annotation[] annotations, MediaType mediatype) 
	{       
		return mediatype.equals(MediaType.APPLICATION_XML_TYPE);
	}
	 
	/**
	 *  Read the object from the 
	 */
	public Object readFrom(Class<Object> type, Type generictype,
		Annotation[] annotations, MediaType mediatype,
		MultivaluedMap<String, String> httpheaders, InputStream entitystream)
		throws IOException, WebApplicationException
	{
		// todo: classloader?
		return JavaReader.objectFromInputStream(entitystream, null);
	}
}