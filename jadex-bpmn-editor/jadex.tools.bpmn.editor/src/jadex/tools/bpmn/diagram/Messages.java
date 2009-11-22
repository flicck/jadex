package jadex.tools.bpmn.diagram;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
	private static final String BUNDLE_NAME = "jadex.tools.bpmn.diagram.messages"; //$NON-NLS-1$
	
	public static String JadexActivityAnnotationDecorator_Class_Label;
	public static String JadexActivityAnnotationDecorator_Parameter_Label;
	public static String CommonSection_label_text;
	public static String JadexUserTaskActivityPropertySection_update_command_name;
	public static String JadexUserTaskActivityPropertySection_add_command_name;

	public static String JadexCommonPropertySection_ButtonAdd_Label;
	public static String JadexCommonPropertySection_ButtonDelete_Label;
	
	public static String JadexUserTaskActivityPropertySection_delete_command_name;

	public static String JadexCommonPropertySection_InvalidEditColumn_Message;

	public static String JadexUserTaskActivityPropertySection_NewParameterName_Value;
	public static String JadexCommonPropertySection_update_eannotation_command_name;
	public static String JadexCommonParameterListSection_ParameterTable_Label;

	public static String JadexSequenceMappingSection_MappingTable_Label;
	public static String ActivityParameterListSection_ImplementationClass_label;
	public static String ActivityParameterListSection_WrongElementDelimiter_message;
	
	static
	{
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages()
	{
	}
}
