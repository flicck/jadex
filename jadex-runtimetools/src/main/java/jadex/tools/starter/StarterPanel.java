package jadex.tools.starter;

import jadex.base.SComponentFactory;
import jadex.bridge.IArgument;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.ILoadableComponentModel;
import jadex.bridge.IReport;
import jadex.commons.FixedJComboBox;
import jadex.commons.Properties;
import jadex.commons.Property;
import jadex.commons.SGUI;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.collection.MultiCollection;
import jadex.commons.collection.SCollection;
import jadex.commons.concurrent.IResultListener;
import jadex.javaparser.javaccimpl.JavaCCExpressionParser;
import jadex.service.library.ILibraryService;
import jadex.tools.common.AgentSelectorDialog;
import jadex.tools.common.ElementPanel;
import jadex.tools.common.GuiProperties;
import jadex.tools.common.JValidatorTextField;
import jadex.tools.common.ParserValidator;
import jadex.tools.jcc.AgentControlCenter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.help.CSH;
import javax.help.HelpBroker;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The starter gui allows for starting components platform independently.
 */
public class StarterPanel extends JPanel
{
	//-------- static part --------

	/** The image icons. */
	protected static UIDefaults	icons	= new UIDefaults(new Object[]
	{
		"Browse", SGUI.makeIcon(StarterPanel.class,	"/jadex/tools/common/images/dots_small.png"),
		"delete", SGUI.makeIcon(StarterPanel.class,	"/jadex/tools/common/images/delete_small.png")
	});

	//-------- attributes --------

	/** The model. */
	protected ILoadableComponentModel model;

	/** The last loaded filename. */
	protected String lastfile;

	/** The selected parent (if any). */
	protected IComponentIdentifier	parent;

	//-------- gui widgets --------

	/** The filename. */
	protected JTextField filename;

//	/** The file chooser. */
//	protected JFileChooser filechooser;

	/** The configuration. */
	protected JComboBox config;

	/** The suspend mode. */
	protected JCheckBox suspend;

	/** The component type. */
	protected JTextField componentname;
	protected JLabel componentnamel;
	protected JTextField parenttf;

//	/** The application name. */
//	protected JComboBox appname;
//	protected JLabel appnamel;
//	protected DefaultComboBoxModel appmodel;
	
	protected JLabel confl;
	protected JLabel confdummy = new JLabel("Component Name"); // Hack! only for reading sizes
	protected JLabel filenamel;
	
	/** The component name generator flag. */
	protected JCheckBox genname;

	/** The component arguments. */
	protected JPanel arguments;
	protected List argelems;
	
	/** The component results. */
	protected JPanel results;
	protected List reselems;
	protected JCheckBox storeresults;
	protected JComboBox selectavail;
	protected MultiCollection resultsets;

	/** The start button. */
	protected JButton start;

	/** The description panel. */
	protected ElementPanel modeldesc;

	/** The component specific panel. */
	protected JPanel componentpanel;
	
//	/** The application specific panel. */
//	protected JPanel apppanel;
	
	/** The starter plugin. */
	protected StarterPlugin	starter;

	/** The spinner for the number of components to start. */
	protected JSpinner numcomponents;
	
	//-------- constructors --------

