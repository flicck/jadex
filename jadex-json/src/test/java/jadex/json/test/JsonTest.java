package jadex.json.test;

import jadex.transformation.jsonserializer.JsonTraverser;

/**
 * Testcases for writer and reader.
 */
public class JsonTest extends jadex.commons.transformation.Test
{
	//-------- methods --------

	/**
	 * 
	 */
	public Object doWrite(Object wo)
	{
		return JsonTraverser.objectToByteArray(wo, null);
	}

	/**
	 * 
	 */
	public Object doRead(Object ro)
	{
		return JsonTraverser.objectFromByteArray((byte[])ro, null, null);
	}

	/**
	 * Main for testing single methods.
	 */
	public static void main(String[] args)
	{
		JsonTest t = new JsonTest();
		t.performTests();
	}
}