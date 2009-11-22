/**
 * 
 */
package jadex.tools.bpmn.editor.properties;

import jadex.tools.bpmn.diagram.Messages;


/**
 * @author Claas Altschaffel
 * 
 */
public class JadexSequenceMappingSection extends
		JadexAbstract2ColumnTablePropertySection
{

	/**
	 * Default constructor, initializes super class
	 */
	public JadexSequenceMappingSection()
	{
		super(JadexCommonPropertySection.JADEX_SEQUENCE_ANNOTATION, JadexCommonPropertySection.JADEX_MAPPING_LIST_DETAIL,
				Messages.JadexSequenceMappingSection_MappingTable_Label);
	}

}
