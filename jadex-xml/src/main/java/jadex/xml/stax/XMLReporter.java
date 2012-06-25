package jadex.xml.stax;

public interface XMLReporter {

  public void report(String message, String errorType, Object relatedInformation, ILocation location) 
    throws Exception;
}
