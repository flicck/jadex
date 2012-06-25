package jadex.xml.writer;

/**
 * Factory implementation for the Java SE Environment. Uses {@link Writer}.
 */
public class XMLWriterFactoryDesktop extends XMLWriterFactory
{
	// ---------- methods ------------

	/**
	 * Create a new reader (with genids=true and indent=true).
	 */
	public AWriter createWriter()
	{
		return new Writer();
	}

	/**
	 * Creates a new XML Reader.
	 * 
	 * @param genids
	 *            flag for generating ids
	 * 
	 * @return reader
	 */
	public AWriter createWriter(boolean genids)
	{
		return new Writer(genids);
	}

	/**
	 * Creates a new default XML Reader.
	 * 
	 * @param genids
	 *            flag for generating ids
	 * @param indents
	 * 
	 * @return reader
	 */
	public AWriter createWriter(boolean genids, boolean indents)
	{
		return new Writer(genids, indents);
	}

}