	/**
	 * Open the GUI.
	 * @param starter The starter.
	 */
	public StarterPanel(final StarterPlugin starter)
	{
		super(new BorderLayout());
		this.starter	= starter;
		this.resultsets = new MultiCollection();
		
		JPanel content = new JPanel(new GridBagLayout());

	   	// The browse button.
		//final JButton browse = new JButton("browse...");
//		final JButton browse = new JButton(icons.getIcon("Browse"));
//		browse.setToolTipText("Browse via file requester to locate a model");
//		browse.setMargin(new Insets(0,0,0,0));
//		// Create the filechooser.
//		// Hack!!! might trhow exception in applet / webstart
//		try
//		{
//			filechooser = new JFileChooser(".");
//			filechooser.setAcceptAllFileFilterUsed(true);
//			javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter()
//			{
//				public String getDescription()
//				{
//					return "Active components";
//				}
//
//				public boolean accept(File f)
//				{
//					String name = f.getName();
////					return f.isDirectory() || name.endsWith(SXML.FILE_EXTENSION_AGENT) || name.endsWith(SXML.FILE_EXTENSION_CAPABILITY);
////					boolean	ret	= f.isDirectory() || componentfactory.isLoadable(name) || appfactory.isLoadable(name);
//					boolean	ret	= f.isDirectory() || SComponentFactory.isLoadable(starter.getJCC().getServiceContainer(), name);
//
////					Thread.currentThread().setContextClassLoader(oldcl);
//
//					return ret;
//				}
//			};
//			filechooser.addChoosableFileFilter(filter);
//		}
//		catch(SecurityException e)
//		{
//			browse.setEnabled(false);
//		}
//		browse.addActionListener(new ActionListener()
//		{
//			public void actionPerformed(ActionEvent ae)
//			{
//				if(filechooser.showDialog(SGUI.getWindowParent(StarterPanel.this)
//					, "Load")==JFileChooser.APPROVE_OPTION)
//				{
//					File file = filechooser.getSelectedFile();
//					String	model	= file!=null ? ""+file : null;
//
////					if(file!=null && file.getName().endsWith(".jar"))
////					{
////						// Start looking into the jar-file for description-files
////						try
////						{
////							DynamicURLClassLoader.addURLToInstance(new URL("file", "", file.toString()));
////
////							JarFile jarFile = new JarFile(file);
////							Enumeration e = jarFile.entries();
////							java.util.List	models	= new ArrayList();
////							while (e.hasMoreElements())
////							{
////								ZipEntry jarFileEntry = (ZipEntry) e.nextElement();
////								if(SXML.isJadexFilename(jarFileEntry.getName()))
////								{
////									models.add(jarFileEntry.getName());
////								}
////							}
////							jarFile.close();
////
////							if(models.size()>1)
////							{
////								Object[]	choices	= models.toArray(new String[models.size()]);
////								JTreeDialog td = new JTreeDialog(
////									null,
//////									(Frame)StarterGui.this.getParent(),
////									"Select Model", true,
////									"Select an model to load:",
////									(String[])choices, (String)choices[0]);
////								td.setVisible(true);
////								model = td.getResult();
////							}
////							else if(models.size()==1)
////							{
////								model	= (String)models.get(0);
////							}
////							else
////							{
////								model	= null;
////							}
////						}
////						catch(Exception e)
////						{
////							//e.printStackTrace();
////						}
////					}
//
//					//System.out.println("... load model: "+model);
////					lastfile	= model;
//					loadModel(model);
//				}
//			}
//		});

		// Create the filename combo box.
		filename = new JTextField();
		filename.setEditable(false);
		ActionListener filelistener = new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				loadModel(filename.getText());
			}
		};
		filename.addActionListener(filelistener);

		// The configuration.
		config = new JComboBox();
		config.setToolTipText("Choose the configuration to start with");
		config.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				refreshArguments();
			}
		});

		// The suspend mode.
		suspend = new JCheckBox("Start suspended");
		suspend.setToolTipText("Start in suspended mode");

		// The component name.
		componentname = new JTextField();
		
