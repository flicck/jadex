package jadex.tools.starter;

import jadex.base.SComponentFactory;
import jadex.base.gui.componenttree.ComponentTreePanel;
import jadex.base.gui.plugin.IControlCenter;
import jadex.bridge.ICMSComponentListener;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IModelInfo;
import jadex.commons.SGUI;
import jadex.commons.ThreadSuspendable;
import jadex.commons.concurrent.DefaultResultListener;
import jadex.commons.concurrent.SwingDefaultResultListener;
import jadex.commons.gui.IMenuItemConstructor;
import jadex.commons.gui.PopupBuilder;
import jadex.tools.common.modeltree.FileNode;
import jadex.tools.common.modeltree.IExplorerTreeNode;
import jadex.tools.common.modeltree.ModelExplorer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 * The starter gui allows for starting components platform independently.
 */
public class StarterServicePanel extends JPanel implements ICMSComponentListener
{
	//-------- static part --------

	/**
	 * The image icons.
	 */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"add_remote_component", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/add_remote_component.png"),
		"kill_platform", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_killplatform.png"),
		"starter", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_starter.png"),
		"starter_sel", SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_starter_sel.png"),
		"start_component",	SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/start.png"),
		"checking_menu",	SGUI.makeIcon(StarterPlugin.class, "/jadex/tools/common/images/new_agent_broken.png")
	});

	//-------- attributes --------

	/** The starter panel. */
	protected StarterPanel spanel;

	/** The panel showing the classpath models. */
	protected ModelExplorer mpanel;

	/** The component instances in a tree. */
	protected ComponentTreePanel comptree;
	
	/** A split panel. */
	protected JSplitPane lsplit;

	/** A split panel. */
    protected JSplitPane csplit;
	
    /** The jcc. */
    protected IControlCenter jcc;
    
	//-------- constructors --------

	/**
	 * Open the GUI.
	 * @param starter The starter.
	 */
	public StarterServicePanel(final IControlCenter jcc)
	{
		super(new BorderLayout());
		this.jcc	= jcc;
		
		System.out.println("jcc: "+jcc);
		
		csplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		csplit.setOneTouchExpandable(true);

		lsplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		lsplit.setOneTouchExpandable(true);
		lsplit.setResizeWeight(0.7);

		mpanel = new ModelExplorer(jcc.getExternalAccess().getServiceProvider(), new StarterNodeFunctionality(jcc));
//		mpanel.setAction(FileNode.class, new INodeAction()
//		{
//			public void validStateChanged(TreeNode node, boolean valid)
//			{
//				String file1 = ((FileNode)node).getFile().getAbsolutePath();
//				String file2 = spanel.getFilename();
//				//System.out.println(file1+" "+file2);
//				if(file1!=null && file1.equals(file2))
//				{
//					spanel.reloadModel(file1);
//				}
//			}
//		});
		mpanel.setPopupBuilder(new PopupBuilder(new Object[]{new StartComponentMenuItemConstructor(), mpanel.ADD_PATH,
			mpanel.REMOVE_PATH, mpanel.REFRESH}));
		mpanel.addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(TreeSelectionEvent e)
			{
				Object	node = mpanel.getLastSelectedPathComponent();
				if(node instanceof FileNode)
				{
					// Models have to be loaded with absolute path.
					// An example to facilitate understanding:
					// root
					//  +-classes1
					//  |  +- MyComponent.component.xml
					//  +-classes2
					//  |  +- MyComponent.component.xml

					final String model = ((FileNode)node).getRelativePath();
//					if(getJCC().getComponent().getPlatform().getComponentFactory().isLoadable(model))
					SComponentFactory.isLoadable(jcc.getExternalAccess().getServiceProvider(), model).addResultListener(new SwingDefaultResultListener(spanel)
					{
						public void customResultAvailable(Object result)
						{
							if(((Boolean)result).booleanValue())
								loadModel(model);
						}
					});
//					else if(getJCC().getComponent().getPlatform().getApplicationFactory().isLoadable(model))
//					{
//						loadModel(model);
//					}
				}
			}
		});
		MouseListener ml = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				int row = mpanel.getRowForLocation(e.getX(), e.getY());
				if(row != -1)
				{
					if(e.getClickCount() == 2)
					{
						Object	node = mpanel.getLastSelectedPathComponent();
						if(node instanceof FileNode)
						{
							mpanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							final String type = ((FileNode)node).getFile().getAbsolutePath();
//							if(getJCC().getComponent().getPlatform().getComponentFactory().isStartable(type))
							// todo: resultcollect = false?
							SComponentFactory.isStartable(jcc.getExternalAccess().getServiceProvider(), type).addResultListener(new SwingDefaultResultListener(spanel)
							{
								public void customResultAvailable(Object result)
								{
									if(((Boolean)result).booleanValue())
										StarterPanel.createComponent(jcc, type, null, null, null, false, null, null, null, null, null, StarterServicePanel.this);
									mpanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
								}
							});
						}
					}
				}
      		}
  		};
  		mpanel.addMouseListener(ml);

		comptree = new ComponentTreePanel(jcc.getExternalAccess(), JSplitPane.HORIZONTAL_SPLIT);
		comptree.setMinimumSize(new Dimension(0, 0));
		
		lsplit.add(new JScrollPane(mpanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
//		lsplit.add(tp);
		lsplit.add(comptree);
		lsplit.setDividerLocation(300);

		csplit.add(lsplit);
		spanel = new StarterPanel(jcc);
		csplit.add(spanel);
		csplit.setDividerLocation(180);
            			
//		jcc.addComponentListListener(this);
		
		// todo: ?! is this ok?
		
		jcc.getExternalAccess().scheduleStep(new IComponentStep()
		{
			public static final String XML_CLASSNAME = "add-component-listener";
			public Object execute(IInternalAccess ia)
			{
				ia.getRequiredService("cms").addResultListener(new DefaultResultListener()
				{
					public void resultAvailable(Object result)
					{
						IComponentManagementService ces = (IComponentManagementService)result;
						ces.getComponentDescriptions().addResultListener(new DefaultResultListener()
						{
							public void resultAvailable(Object result)
							{
								IComponentDescription[] res = (IComponentDescription[])result;
								for(int i=0; i<res.length; i++)
									componentAdded(res[i]);
							}
						});
						ces.addComponentListener(null, StarterServicePanel.this);
					}
				});
				return null;
			}
		});
		
		this.add(csplit, BorderLayout.CENTER);
	}
	
	/**
	 *  Called when an component has died.
	 *  @param ad The component description.
	 */
	public void componentRemoved(final IComponentDescription ad, Map results)
	{
		// Update components on awt thread.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if(ad.getName().equals(spanel.parent))
					spanel.setParent(null);
			}
		});
	}

	/**
	 *  Called when an component is born.
	 *  @param ad the component description.
	 */
	public void componentAdded(final IComponentDescription ad)
	{
	}
	
	/**
	 *  Called when an component changed.
	 *  @param ad the component description.
	 */
	public void componentChanged(final IComponentDescription ad)
	{
	}
	
	/**
	 *  Load a model.
	 *  @param model The model name.
	 */
	protected void loadModel(final String model)
	{
		csplit.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		spanel.loadModel(model);
		csplit.setCursor(Cursor.getDefaultCursor());
	}
	
	/**
	 *  Dynamically create a new menu item structure for starting components.
	 */
	class StartComponentMenuItemConstructor implements IMenuItemConstructor
	{
		/**
		 *  Get or create a new menu item (struture).
		 *  @return The menu item (structure).
		 */
		public JMenuItem getMenuItem()
		{
			JMenuItem ret = null;

//			if(isEnabled())
//			{
//				IExplorerTreeNode node = (IExplorerTreeNode)mpanel.getLastSelectedPathComponent();
//				if(node instanceof FileNode)
//				{
//					final String type = ((FileNode)node).getFile().getAbsolutePath();
//					
//					if(((Boolean)SComponentFactory.isStartable(jcc.getExternalAccess().getServiceProvider(), type).get(new ThreadSuspendable())).booleanValue())//&& ((FileNode)node).isValid())
//					{
//						try
//						{
////							IComponentFactory componentfactory = getJCC().getComponent().getPlatform().getComponentFactory();
//							IModelInfo model = (IModelInfo)SComponentFactory.loadModel(jcc.getExternalAccess().getServiceProvider(), type).get(new ThreadSuspendable());
//							String[] inistates = model.getConfigurations();
////							IMBDIComponent model = SXML.loadComponentModel(type, null);
////							final IMConfiguration[] inistates = model.getConfigurationbase().getConfigurations();
//							
//							if(inistates.length>1)
//							{
//								JMenu re = new JMenu("Start Component");
//								re.setIcon(icons.getIcon("start_component"));
//								for(int i=0; i<inistates.length; i++)
//								{
//									final String config = inistates[i];
//									JMenuItem me = new JMenuItem(config);
//									re.add(me);
//									me.addActionListener(new ActionListener()
//									{
//										public void actionPerformed(ActionEvent e)
//										{
//											// todo: collectresults = false?
//											StarterPanel.createComponent(jcc, type, null, config, null, false, null, null, null, null, null, spanel);
//										}
//									});
//									me.setToolTipText("Start in configuration: "+config);
//
//								}
//								ret = re;
//								ret.setToolTipText("Start component in selectable configuration");
//							}
//							else
//							{
//								if(inistates.length==1)
//								{
//									ret = new JMenuItem("Start Component ("+inistates[0]+")");
//									ret.setToolTipText("Start component in configuration:"+inistates[0]);
//								}
//								else
//								{
//									ret = new JMenuItem("Start Component");
//									ret.setToolTipText("Start component without explicit initial state");
//								}
//								ret.setIcon(icons.getIcon("start_component"));
//								ret.addActionListener(new ActionListener()
//								{
//									public void actionPerformed(ActionEvent e)
//									{
//										// todo: collectresults = false?
//										StarterPanel.createComponent(jcc, type, null, null, null, false, null, null, null, null, null, spanel);
//									}
//								});
//							}
//						}
//						catch(Exception e)
//						{
//							// NOP
//						}
//					}
//				}
//			}

			return ret;
		}

		/**
		 *  Test if action is available in current context.
		 *  @return True, if available.
		 */
		public boolean isEnabled()
		{
			boolean ret = false;
			IExplorerTreeNode node = (IExplorerTreeNode)mpanel.getLastSelectedPathComponent();
			if(node instanceof FileNode)
			{
				String type = ((FileNode)node).getFile().getAbsolutePath();
				if(((Boolean)SComponentFactory.isStartable(jcc.getExternalAccess().getServiceProvider(), type).get(new ThreadSuspendable())))
					ret = true;
			}
			return ret;
		}
	}
}