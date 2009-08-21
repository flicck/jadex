package jadex.commons.xml.writer;

import jadex.commons.xml.SXML;
import jadex.commons.xml.StackElement;
import jadex.commons.xml.TypeInfo;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

/**
 *  XML writer for conversion of objects to XML.
 */
public class Writer
{
	//-------- static part --------
	
	/** The linefeed separator. */
	public static final String lf = (String)System.getProperty("line.separator");
		
	//-------- attributes --------
	
	/** The object creator. */
	protected IObjectWriterHandler handler;
	
	
	/** The ignored attribute types. */
	protected Set ignoredattrs;
	
	/** The id counter. */
	protected int id;
	
	/** Control flag for generating ids. */
	protected boolean genids;	
	
	/** Control flag for generating indention. */
	protected boolean indent;
	
	//-------- constructors --------

	/**
	 *  Create a new reader.
	 *  @param handler The handler.
	 */
	public Writer(IObjectWriterHandler handler)
	{
		this.handler = handler;
		this.genids = true;
		this.indent = true;
	}
	
	//-------- methods --------
	
	/**
	 *  Write the properties to an xml.
	 *  @param input The input stream.
	 *  @param classloader The classloader.
	 * 	@param context The context.
 	 */
	public void write(Object object, OutputStream out, ClassLoader classloader, final Object context) throws Exception
	{
		Map writtenobs = new HashMap();
		List stack = new ArrayList();
		
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
//		factory.setProperty("javax.xml.stream.isRepairingNamespaces", Boolean.TRUE);
		XMLStreamWriter	writer	= factory.createXMLStreamWriter(out);
		
		writer.writeStartDocument();//"utf-8", "1.0");
		writer.writeCharacters(lf);
		writeObject(writer, object, writtenobs, null, stack, context, classloader);
		writer.writeEndDocument();
		writer.close();
	}
	
	/**
	 *  Write an object to xml.
	 */
	public void writeObject(XMLStreamWriter writer, Object object, Map writtenobs, QName tag, 
		List stack, Object context, ClassLoader classloader) throws Exception
	{
		// Special case null
		if(object==null)
		{
			writeStartObject(writer, tag==null? SXML.NULL: tag, stack.size());
			writeEndObject(writer, stack.size());
			return;
		}
		
//		if(tagname!=null)
//			System.out.println("tagname: "+tagname);
		TypeInfo typeinfo = handler.getTypeInfo(object, getXMLPath(stack), context); 
		if(typeinfo!=null)
		{
			tag = typeinfo.getXMLTag();
		}
		
		if(tag==null)
		{
			tag = handler.getTagName(object, context);
		}
		
		if(genids && writtenobs.containsKey(object))
		{
			writeStartObject(writer, tag, stack.size());
			writer.writeAttribute(SXML.IDREF, (String)writtenobs.get(object));
			writeEndObject(writer, 0);
		}
		else
		{
			// Check for cycle structures, which are not mappable without ids.
			if(writtenobs.containsKey(object))
			{
				boolean rec = false;
				for(int i=0; i<stack.size() && !rec; i++)
				{
					if(object.equals(((StackElement)stack.get(i)).getObject()))
						throw new RuntimeException("Object structure contains cycles: Enable 'genids' mode for serialization.");
				}
			}
			
			WriteObjectInfo wi = handler.getObjectWriteInfo(object, typeinfo, context, classloader);

			// Comment
			
			String comment = wi.getComment();
			if(comment!=null)
			{
				writeIndentation(writer, stack.size());
				writer.writeComment(comment);
				writer.writeCharacters(lf);
			}
			
			writeStartObject(writer, tag, stack.size());
			
			StackElement topse = new StackElement(tag, object);
			stack.add(topse);
			writtenobs.put(object, ""+id);
			if(genids)
				writer.writeAttribute(SXML.ID, ""+id);
			id++;
			
			// Attributes
			
			Map attrs = wi.getAttributes();
			if(attrs!=null)
			{
				for(Iterator it=attrs.keySet().iterator(); it.hasNext(); )
				{
					String propname = (String)it.next();
					String value = (String)attrs.get(propname);
					writer.writeAttribute(propname, value);
				}
			}
			
			if(wi.getContent()==null && (wi.getSubobjects()==null || wi.getSubobjects().size()==0))
			{
				writeEndObject(writer, 0);
			}
			else
			{
				// Content
				
				String content = wi.getContent();
				if(content!=null)
				{
					if(content.indexOf("<")!=-1 || content.indexOf(">")!=-1 || content.indexOf("&")!=-1)
						writer.writeCData(content);
					else
						writer.writeCharacters(content);
				}
				
				// Subobjects
				
				Map subobs = wi.getSubobjects();
				if(subobs==null || subobs.size()==0)
				{
					writeEndObject(writer, 0);
				}
				else
				{	
					writer.writeCharacters(lf);
					
					writeSubobjects(writer, subobs, writtenobs, stack, context, classloader, typeinfo);
					
					writeEndObject(writer, stack.size()-1);
				}
			}
			stack.remove(stack.size()-1);
		}
	}
	