//		// The application name.
//		appmodel = new DefaultComboBoxModel();
//		appmodel.addElement("");
//		IContextService cs = (IContextService)starter.getJCC().getServiceContainer().getService(IContextService.class);
//		if(cs!=null)
//		{
//			cs.addContextListener(new IChangeListener()
//			{
//				public void changeOccurred(jadex.commons.ChangeEvent event)
//				{
//					if(IContextService.EVENT_TYPE_CONTEXT_CREATED.equals(event.getType()))
//					{
//						appmodel.addElement(((IContext)event.getValue()).getName());
//					}
//					else if(IContextService.EVENT_TYPE_CONTEXT_DELETED.equals(event.getType()))
//					{
//						appmodel.removeElement(((IContext)event.getValue()).getName());
//					}
//				}
//			});
//		}
//		appname = new JComboBox();

		// The generate flag for the componentname;
		genname = new JCheckBox("Auto generate", false);
		genname.setToolTipText("Auto generate the component instance name");
		genname.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				componentname.setEditable(!genname.isSelected());
				numcomponents.setEnabled(genname.isSelected());
			}
		});
		
		numcomponents = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
		((JSpinner.DefaultEditor)numcomponents.getEditor()).getTextField().setColumns(4);
		
		// The arguments.
		arguments = new JPanel(new GridBagLayout());
		
		// The results.
		results = new JPanel(new GridBagLayout());

		// The reload button.
		final JButton reload = new JButton("Reload");
		reload.setToolTipText("Reload the current model");
		reload.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				reloadModel(lastfile);
			}
		});

		int mw = (int)reload.getMinimumSize().getWidth();
		int pw = (int)reload.getPreferredSize().getWidth();
		int mh = (int)reload.getMinimumSize().getHeight();
		int ph = (int)reload.getPreferredSize().getHeight();

		// The start button.
		this.start = new JButton("Start");
		start.setToolTipText("Start selected model");
		start.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				if(model!=null)
				{
					String configname = (String)config.getModel().getSelectedItem();
					Map args = SCollection.createHashMap();
					String errortext = null;
					for(int i=0; i<argelems.size(); i++)
					{
						String argname = ((JLabel)arguments.getComponent(i*4+1)).getText();
						String argval = ((JTextField)arguments.getComponent(i*4+3)).getText();
						if(argval.length()>0)
						{
							Object arg = null;
							try
							{
								ILibraryService ls = (ILibraryService)StarterPanel.this.starter.getJCC().getServiceContainer().getService(ILibraryService.class);
								arg = new JavaCCExpressionParser().parseExpression(argval, null, null, ls.getClassLoader()).getValue(null);
							}
							catch(Exception e)
							{
								if(errortext==null)
									errortext = "Error within argument expressions:\n";
								errortext += argname+" "+e.getMessage()+"\n";
							}
							args.put(argname, arg);
						}
					}
					if(errortext!=null)
					{
						JOptionPane.showMessageDialog(SGUI.getWindowParent(StarterPanel.this), errortext, 
							"Display Problem", JOptionPane.INFORMATION_MESSAGE);
					}
					else
					{
//						if(model instanceof ApplicationModel)
//						{
////							IApplicationFactory fac = starter.getJCC().getComponent().getPlatform().getApplicationFactory();
//							try
//							{
//								SComponentFactory.createApplication(starter.getJCC().getServiceContainer(), (String)appname.getSelectedItem(), filename.getText(), configname, args);
//							}
//							catch(Exception e)
//							{
//								e.printStackTrace();
//								JOptionPane.showMessageDialog(SGUI.getWindowParent(StarterPanel.this), "Could not start application: "+e, 
//									"Application Problem", JOptionPane.INFORMATION_MESSAGE);
//							}
//						}
//						else
						{
//							IApplicationContext ac = null;
//							final String apn = (String)appname.getSelectedItem();
//							if(apn!=null && apn.length()>0)
//							{
//								IContextService cs = (IContextService)starter.getJCC().getServiceContainer().getService(IContextService.class);
//								if(cs!=null)
//								{
//									ac = (IApplicationContext)cs.getContext(apn);
//								}
//							}	
							String typename = /*ac!=null? ac.getComponentType(filename.getText()):*/ filename.getText();
							final String fullname = model.getPackage()+"."+model.getName();
//							if(typename==null)
//							{
//								JOptionPane.showMessageDialog(SGUI.getWindowParent(StarterPanel.this), "Could not resolve component type: "
//									+filename.getText()+"\n in application: "+ac.getName(), 
//									"Component Type Problem", JOptionPane.INFORMATION_MESSAGE);
//							}
//							else
							{
								IResultListener killlistener = null;
								final ILoadableComponentModel mymodel = model;
								if(storeresults!=null && storeresults.isSelected())
								{
									killlistener = new IResultListener()
									{
										public void resultAvailable(final Object source, final Object result)
										{
											SwingUtilities.invokeLater(new Runnable()
											{
												public void run()
												{
//													System.out.println("fullname: "+fullname+" "+model.getFilename());
													String tmp = (String)mymodel.getPackage()+"."+mymodel.getName();
													resultsets.put(tmp, new Object[]{source, result});
													if(model!=null && fullname.equals(model.getPackage()+"."+model.getName()))
													{
														selectavail.addItem(source);
														refreshResults();
													}
												}
											});
										}
										
										public void exceptionOccurred(Object source, Exception exception)
										{
											// todo?!
//											resultsets.put(typename, exception);
										}
									};
								}
								
								String an = genname.isSelected()?  null: componentname.getText();
								if(an==null) // i.e. name auto generate
								{
									int max = ((Integer)numcomponents.getValue()).intValue();
									for(int i=0; i<max; i++)
									{
//										if(ac!=null)
//										{
//											ac.createComponent(an, typename, configname, args, suspend.isSelected(), false, null, null);
//										}
//										else
										{
											starter.createComponent(typename, an, configname, args, suspend.isSelected(), killlistener);
										}
									}
								}
								else
								{
//									if(ac!=null)
//									{
//										ac.createComponent(an, typename, configname, args, suspend.isSelected(), false, null, null);
//									}
//									else
									{
										starter.createComponent(typename, an, configname, args, suspend.isSelected(), killlistener);
									}
								}
							}
						}
					}
				}
			}
		});
		start.setMinimumSize(new Dimension(mw, mh));
		start.setPreferredSize(new Dimension(pw, ph));

		// The reset button.
		final JButton reset = new JButton("Reset");
		reset.setToolTipText("Reset all fields");
		reset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent ae)
			{
				reset();
			}
		});
		reset.setMinimumSize(new Dimension(mw, mh));
		reset.setPreferredSize(new Dimension(pw, ph));

		// The description panel.
		modeldesc = new ElementPanel("Description", null);
		ChangeListener desclistener = new ChangeListener()
		{
			public void stateChanged(ChangeEvent ce)
			{
				Object id = modeldesc.getId(modeldesc.getSelectedComponent());
				if(id instanceof String)
				{
					//System.out.println("SystemEvent: "+id);
					loadModel((String)id);
					updateGuiForNewModel((String)id);
				}
			}
		};
		modeldesc.addChangeListener(desclistener);
		modeldesc.setMinimumSize(new Dimension(200, 150));
		modeldesc.setPreferredSize(new Dimension(400, 150));

		// Avoid panel being not resizeable when long filename is displayed
		filename.setMinimumSize(filename.getMinimumSize());

		int y = 0;
	
		componentpanel = new JPanel(new GridBagLayout());
		componentnamel = new JLabel("Component name");
		componentpanel.add(componentnamel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(0, 0, 0, 2), 0, 0));
		JPanel tmp = new JPanel(new BorderLayout());
		tmp.add(componentname, BorderLayout.CENTER);
		JPanel tmp2 = new JPanel(new BorderLayout());
		tmp2.add(genname, BorderLayout.WEST);
		tmp2.add(numcomponents, BorderLayout.EAST);
		tmp.add(tmp2, BorderLayout.EAST);
		componentpanel.add(tmp, new GridBagConstraints(1, 0, 4, 1, 1, 0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(0, 2, 2, 2), 0, 0));
			
		componentpanel.add(new JLabel("Parent"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 0, 0, 2), 0, 0));
		parenttf	= new JTextField();
		parenttf.setEditable(false);
		componentpanel.add(parenttf, new GridBagConstraints(1, 1, 2, 1, 1, 0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(2, 2, 0, 2), 0, 0));
		
		JButton	chooseparent	= new JButton(icons.getIcon("Browse"));
		chooseparent.setMargin(new Insets(0,0,0,0));
		chooseparent.setToolTipText("Choose parent");
		componentpanel.add(chooseparent, new GridBagConstraints(3, 1, 1, 1, 0, 0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(2, 2, 0, 2), 0, 0));
		final AgentSelectorDialog	agentselector	= new AgentSelectorDialog(this, ((AgentControlCenter)starter.getJCC()).getAgent());
		chooseparent.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				IComponentIdentifier newparent	= agentselector.selectAgent(parent);
				if(newparent!=null)
					setParent(newparent);
			}
		});
		JButton	clearparent	= new JButton(icons.getIcon("delete"));
		clearparent.setMargin(new Insets(0,0,0,0));
		clearparent.setToolTipText("Clear parent");
		componentpanel.add(clearparent, new GridBagConstraints(4, 1, 1, 1, 0, 0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(2, 2, 0, 2), 0, 0));
		clearparent.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				setParent(null);
			}
		});

			
