package jadex.xml.tutorial.example08;

import jadex.commons.SUtil;
import jadex.xml.AccessInfo;
import jadex.xml.AttributeInfo;
import jadex.xml.MappingInfo;
import jadex.xml.ObjectInfo;
import jadex.xml.SubobjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.XMLInfo;
import jadex.xml.bean.BeanObjectReaderHandler;
import jadex.xml.bean.BeanObjectWriterHandler;
import jadex.xml.reader.Reader;
import jadex.xml.writer.Writer;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 *  Main class to execute tutorial lesson.
 */
public class Main
{
	/**
	 *  Main method for using the xml reader/writer.
	 */
	public static void main(String[] args) throws Exception
	{
		// Reading a simple xml in a Java class with public bean getter/setters.
		// Here the xml comment of the invoice is directed mapped to the description
		// via an attribute mapping for the comment.
		
		// Create minimal type infos for types that need to be mapped
		Set typeinfos = new HashSet();
		typeinfos.add(new TypeInfo(new XMLInfo("invoice"), new ObjectInfo(Invoice.class),
			new MappingInfo(null, new AttributeInfo(new AccessInfo((String)null, "description")), 
			null, null, new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo("product-key", "key"))		
			})));
		
		// Create an xml reader with standard bean object reader and the
		// custom typeinfos
		Reader xmlreader = new Reader(new BeanObjectReaderHandler(typeinfos));
		InputStream is = SUtil.getResource("jadex/xml/tutorial/example08/data.xml", null);
		Object object = xmlreader.read(is, null, null);
		is.close();
		
		// Write the xml to the output file.
		Writer xmlwriter = new Writer(new BeanObjectWriterHandler(false, true, typeinfos), false, true);
		OutputStream os = new FileOutputStream("out.xml");
		xmlwriter.write(object, os, null, null);
		os.close();
		
		// And print out the result.
		System.out.println("Read object: "+object);
		System.out.println("Wrote object to out.xml");
	}
}