	/**
	 *  Write the subobjects of an object.
	 */
	protected void writeSubobjects(XMLStreamWriter writer, Map subobs, Map writtenobs, 
		List stack, Object context, ClassLoader classloader, TypeInfo typeinfo) throws Exception
	{
		for(Iterator it=subobs.keySet().iterator(); it.hasNext(); )
		{
			Object tmp = it.next();
//			if(WriteObjectInfo.INTERAL_STRUCTURE.equals(tmp))
//				continue;
				
			QName subtag = (QName)tmp;
			Object subob = subobs.get(subtag);
			if(subob instanceof Map)// && ((Map)subob).containsKey(WriteObjectInfo.INTERAL_STRUCTURE))
			{		
				writeStartObject(writer, subtag, stack.size());
				writer.writeCharacters(lf);
				stack.add(new StackElement(subtag, null));
				
				writeSubobjects(writer, (Map)subob, writtenobs, stack, context, classloader, typeinfo);
				
				stack.remove(stack.size()-1);
				writeEndObject(writer, stack.size());
			}
			else if(subob instanceof List)// && ((List)subob).contains(WriteObjectInfo.INTERAL_STRUCTURE))
			{
				writeStartObject(writer, subtag, stack.size());
				writer.writeCharacters(lf);
				stack.add(new StackElement(subtag, null));
				
				List sos = (List)subob;
				for(int i=0; i<sos.size(); i++)
				{
					Object so = sos.get(i);
//					if(WriteObjectInfo.INTERAL_STRUCTURE.equals(so))
//						continue;
					Object[] info = (Object[])so;
					writeObject(writer, info[1], writtenobs, (QName)info[0], stack, context, classloader);
				}			
				
				stack.remove(stack.size()-1);
				writeEndObject(writer, stack.size());
			}	
			else
			{
//				if(subob instanceof Map || subob instanceof List)
//					System.out.println("here");
				writeObject(writer, subob, writtenobs, subtag, stack, context, classloader);
			}
		}
	}

	/**
	 *  Write the start of an object.
	 */
	public void writeStartObject(XMLStreamWriter writer, QName tag, int level) throws Exception
	{
		writeIndentation(writer, level);
			
		String uri = tag.getNamespaceURI();
		String prefix = tag.getPrefix();
//		System.out.println("name"+tag.getLocalPart()+" prefix:"+prefix+" writerprefix:"+writer.getPrefix(uri)+" uri:"+uri);
		
		if(!XMLConstants.NULL_NS_URI.equals(uri))
		{
			if(!prefix.equals(writer.getPrefix(uri)))
			{
				writer.writeStartElement(tag.getPrefix(), tag.getLocalPart(), tag.getNamespaceURI());
				writer.writeNamespace(tag.getPrefix(), tag.getNamespaceURI());
			}
			else
			{
				writer.writeStartElement(tag.getPrefix(), tag.getLocalPart(), tag.getNamespaceURI());
			}
		}
		else
		{
			writer.writeStartElement(tag.getLocalPart());
		}
	}
	
	/**
	 *  Write the end of an object.
	 */
	public void writeEndObject(XMLStreamWriter writer, int level) throws Exception
	{
		writeIndentation(writer, level);
		writer.writeEndElement();
		writer.writeCharacters(lf);
	}
		
	/**
	 *  Write content.
	 */
	public void writeContent(PrintWriter writer, String value)
	{
		writer.write(value);
	}
	
	/**
	 *  Write the indentation.
	 */
	public void writeIndentation(XMLStreamWriter writer, int level) throws Exception
	{
		if(indent)
		{
			for(int i=0; i<level; i++)
				writer.writeCharacters("\t");
		}
	}
	
	/**
	 *  Get the xml path for a stack.
	 *  @param stack The stack.
	 *  @return The string representig the xml stack (e.g. tag1/tag2/tag3)
	 */
	protected QName[] getXMLPath(List stack)
	{
		QName[] ret = new QName[stack.size()];
		for(int i=0; i<stack.size(); i++)
		{
			ret[i] = ((StackElement)stack.get(i)).getTag();
		}
		return ret;
		
//		StringBuffer ret = new StringBuffer();
//		for(int i=0; i<stack.size(); i++)
//		{
//			ret.append(((StackElement)stack.get(i)).getTag());
//			if(i<stack.size()-1)
//				ret.append("/");
//		}
//		return ret.toString();
	}
	
	/**
	 *  Convert to a string.
	 */
	public static String objectToXML(Writer writer, Object val, ClassLoader classloader)
	{
		return new String(objectToByteArray(writer, val, classloader));
	}
	
	/**
	 *  Convert to a byte array.
	 */
	public static byte[] objectToByteArray(Writer writer, Object val, ClassLoader classloader)
	{
		try
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			writer.write(val, bos, classloader, null);
			byte[] ret = bos.toByteArray();
			bos.close();
			return ret;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
