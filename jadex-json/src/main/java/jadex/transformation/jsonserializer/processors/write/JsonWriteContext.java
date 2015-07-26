package jadex.transformation.jsonserializer.processors.write;

import java.nio.charset.Charset;

import jadex.commons.SReflect;
import jadex.transformation.jsonserializer.JsonTraverser;

/**
 * 
 */
public class JsonWriteContext
{
	protected StringBuffer buffer = new StringBuffer();
	
	protected boolean writeclass = true;
	
	/**
	 * 
	 */
	public JsonWriteContext(boolean writeclass)
	{
		this.writeclass = writeclass;
	}

	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext write(String str)
	{
		buffer.append(str);
		return this;
	}
	
	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext writeString(String str)
	{
		buffer.append(encodeJsonString(str));
		return this;
	}
	
	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext writeNameString(String name, String str)
	{
		buffer.append(encodeJsonString(name));
		buffer.append(":");
		buffer.append(encodeJsonString(str));
		return this;
	}
	
	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext writeNameValue(String name, Object val)
	{
		buffer.append(encodeJsonString(name));
		buffer.append(":");
		buffer.append(val);
		return this;
	}
	
	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext writeNameValue(String name, int val)
	{
		buffer.append(encodeJsonString(name));
		buffer.append(":");
		buffer.append(val);
		return this;
	}
	
	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext writeNameValue(String name, long val)
	{
		buffer.append(encodeJsonString(name));
		buffer.append(":");
		buffer.append(val);
		return this;
	}
	
	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext writeNameValue(String name, boolean val)
	{
		buffer.append(encodeJsonString(name));
		buffer.append(":");
		buffer.append(val);
		return this;
	}
	
	/**
	 *  Write a string to the buffer.
	 */
	public JsonWriteContext writeNameValue(String name, Class<?> val)
	{
		buffer.append(encodeJsonString(name));
		buffer.append(":");
		buffer.append("\"").append(SReflect.getClassName(val)).append("\"");
		return this;
	}
	
	/**
	 *  Write the classname.
	 *  @param object
	 */
	public void writeClass(Class<?> clazz)
	{
		write("\"").write(JsonTraverser.CLASSNAME_MARKER).write("\"");
		write(":");
		write("\"").write(SReflect.getClassName(clazz)).write("\"");
	}
	
	/**
	 * 
	 */
	public String getString()
	{
//		Charset.forName("UTF-8").encode(buffer.toString()).array();

		return buffer.toString();
	}

	/**
	 *  Get the writeclass. 
	 *  @return The writeclass
	 */
	public boolean isWriteClass()
	{
		return writeclass;
	}
	

	/**
	 * 
	 */
	public static String encodeJsonString(String string) 
	{
		if(string == null || string.length() == 0)
			return "\"\"";

		char c = 0;
		int i;
		int len = string.length();
		StringBuilder sb = new StringBuilder(len + 4);
		String t;

		sb.append('"');
		for(i = 0; i < len; i += 1) 
		{
			c = string.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				sb.append('\\');
				sb.append(c);
				break;
			case '/':
				// if (b == '<') {
				sb.append('\\');
				// }
				sb.append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				if(c < ' ') 
				{
					t = "000" + Integer.toHexString(c);
					sb.append("\\u" + t.substring(t.length() - 4));
				} 
				else 
				{
					sb.append(c);
				}
			}
		}
		
		sb.append('"');
		return sb.toString();
	}
}