//		apppanel = new JPanel(new GridBagLayout());
//		appnamel = new JLabel("Application name");
//		apppanel.add(appnamel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
//			GridBagConstraints.BOTH, new Insets(0, 0, 0, 2), 0, 0));
//		apppanel.add(appname, new GridBagConstraints(1, 0, 4, 1, 1, 0, GridBagConstraints.EAST,
//			GridBagConstraints.BOTH, new Insets(0, 2, 0, 2), 0, 0));
		
		JPanel upper = new JPanel(new GridBagLayout());
		upper.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), " Settings "));
		filenamel = new JLabel("Filename");
		upper.add(filenamel, new GridBagConstraints(0, y, 1, 1, 0, 0,
			GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,2,2,2), 0, 0));
		upper.add(filename, new GridBagConstraints(1, y, 4, 1, 1, 0,
			GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,2,2,2), 0, 0));
//		upper.add(browse, new GridBagConstraints(4, y, 1, 1, 0, 0,
//			GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2,2,2,2), 0, 0));
		y++;
		confl = new JLabel("Configuration");
		upper.add(confl, new GridBagConstraints(0, y, 1, 1, 0, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		upper.add(config, new GridBagConstraints(1, y, 1, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		upper.add(suspend, new GridBagConstraints(2, y, 3, 1, 0, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		y++;
		upper.add(componentpanel, new GridBagConstraints(0, y, 5, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		y++;
//		upper.add(apppanel, new GridBagConstraints(0, y, 5, 1, 1, 0, GridBagConstraints.WEST,
//			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

		y = 0;
		content.add(upper, new GridBagConstraints(0, y, 5, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		y++;
		content.add(arguments, new GridBagConstraints(0, y, 5, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		y++;
		content.add(results, new GridBagConstraints(0, y, 5, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		componentnamel.setMinimumSize(confl.getMinimumSize());
		componentnamel.setPreferredSize(confl.getPreferredSize());

		/*y++;
		componentnamel = new JLabel("Component name");
		content.add(componentnamel, new GridBagConstraints(0, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		JPanel tmp = new JPanel(new BorderLayout());
		tmp.add(componentname, "Center");
		tmp.add(gencomponentname, "East");
		//content.add(componentname, new GridBagConstraints(1, y, 2, 1, 1, 0, GridBagConstraints.WEST,
		//			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		content.add(tmp, new GridBagConstraints(1, y, 4, 1, 0, 0, GridBagConstraints.EAST,
					GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

		y++;
		argumentsl = new JLabel("Arguments");
		content.add(argumentsl, new GridBagConstraints(0, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		content.add(arguments, new GridBagConstraints(1, y, 4, 1, 1, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));*/

		/*y++;
		content.add(new JButton("1"), new GridBagConstraints(0, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		content.add(new JButton("2"), new GridBagConstraints(1, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		content.add(new JButton("3"), new GridBagConstraints(2, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		content.add(new JButton("4"), new GridBagConstraints(3, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		content.add(new JButton("5"), new GridBagConstraints(4, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
*/
		JPanel buts = new JPanel(new GridBagLayout());
		buts.add(start, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		buts.add(reload, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		buts.add(reset, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));

		HelpBroker hb = GuiProperties.setupHelp(this, "tools.starter");
		if(hb!=null)
		{
			JButton help = new JButton("Help");
			help.setToolTipText("Activate JavaHelp system");
			help.addActionListener(new CSH.DisplayHelpFromSource(hb));
			help.setMinimumSize(new Dimension(mw, mh));
			help.setPreferredSize(new Dimension(pw, ph));
			buts.add(help, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 2, 2, 2), 0, 0));
		}

		//content.add(prodmode, new GridBagConstraints(3, 4, 1, 1, 1, 0,
		//	GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,2,2,2), 0, 0));
		y++;
		content.add(buts, new GridBagConstraints(0, y, 5, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(2, 2, 2, 2), 0, 0));

		y++;
		content.add(modeldesc, new GridBagConstraints(0, y, 5, 1, 1, 1, GridBagConstraints.CENTER,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));

		this.add("Center", content);
	}

	/**
	 *  Reload the model.
	 *  @param adf The adf.
	 */
	public void reloadModel(String adf)
	{
		if(lastfile==null)
			return;
		
		// todo: remove this hack
//		String cachename = lastfile.substring(0, lastfile.length()-3)+"cam";
//		SXML.clearModelCache(cachename);
		
		String toload = lastfile;
		lastfile = null;
		loadModel(toload);
	}
	
	/**
	 *  Load an component model.
	 *  @param adf The adf to load.
	 */
	public void loadModel(final String adf)
	{
		// Don't load same model again (only on reload).
		if(adf!=null && adf.equals(lastfile))
			return;
		
		//System.out.println("loadModel: "+adf+" "+modelname.getActionListeners().length+" "+SUtil.arrayToString(modelname.getActionListeners()));

		String	error	= null;
		if(adf!=null)
		{
//			ClassLoader	oldcl	= Thread.currentThread().getContextClassLoader();
//			if(starter.getModelExplorer().getClassLoader()!=null)
//				Thread.currentThread().setContextClassLoader(starter.getModelExplorer().getClassLoader());

			try
			{
				if(SComponentFactory.isLoadable(starter.getJCC().getServiceContainer(), adf))
				{
					model = SComponentFactory.loadModel(starter.getJCC().getServiceContainer(), adf);
					updateGuiForNewModel(adf);
					
					if(SComponentFactory.isStartable(starter.getJCC().getServiceContainer(), adf))
					{
						createArguments();
						createResults();
						arguments.setVisible(true);
						results.setVisible(true);
						componentpanel.setVisible(true);
						start.setVisible(true);
						
						filenamel.setMinimumSize(confdummy.getMinimumSize());
						filenamel.setPreferredSize(confdummy.getPreferredSize());
						confl.setMinimumSize(confdummy.getMinimumSize());
						confl.setPreferredSize(confdummy.getPreferredSize());
						componentnamel.setMinimumSize(confdummy.getMinimumSize());
						componentnamel.setPreferredSize(confdummy.getPreferredSize());
					}
					else
					{
						arguments.setVisible(false);
						results.setVisible(false);
						componentpanel.setVisible(false);
						start.setVisible(false);
						
						filenamel.setMinimumSize(confdummy.getMinimumSize());
						filenamel.setPreferredSize(confdummy.getPreferredSize());
						confl.setMinimumSize(confdummy.getMinimumSize());
						confl.setPreferredSize(confdummy.getPreferredSize());
					}
				}
				else
				{
					model = null;
				}
				lastfile = adf;
			}
			catch(Exception e)
			{
				//e.printStackTrace();
				model = null;
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				error	= sw.toString();
			}
			
//			Thread.currentThread().setContextClassLoader(oldcl);
		}
		else
		{
			model	= null;
		}

		if(model==null)
		{
			start.setEnabled(false);
			config.removeAllItems();
//			clearArguments();
//			clearResults();
			setComponentName("");
			clearApplicationName();
			filename.setText("");
			if(error!=null)
				modeldesc.addTextContent("Error", null, "No model loaded:\n"+error, "error");
			else
				modeldesc.addTextContent("Model", null, "No model loaded.", "model");
		}
	}

	/**
	 *  Update the GUI for a new model.
	 *  @param adf The adf.
	 */
	void updateGuiForNewModel(final String adf)
	{
		if(model==null)
			return;
		
//		ClassLoader	oldcl	= Thread.currentThread().getContextClassLoader();
//		if(starter.getModelExplorer().getClassLoader()!=null)
//			Thread.currentThread().setContextClassLoader(starter.getModelExplorer().getClassLoader());
		
//		System.out.println("updategui "+model);
		
		filename.setText(adf);

//		if(model.getName()!=null && SXML.isComponentFilename(adf))
		/*if(model.getName()!=null && model instanceof ApplicationModel)
		{
			appname.setModel(new DefaultComboBoxModel(new String[]{model.getName()}));
			appname.setEditable(true);
		}
		else*/ if(model.isStartable())
		{
//			appname.setModel(appmodel);
			componentname.setText(model.getName());
//			appname.removeAllItems();
//			appname.addItem("");
//			appname.setSelectedItem("");
//			IContextService cs = (IContextService)starter.getJCC().getComponent().getPlatform().getService(IContextService.class);
//			if(cs!=null)
//			{
//				IContext[] contexts =  cs.getContexts(IApplicationContext.class);
//				for(int i=0; contexts!=null && i<contexts.length; i++)
//				{
//					appname.addItem(contexts[i].getName());
//				}
//			}
//			appname.setEditable(false);
		}
		else
		{
			componentname.setText("");
//			appname.setEditable(true);
		}
		
		lastfile = model.getFilename();

		ItemListener[] lis = config.getItemListeners();
		for(int i=0; i<lis.length; i++)
			config.removeItemListener(lis[i]);
		config.removeAllItems();
		
		// Add all known component configuration names to the config chooser.
		
		String[] confignames = model.getConfigurations();
		for(int i = 0; i<confignames.length; i++)
		{
			((DefaultComboBoxModel)config.getModel()).addElement(confignames[i]);
		}
		if(confignames.length>0)
			config.getModel().setSelectedItem(confignames[0]);
		
		/*IMConfiguration[] states = model.getConfigurationbase().getConfigurations();
		for(int i = 0; i<states.length; i++)
		{
			((DefaultComboBoxModel)config.getModel()).addElement(states[i].getName());
		}
		IMConfiguration defstate = model.getConfigurationbase().getDefaultConfiguration();
		if(defstate!=null)
		{
			config.getModel().setSelectedItem(defstate.getName());
		}*/

		//		if(modeldesc.getSelectedComponent()==null
		//			|| !modeldesc.getId(modeldesc.getSelectedComponent()).equals(adf))
		{
			String clazz = SReflect.getInnerClassName(model.getClass());
			if(clazz.endsWith("Data")) clazz = clazz.substring(0, clazz.length()-4);

			final IReport report = model.getReport();
			if(report!=null && !report.isEmpty())
			{
				final Icon icon = GuiProperties.getElementIcon(clazz+"_broken");
				try
				{
					modeldesc.addHTMLContent(model.getName(), icon, report.toHTMLString(), adf, report.getDocuments());
				}
				catch(final Exception e)
				{
					//e.printStackTrace();
//					SwingUtilities.invokeLater(new Runnable()
//					{
//						public void run()
//						{
							String text = SUtil.wrapText("Could not display HTML content: "+e.getMessage());
							JOptionPane.showMessageDialog(SGUI.getWindowParent(StarterPanel.this), text, "Display Problem", JOptionPane.INFORMATION_MESSAGE);
							modeldesc.addTextContent(model.getName(), icon, report.toString(), adf);
//						}
//					});
				}
			}
			else
			{
				final Icon icon = GuiProperties.getElementIcon(clazz);
				try
				{
					modeldesc.addHTMLContent(model.getName(), icon, model.getDescription(), adf, null);
				}
				catch(final Exception e)
				{
//					SwingUtilities.invokeLater(new Runnable()
//					{
//						public void run()
//						{
							String text = SUtil.wrapText("Could not display HTML content: "+e.getMessage());
							JOptionPane.showMessageDialog(SGUI.getWindowParent(StarterPanel.this), text, "Display Problem", JOptionPane.INFORMATION_MESSAGE);
							modeldesc.addTextContent(model.getName(), icon, model.getDescription(), adf);
//						}
//					});
				}
			}

			// Adjust state of start button depending on model checking state.
//			start.setEnabled(SXML.isComponentFilename(adf) && (report==null || report.isEmpty()));
			start.setEnabled(model.isStartable() && (report==null || report.isEmpty()));
		
			for(int i=0; i<lis.length; i++)
				config.addItemListener(lis[i]);
		}
//		Thread.currentThread().setContextClassLoader(oldcl);
	}

	/**
	 *  Get the properties.
	 *  @param props The properties.
	 */
	public Properties	getProperties()
	{
		Properties	props	= new Properties();
		
		String m = SUtil.convertPathToRelative(filename.getText());
		if(m!=null) props.addProperty(new Property("model", m));

		String c = (String)config.getSelectedItem();
		if(c!=null) props.addProperty(new Property("config", c));

		props.addProperty(new Property("startsuspended", ""+suspend.isSelected()));

		props.addProperty(new Property("autogenerate", ""+genname.isSelected()));
		
		props.addProperty(new Property("name", componentname.getText()));
		for(int i=0; argelems!=null && i<argelems.size(); i++)
		{
			JTextField valt = (JTextField)arguments.getComponent(i*4+3);
			props.addProperty(new Property("argument", valt.getText()));
		}
		
		return props;
	}

	/**
	 *  Set the properties.
	 *  @param props The propoerties.
	 */
	protected void setProperties(Properties props)
	{
		// Settings are invoke later'd due to getting overridden otherwise.!?
		
		String mo = props.getStringProperty("model");
		if(mo!=null)
		{
			loadModel(mo);
			selectConfiguration(props.getStringProperty("config"));
		}
		setStartSuspended(props.getBooleanProperty("startsuspended"));

		Property[]	aargs	= props.getProperties("argument");
		String[] argvals = new String[aargs.length];
		for(int i=0; i<aargs.length; i++)
		{
			argvals[i] = aargs[i].getValue();
		}
		setArguments(argvals);

		setComponentName(props.getStringProperty("name"));
		setAutoGenerate(props.getBooleanProperty("autogenerate"));
		
	}

	/**
	 *  Reset the gui.
	 */
	public void reset()
	{
		filename.setText("");
		modeldesc.removeAll();
		loadModel(null);
		config.removeAllItems();
		clearArguments();
		clearResults();
		setComponentName("");
		//model = null;
		//start.setEnabled(false);
	}

	/**
	 *  Select a configuration.
	 *  @param conf The configuration.
	 */
	protected void selectConfiguration(final String conf)
	{
		if(conf!=null)
		{
			//System.out.println("selecting: "+conf+" "+config.getModel().getSize());
			config.getModel().setSelectedItem(conf);
		}
	}

	/**
	 *  Set the arguments.
	 *  @param args The arguments.
	 */
	protected void setArguments(final String[] args)
	{
		if(args!=null && args.length>0)
		{
			if(arguments==null || argelems==null || arguments.getComponentCount()!=4*argelems.size())
				return;
			
			for(int i=0; i<args.length; i++)
			{
				JTextField valt = (JTextField)arguments.getComponent(i*4+3);
				valt.setText(args[i]);
			}
		}
	}
	
	/**
	 *  Refresh the argument values.
	 *  Called only from gui thread.
	 */
	protected void refreshArguments()
	{
		// Assert that all argument components are there.
		if(model==null || arguments==null || argelems==null)
			return;
		
		for(int i=0; argelems!=null && i<argelems.size(); i++)
		{
			JTextField valt = (JTextField)arguments.getComponent(i*4+2);
			valt.setText(""+((IArgument)argelems.get(i)).getDefaultValue((String)config.getSelectedItem()));
		}
	}
	
	/**
	 *  Refresh the argument values.
	 */
	protected void clearArguments()
	{
		// Assert that all argument components are there.
		if(arguments==null || argelems==null)
			return;
		
		for(int i=0; i<argelems.size(); i++)
		{
			JTextField valt = (JTextField)arguments.getComponent(i*4+3);
			valt.setText("");
		}
	}
	
	/**
	 *  Create the arguments panel.
	 */
	protected void createArguments()
	{
		argelems = SCollection.createArrayList();
		arguments.removeAll();
		arguments.setBorder(null);
		
		IArgument[] args = model.getArguments();
		
		for(int i=0; i<args.length; i++)
		{
			argelems.add(args[i]);
			createArgumentGui(args[i], i);
		}
		
		if(args.length>0)
			arguments.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), " Arguments "));
	}
	
	/**
	 *  Refresh the result values.
	 */
	protected void refreshResults()
	{
		// Assert that all argument components are there.
		if(model==null || results==null || reselems==null)
			return;
		
		// Find results of specific instance.
		Map mres = null;
		int sel = selectavail.getSelectedIndex();
//		System.out.println("Selected index: "+sel+selectavail.getSelectedItem().hashCode());
		if(sel>0)
		{
			List rs = (List)resultsets.get(model.getPackage()+"."+model.getName());
			Object[] r = (Object[])rs.get(sel-1);
			mres = (Map)r[1];
		}
		
		for(int i=0; reselems!=null && i<reselems.size(); i++)
		{
			IArgument arg = ((IArgument)reselems.get(i));
//			Object value = mres!=null? mres.get(arg.getName()): arg.getDefaultValue((String)config.getSelectedItem());
			Object value = mres!=null? mres.get(arg.getName()): "";
			JTextField valt = (JTextField)results.getComponent(i*4+3);
			valt.setText(""+value);
		}
	}
	
	/**
	 *  Clear the result values.
	 */
	protected void clearResults()
	{
		// Assert that all argument components are there.
		if(results==null || reselems==null)
			return;
		
		for(int i=0; i<reselems.size(); i++)
		{
			JTextField valt = (JTextField)results.getComponent(i*4+3);
			valt.setText("");
		}
	}
	
	/**
	 *  Create the results panel.
	 */
	protected void createResults()
	{
		reselems = SCollection.createArrayList();
		results.removeAll();
		results.setBorder(null);
		
		final IArgument[] res = model.getResults();
		
		for(int i=0; i<res.length; i++)
		{
			reselems.add(res[i]);
			createResultGui(res[i], i);
		}
		
		if(res.length>0)
		{
			results.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), " Results "));
			
			JLabel sr = new JLabel("Store results");
			storeresults = new JCheckBox();
			
			JButton cr = new JButton("Clear results");
			
			JLabel sa = new JLabel("Select component instance");
			selectavail= new FixedJComboBox();
			
			selectavail.addItem("- no instance selected -");
			
			cr.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					storeresults.removeAll();
					selectavail.removeAllItems();
					selectavail.addItem("- no instance selected -");
					clearResults();
				}
			});
			
			List rs = (List)resultsets.get(model.getPackage()+"."+model.getName());
			if(rs!=null)
			{
				for(int i=0; i<rs.size(); i++)
				{
					Object[] r = (Object[])rs.get(i);
					selectavail.addItem(r[0]);
				}
				selectavail.setSelectedIndex(0);
			}
			
//					selectavail.addItemListener(new ItemListener()
//					{
//						public void itemStateChanged(ItemEvent e)
//						{
//							System.out.println("here: "+resultsets);
//							refreshResults();
//						}
//					});
			selectavail.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					refreshResults();
				}
			});
			
			int y = res.length;
			
			results.add(sr, new GridBagConstraints(0, y, 1, 1, 0, 0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 0), 0, 0));
			results.add(storeresults, new GridBagConstraints(1, y, 2, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 0, 2, 2), 0, 0));
			results.add(cr, new GridBagConstraints(3, y, 1, 1, 0, 0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
			y++;
			results.add(sa, new GridBagConstraints(0, y, 1, 1, 0, 0, GridBagConstraints.EAST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
			results.add(selectavail, new GridBagConstraints(1, y, 3, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		}
	}
	
	/**
	 *  Create the gui for one argument. 
	 *  @param arg The belief or belief reference.
	 *  @param y The row number where to add.
	 */
	protected void createArgumentGui(final IArgument arg, int y)
	{
		JLabel namel = new JLabel(arg.getName());
		final JValidatorTextField valt = new JValidatorTextField(15);
		
		// todo:
		ILibraryService ls = (ILibraryService)StarterPanel.this.starter.getJCC().getServiceContainer().getService(ILibraryService.class);
		valt.setValidator(new ParserValidator(ls.getClassLoader()));
		
		String configname = (String)config.getSelectedItem();
		JTextField mvalt = new JTextField(""+arg.getDefaultValue(configname));
		// Java JTextField bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4247013
		//mvalt.setMinimumSize(new Dimension(mvalt.getPreferredSize().width/4, mvalt.getPreferredSize().height/4));
		mvalt.setEditable(false);
		
		JLabel typel = new JLabel(arg.getTypename()!=null? arg.getTypename(): "undefined");
		
		String description = arg.getDescription();
		if(description!=null)
		{
			namel.setToolTipText(description);
			valt.setToolTipText(description);
			mvalt.setToolTipText(description);
//			typel.setToolTipText(description);
		}
		
		int x = 0;
		arguments.add(typel, new GridBagConstraints(x++, y, 1, 1, 0, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		arguments.add(namel, new GridBagConstraints(x++, y, 1, 1, 0, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		arguments.add(mvalt, new GridBagConstraints(x++, y, 1, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		arguments.add(valt, new GridBagConstraints(x++, y, 1, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		y++;
	}
	
	/**
	 *  Create the gui for one argument. 
	 *  @param arg The belief or belief reference.
	 *  @param y The row number where to add.
	 */
	protected void createResultGui(final IArgument arg, int y)
	{
		JLabel namel = new JLabel(arg.getName());
		final JTextField valt = new JTextField();
		valt.setEditable(false);
		
		String configname = (String)config.getSelectedItem();
		JTextField mvalt = new JTextField(""+arg.getDefaultValue(configname));
		// Java JTextField bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4247013
		//mvalt.setMinimumSize(new Dimension(mvalt.getPreferredSize().width/4, mvalt.getPreferredSize().height/4));
		mvalt.setEditable(false);
		
		JLabel typel = new JLabel(arg.getTypename()!=null? arg.getTypename(): "undefined");
		
		String description = arg.getDescription();
		if(description!=null)
		{
			namel.setToolTipText(description);
			valt.setToolTipText(description);
			mvalt.setToolTipText(description);
//			typel.setToolTipText(description);
		}
		
		int x = 0;
		results.add(typel, new GridBagConstraints(x++, y, 1, 1, 0, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		results.add(namel, new GridBagConstraints(x++, y, 1, 1, 0, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		results.add(mvalt, new GridBagConstraints(x++, y, 1, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		results.add(valt, new GridBagConstraints(x++, y, 1, 1, 1, 0, GridBagConstraints.WEST,
			GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		y++;
	}
	
	/**
	 *  Set the component name.
	 *  @param name The name.
	 */
	protected void setComponentName(final String name)
	{
		if(name!=null)
		{
			componentname.setText(name);
		}
	}
	
	/**
	 *  Clear the application name.
	 *  @param name The name.
	 */
	protected void clearApplicationName()
	{
//		appname.removeAll();
	}

	/**
	 *  Set the auto generate in gui.
	 *  @param autogen The autogen property.
	 */
	protected void setAutoGenerate(final boolean autogen)
	{
		genname.setSelected(autogen);
		componentname.setEditable(!autogen);
		numcomponents.setEnabled(autogen);
	}

	/**
	 *  Set the start suspended flag in gui.
	 *  @param startsuspended The start suspended flag property.
	 */
	protected void setStartSuspended(final boolean startsuspended)
	{
		suspend.setSelected(startsuspended);
	}
	
	/**
	 *  Get the last loaded filename.
	 *  @return The filename.
	 */
	public String getFilename()
	{
		return lastfile;
	}

	/**
	 *  Main for testing only.
	 *  @param args The arguments.
	 */
	public static void main(String[] args)
	{
		JFrame f = new JFrame();
		f.getContentPane().add(new StarterPanel(null));
		f.pack();
		f.setVisible(true);
	}

	/**
	 *  Set the current parent.
	 *  @param parent	The component id.
	 */
	public void setParent(IComponentIdentifier parent)
	{
		this.parent	= parent;
		parenttf.setText(parent!=null ? parent.getName() : "");
	}
}


