package jadex.extension.envsupport;

import jadex.bridge.modelinfo.IModelInfo;
import jadex.commons.IPropertyObject;
import jadex.commons.IValueFetcher;
import jadex.commons.SReflect;
import jadex.commons.Tuple;
import jadex.commons.collection.MultiCollection;
import jadex.commons.gui.SGUI;
import jadex.component.ComponentXMLReader;
import jadex.extension.envsupport.dataview.IDataView;
import jadex.extension.envsupport.environment.AvatarMapping;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.math.IVector2;
import jadex.extension.envsupport.math.Vector2Double;
import jadex.extension.envsupport.math.Vector3Double;
import jadex.extension.envsupport.observer.graphics.drawable.DrawableCombiner;
import jadex.extension.envsupport.observer.graphics.drawable.Primitive;
import jadex.extension.envsupport.observer.graphics.drawable.RegularPolygon;
import jadex.extension.envsupport.observer.graphics.drawable.Text;
import jadex.extension.envsupport.observer.graphics.drawable.TexturedRectangle;
import jadex.extension.envsupport.observer.graphics.layer.GridLayer;
import jadex.extension.envsupport.observer.graphics.layer.Layer;
import jadex.extension.envsupport.observer.graphics.layer.TiledLayer;
import jadex.extension.envsupport.observer.perspective.IPerspective;
import jadex.extension.envsupport.observer.perspective.Perspective2D;
import jadex.javaparser.IExpressionParser;
import jadex.javaparser.IParsedExpression;
import jadex.javaparser.SimpleValueFetcher;
import jadex.javaparser.javaccimpl.JavaCCExpressionParser;
import jadex.xml.AccessInfo;
import jadex.xml.AttributeConverter;
import jadex.xml.AttributeInfo;
import jadex.xml.BasicTypeConverter;
import jadex.xml.IAttributeConverter;
import jadex.xml.IContext;
import jadex.xml.IObjectObjectConverter;
import jadex.xml.IPostProcessor;
import jadex.xml.IStringObjectConverter;
import jadex.xml.ISubObjectConverter;
import jadex.xml.MappingInfo;
import jadex.xml.ObjectInfo;
import jadex.xml.StackElement;
import jadex.xml.SubObjectConverter;
import jadex.xml.SubobjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.XMLInfo;
import jadex.xml.bean.BeanAccessInfo;
import jadex.xml.reader.ReadContext;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 *  Java representation of environment space type for xml description.
 */
public class MEnvSpaceType
{
	//-------- constants --------
	
	/** The border object placement. */
	public static final String	OBJECTPLACEMENT_BORDER	= "border";
	
	/** The center object placement. */
	public static final String	OBJECTPLACEMENT_CENTER	= "center";
	
	//-------- attributes --------
	
	/** The name. */
	protected String name;

	/** The class name. */
	protected String classname;
	
	/** The properties. */
	protected Map properties;
	
	//-------- methods --------
	
	/**
	 *  Get the name.
	 *  @return The name.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 *  Set the name.
	 *  @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 *  Get the classname.
	 *  @return the classname.
	 */
	public String getClassName()
	{
		return classname;
	}

	/**
	 *  Set the classname.
	 *  @param classname The classname to set.
	 */
	public void setClassName(String classname)
	{
		this.classname = classname;
	}
	
	/**
	 *  Add a property.
	 *  @param key The key.
	 *  @param value The value.
	 */
	public void addProperty(String key, Object value)
	{
		if(properties==null)
			properties = new MultiCollection();
		properties.put(key, value);
	}
	
	/**
	 *  Get a property.
	 *  @param key The key.
	 *  @return The value.
	 */
	public List getPropertyList(String key)
	{
		return properties!=null? (List)properties.get(key):  null;
	}
	
	/**
	 *  Get the properties.
	 *  @return The properties.
	 */
	public Map getProperties()
	{
		return properties;
	}
	
	/**
	 *  Get a property from a (multi)map.
	 *  @param map The map.
	 *  @param name The name.
	 *  @return The property.
	 */
	public Object getProperty(String name)
	{
		Object ret = null;
		if(properties!=null)
		{
			Object tmp = properties.get(name);
			ret = (tmp instanceof List)? ((List)tmp).get(0): tmp; 
		}
		return ret;
	}

//	/**
//	 *  Get a string representation of this AGR space type.
//	 *  @return A string representation of this AGR space type.
//	 */
//	public String	toString()
//	{
//		StringBuffer	sbuf	= new StringBuffer();
//		sbuf.append(SReflect.getInnerClassName(getClass()));
//		sbuf.append("(name=");
//		sbuf.append(getName());
//		sbuf.append(", dimensions=");
//		sbuf.append(getDimensions());
//		sbuf.append(", component action types=");
//		sbuf.append(getMEnvComponentActionTypes());
//		sbuf.append(", space action types=");
//		sbuf.append(getMEnvSpaceActionTypes());
//		sbuf.append(", class=");
//		sbuf.append(getClazz());
//		sbuf.append(")");
//		return sbuf.toString();
//	}
	
	//-------- static part --------
	
	protected static final Set	TYPES;
	
	static
	{
		Set types = new HashSet();
		
		IStringObjectConverter typeconv = new ClassConverter();
		IStringObjectConverter colorconv = new ColorConverter();
		IStringObjectConverter tcolorconv = new TolerantColorConverter();
		final IStringObjectConverter expconv = new ExpressionConverter();
//		ITypeConverter tdoubleconv = new TolerantDoubleTypeConverter();
		IStringObjectConverter tintconv = new TolerantIntegerTypeConverter();
		IObjectObjectConverter nameconv = new NameAttributeToObjectConverter();
		
		IAttributeConverter attypeconv = new AttributeConverter(typeconv, null);
		IAttributeConverter atexconv = new AttributeConverter(expconv, null);
		IAttributeConverter atcolconv = new AttributeConverter(colorconv, null);
		IAttributeConverter attcolconv = new AttributeConverter(tcolorconv, null);
		IAttributeConverter attintconv = new AttributeConverter(tintconv, null);
		ISubObjectConverter sunameconv = new SubObjectConverter(nameconv, null);
		ISubObjectConverter suexconv = new SubObjectConverter(new IObjectObjectConverter() 
		{
			public Object convertObject(Object val, IContext context) throws Exception
			{
				return expconv.convertString((String)val, context);
			}
		}, null);
		
		String uri =  "http://jadex.sourceforge.net/jadex-envspace"; 
		
		TypeInfo ti_po = new TypeInfo(new XMLInfo("abstract_propertyobject"), new ObjectInfo(MultiCollection.class), 
			new MappingInfo(null, new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "property"), "properties", null, null, new BeanAccessInfo(AccessInfo.THIS)))
			}));		
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "objecttype")}), new ObjectInfo(MObjectType.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("kdtree", "kdTree"))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "envspacetype")}), new ObjectInfo(MEnvSpaceType.class),
			new MappingInfo(null, new AttributeInfo[]{
//			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo("property")), attypeconv),
			new AttributeInfo(new AccessInfo("class", "className")),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo("property")), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo("property")), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("depth", null, null, null, new BeanAccessInfo("property")), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("border", null, null, null, new BeanAccessInfo("property"))),
			new AttributeInfo(new AccessInfo("neighborhood", null, null, null, new BeanAccessInfo("property"))),
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "property"), "properties", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "objecttype"), "objecttypes", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "avatarmapping"), "avatarmappings", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "actiontype"), "actiontypes", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "processtype"), "processtypes", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "tasktype"), "tasktypes", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "perceptgenerator"), "perceptgenerators", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "perceptprocessor"), "perceptprocessors", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "dataview"), "dataviews", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "spaceexecutor"), null, null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "perspective"), "perspectives", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "percepttype"), "percepttypes", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "dataprovider"), "dataproviders", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "dataconsumer"), "dataconsumers", null, null, new BeanAccessInfo("property")))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "avatarmapping")}), new ObjectInfo(AvatarMapping.class), 
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("componenttype", "componentType")),
			new AttributeInfo(new AccessInfo("objecttype", "objectType")),
			new AttributeInfo(new AccessInfo("createavatar", "createAvatar")),
			new AttributeInfo(new AccessInfo("createcomponent", "createComponent")),
			new AttributeInfo(new AccessInfo("killavatar", "killAvatar")),
			new AttributeInfo(new AccessInfo("killcomponent", "killComponent"))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "name"), "componentName"))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "name")}), null, 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo((String)null, AccessInfo.THIS), atexconv))));

/*
		types.add(new TypeInfo(null, new QName[]{new QName(uri, "createcomponent")}, MultiCollection.class, null, null,
			null, null, null,
			new SubobjectInfo[]{
			new SubobjectInfo(new BeanAttributeInfo(new QName(uri, "name"), "name", null, null, null, "")),
			new SubobjectInfo(new BeanAttributeInfo(new QName(uri, "argument"), "arguments", null, null, null, ""))
			}));

		types.add(new TypeInfo(null, new QName[]{new QName(uri, "name")}, IParsedExpression.class, null, null,
			null, null, null,
			new SubobjectInfo[]{
			new SubobjectInfo(new BeanAttributeInfo(new QName(uri, "name"), "name", null, null, null, "")),
			new SubobjectInfo(new BeanAttributeInfo(new QName(uri, "argument"), "arguments", null, null, null, ""))
			}));
*/
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "percepttype")}), new ObjectInfo(MultiCollection.class), 
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("objecttype", "objecttypes", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("componenttype", "componenttypes", null, null, new BeanAccessInfo(AccessInfo.THIS)))
			}, 
			new SubobjectInfo[]{
			// fetches the name attribute of the objecttype/componenttype and return it as object
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "objecttypes"), new QName(uri, "objecttype")}), new AccessInfo(new QName(uri, "objecttype"), "objecttypes", null, null, new BeanAccessInfo(AccessInfo.THIS)), sunameconv),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "componenttypes"), new QName(uri, "componenttype")}), new AccessInfo(new QName(uri, "componenttype"), "componenttypes", null, null, new BeanAccessInfo(AccessInfo.THIS)), sunameconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "actiontype")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "parameter"), "parameters", null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "parameter")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "processtype")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "tasktype")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri,"perceptgenerator")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri,"perceptprocessor")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{	
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("componenttype", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "percepttype"), "percepttypes", null, null, new BeanAccessInfo(AccessInfo.THIS)), sunameconv),
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri,"perceptprocessor"), new QName(uri, "percepttype")}), new ObjectInfo(HashMap.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "dataview")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("objecttype", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					Map sourceview = (Map)args.get("sourceview");
					IEnvironmentSpace space = (IEnvironmentSpace)args.get("space");
					IDataView ret = (IDataView)((Class)getProperty(sourceview, "clazz")).newInstance();
					
					Map	props	= null;
					List lprops = (List)sourceview.get("properties");
					if(lprops!=null)
					{
						SimpleValueFetcher	fetcher	= new SimpleValueFetcher();
						fetcher.setValue("$space", space);
						fetcher.setValue("$object", args.get("object"));
						fetcher.setValue("$view", args.get(ret));
						props = convertProperties(lprops, fetcher);
					}
					
					ret.init(space, props);
					return ret;
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "perspective")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),		
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("opengl", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("invertxaxis", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("invertyaxis", null, null, Boolean.TRUE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("objectplacement", null, null, OBJECTPLACEMENT_BORDER, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.STRING_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("zoomlimit", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("bgcolor", null, null, null, new BeanAccessInfo(AccessInfo.THIS)),  new AttributeConverter(colorconv, null)),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IValueFetcher fetcher = (IValueFetcher)args.get("fetcher");
					args = (Map)args.get("object");
					
					IPerspective ret = (IPerspective)((Class)getProperty(args, "clazz")).newInstance();
					boolean opengl = getProperty(args, "opengl")!=null? 
						((Boolean)getProperty(args, "opengl")).booleanValue(): true;
					ret.setOpenGl(opengl);
//					String name = (String)getProperty(args, "name");
//					System.out.println("Perspective: "+name+" using opengl="+opengl);
					
					// Hack!!!
					if(ret instanceof Perspective2D)
					{
						Perspective2D pers = (Perspective2D)ret;
						Boolean invertx = (Boolean)getProperty(args, "invertxaxis");
						pers.setInvertYAxis(invertx.booleanValue());
						Boolean inverty = (Boolean)getProperty(args, "invertyaxis");
						pers.setInvertYAxis(inverty.booleanValue());
						
						pers.setBackground((Color)getProperty(args, "bgcolor"));
						
						String	placement	= (String)getProperty(args, "objectplacement");
						if(OBJECTPLACEMENT_CENTER.equals(placement))
							pers.setObjectShift(new Vector2Double(0.5));
						
						Double zoomlimit = (Double)getProperty(args, "zoomlimit");
						if(zoomlimit != null)
							pers.setZoomLimit(zoomlimit.doubleValue());
					}
					
					List drawables = (List)args.get("drawables");
					if(drawables!=null)
					{
						for(int k=0; k<drawables.size(); k++)
						{
							Map sourcedrawable = (Map)drawables.get(k);
							Map tmp = new HashMap();
							tmp.put("fetcher", fetcher);
							tmp.put("object", sourcedrawable);
							ret.addVisual(getProperty(sourcedrawable, "objecttype"), 
								((IObjectCreator)getProperty(sourcedrawable, "creator")).createObject(tmp));
						}
					}
					
					List prelayers = (List)args.get("prelayers");
					if(prelayers!=null)
					{
						List targetprelayers = new ArrayList();
						for(int k=0; k<prelayers.size(); k++)
						{
							Map layer = (Map)prelayers.get(k);
//							System.out.println("prelayer: "+layer);
							targetprelayers.add(((IObjectCreator)getProperty(layer, "creator")).createObject(layer));
						}
						((Perspective2D) ret).setPrelayers((Layer[]) targetprelayers.toArray(new Layer[0]));
					}
					
					List postlayers = (List)args.get("postlayers");
					if(postlayers!=null)
					{
						List targetpostlayers = new ArrayList();
						for(int k=0; k<postlayers.size(); k++)
						{
							Map layer = (Map)postlayers.get(k);
//							System.out.println("postlayer: "+layer);
							targetpostlayers.add(((IObjectCreator)getProperty(layer, "creator")).createObject(layer));
						}
						((Perspective2D) ret).setPostlayers((Layer[]) targetpostlayers.toArray(new Layer[0]));
					}
					
					return ret;
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawable"), "drawables", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "prelayers"), new QName(uri, "gridlayer")}), new AccessInfo(new QName(uri, "gridlayer"), "prelayers", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "prelayers"), new QName(uri, "tiledlayer")}), new AccessInfo(new QName(uri, "tiledlayer"), "prelayers", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "prelayers"), new QName(uri, "colorlayer")}), new AccessInfo(new QName(uri, "colorlayer"), "prelayers", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "postlayers"), new QName(uri, "gridlayer")}), new AccessInfo(new QName(uri, "gridlayer"), "postlayers", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "postlayers"), new QName(uri, "tiledlayer")}), new AccessInfo(new QName(uri, "tiledlayer"), "postlayers", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "postlayers"), new QName(uri, "colorlayer")}), new AccessInfo(new QName(uri, "colorlayer"), "postlayers", null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "drawable")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("objecttype", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("x", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("y", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatex", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatey", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatez", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("position", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("rotation", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("size", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IValueFetcher fetcher = (IValueFetcher)args.get("fetcher");
					args = (Map)args.get("object");
					Object position = getProperty(args, "position");
					if(position==null)
					{
						position = Vector2Double.getVector2((Double)getProperty(args, "x"),
							(Double)getProperty(args, "y"));
					}				
					Object rotation = getProperty(args, "rotation");
					if(rotation==null)
					{
						Double rx = (Double)getProperty(args, "rotatex");
						Double ry = (Double)getProperty(args, "rotatey");
						Double rz = (Double)getProperty(args, "rotatez");
						rotation = Vector3Double.getVector3(rx!=null? rx: new Double(0), ry!=null? ry: new Double(0), rz!=null? rz: new Double(0));
					}
					Object size = getProperty(args, "size");
					if(size==null)
					{
						size = Vector2Double.getVector2((Double)getProperty(args, "width"),
							(Double)getProperty(args, "height"));
					}
					DrawableCombiner ret = new DrawableCombiner(position, rotation, size);
					
					List parts = (List)args.get("parts");
					if(parts!=null)
					{
						for(int l=0; l<parts.size(); l++)
						{
							Map sourcepart = (Map)parts.get(l);
							int layer = getProperty(sourcepart, "layer")!=null? ((Integer)getProperty(sourcepart, "layer")).intValue(): 0;
							ret.addPrimitive((Primitive)((IObjectCreator)getProperty(sourcepart, "creator")).createObject(sourcepart), layer);
						}
					}
					
					List props = (List)args.get("properties");
					setProperties(ret, props, fetcher);
					
					return ret;
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "texturedrectangle"), "parts", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "triangle"), "parts", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "rectangle"), "parts", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "regularpolygon"), "parts", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "ellipse"), "parts", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "text"), "parts", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawcondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "texturedrectangle")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("x", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("y", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatex", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatey", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatez", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("position", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("rotation", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("size", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("abspos", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("abssize", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("absrot", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(attcolconv, null)),
			new AttributeInfo(new AccessInfo("imagepath", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("layer", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(attintconv, null)),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					Object position = getProperty(args, "position");
					if(position==null)
					{
						position = Vector2Double.getVector2((Double)getProperty(args, "x"),
							(Double)getProperty(args, "y"));
					}				
					Object rotation = getProperty(args, "rotation");
					if(rotation==null)
					{
						Double rx = (Double)getProperty(args, "rotatex");
						Double ry = (Double)getProperty(args, "rotatey");
						Double rz = (Double)getProperty(args, "rotatez");
						rotation = Vector3Double.getVector3(rx!=null? rx: new Double(0), ry!=null? ry: new Double(0), rz!=null? rz: new Double(0));
					}
					Object size = getProperty(args, "size");
					if(size==null)
					{
						size = Vector2Double.getVector2((Double)getProperty(args, "width"),
							(Double)getProperty(args, "height"));
					}
					int absFlags = Boolean.TRUE.equals(getProperty(args, "abspos"))? Primitive.ABSOLUTE_POSITION : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "abssize"))? Primitive.ABSOLUTE_SIZE : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "absrot"))? Primitive.ABSOLUTE_ROTATION : 0;

					IParsedExpression exp = (IParsedExpression)getProperty(args, "drawcondition");
					return new TexturedRectangle(position, rotation, size, absFlags, getProperty(args, "color"), (String)getProperty(args, "imagepath"), exp);
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			}, 
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawcondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "triangle")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("x", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("y", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatex", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatey", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatez", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("position", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("rotation", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("size", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("abspos", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("abssize", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("absrot", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attcolconv),
			new AttributeInfo(new AccessInfo("layer", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attintconv),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					Object position = getProperty(args, "position");
					if(position==null)
					{
						position = Vector2Double.getVector2((Double)getProperty(args, "x"),
							(Double)getProperty(args, "y"));
					}				
					Object rotation = getProperty(args, "rotation");
					if(rotation==null)
					{
						Double rx = (Double)getProperty(args, "rotatex");
						Double ry = (Double)getProperty(args, "rotatey");
						Double rz = (Double)getProperty(args, "rotatez");
						rotation = Vector3Double.getVector3(rx!=null? rx: new Double(0), ry!=null? ry: new Double(0), rz!=null? rz: new Double(0));
					}
					Object size = getProperty(args, "size");
					if(size==null)
					{
						size = Vector2Double.getVector2((Double)getProperty(args, "width"),
							(Double)getProperty(args, "height"));
					}
					
					int absFlags = Boolean.TRUE.equals(getProperty(args, "abspos"))? Primitive.ABSOLUTE_POSITION : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "abssize"))? Primitive.ABSOLUTE_SIZE : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "absrot"))? Primitive.ABSOLUTE_ROTATION : 0;
					
					IParsedExpression exp = (IParsedExpression)getProperty(args, "drawcondition");
					return new Primitive(Primitive.PRIMITIVE_TYPE_TRIANGLE, position, rotation, size, absFlags, getProperty(args, "color"), exp);
					//return new Triangle(position, rotation, size, absFlags, getProperty(args, "color"), exp);
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawcondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "rectangle")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("x", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("y", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatex", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatey", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatez", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("position", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("rotation", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("size", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("abspos", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("abssize", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("absrot", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attcolconv),
			new AttributeInfo(new AccessInfo("layer", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attintconv),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()		
			{
				public Object createObject(Map args) throws Exception
				{
					Object position = getProperty(args, "position");
					if(position==null)
					{
						position = Vector2Double.getVector2((Double)getProperty(args, "x"),
							(Double)getProperty(args, "y"));
					}				
					Object rotation = getProperty(args, "rotation");
					if(rotation==null)
					{
						Double rx = (Double)getProperty(args, "rotatex");
						Double ry = (Double)getProperty(args, "rotatey");
						Double rz = (Double)getProperty(args, "rotatez");
						rotation = Vector3Double.getVector3(rx!=null? rx: new Double(0), ry!=null? ry: new Double(0), rz!=null? rz: new Double(0));
					}
					Object size = getProperty(args, "size");
					if(size==null)
					{
						size = Vector2Double.getVector2((Double)getProperty(args, "width"),
							(Double)getProperty(args, "height"));
					}
					int absFlags = Boolean.TRUE.equals(getProperty(args, "abspos"))? Primitive.ABSOLUTE_POSITION : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "abssize"))? Primitive.ABSOLUTE_SIZE : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "absrot"))? Primitive.ABSOLUTE_ROTATION : 0;
					
					IParsedExpression exp = (IParsedExpression)getProperty(args, "drawcondition");
					return new Primitive(Primitive.PRIMITIVE_TYPE_RECTANGLE, position, rotation, size, absFlags, getProperty(args, "color"), exp);
					//return new Rectangle(position, rotation, size, absFlags, getProperty(args, "color"), exp);
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawcondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "regularpolygon")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("x", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("y", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatex", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatey", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatez", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("width", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("position", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("rotation", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("size", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("abspos", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("abssize", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("absrot", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attcolconv),
			new AttributeInfo(new AccessInfo("vertices", null, null, new Integer(3), new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.INTEGER_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("layer", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attintconv),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()		
			{
				public Object createObject(Map args) throws Exception
				{
					Object position = getProperty(args, "position");
					if(position==null)
					{
						position = Vector2Double.getVector2((Double)getProperty(args, "x"),
							(Double)getProperty(args, "y"));
					}				
					Object rotation = getProperty(args, "rotation");
					if(rotation==null)
					{
						Double rx = (Double)getProperty(args, "rotatex");
						Double ry = (Double)getProperty(args, "rotatey");
						Double rz = (Double)getProperty(args, "rotatez");
						rotation = Vector3Double.getVector3(rx!=null? rx: new Double(0), ry!=null? ry: new Double(0), rz!=null? rz: new Double(0));
					}
					Object size = getProperty(args, "size");
					if(size==null)
					{
						size = Vector2Double.getVector2((Double)getProperty(args, "width"),
							(Double)getProperty(args, "height"));
					}
					
					int absFlags = Boolean.TRUE.equals(getProperty(args, "abspos"))? Primitive.ABSOLUTE_POSITION : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "abssize"))? Primitive.ABSOLUTE_SIZE : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "absrot"))? Primitive.ABSOLUTE_ROTATION : 0;
					
					int vertices  = getProperty(args, "vertices")==null? 3: 
						((Integer)getProperty(args, "vertices")).intValue();
					
					IParsedExpression exp = (IParsedExpression)getProperty(args, "drawcondition");
					return new RegularPolygon(position, rotation, size, absFlags, getProperty(args, "color"), vertices, exp);
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawcondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)
			})));
	
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "ellipse")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("x", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("y", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatex", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatey", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("rotatez", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("width", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null,null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("position", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("rotation", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("size", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("abspos", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("abssize", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("absrot", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attcolconv),
			new AttributeInfo(new AccessInfo("layer", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attintconv),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()		
			{
				public Object createObject(Map args) throws Exception
				{
					Object position = getProperty(args, "position");
					if(position==null)
					{
						position = Vector2Double.getVector2((Double)getProperty(args, "x"),
							(Double)getProperty(args, "y"));
					}				
					Object rotation = getProperty(args, "rotation");
					if(rotation==null)
					{
						Double rx = (Double)getProperty(args, "rotatex");
						Double ry = (Double)getProperty(args, "rotatey");
						Double rz = (Double)getProperty(args, "rotatez");
						rotation = Vector3Double.getVector3(rx!=null? rx: new Double(0), ry!=null? ry: new Double(0), rz!=null? rz: new Double(0));
					}
					Object size = getProperty(args, "size");
					if(size==null)
					{
						size = Vector2Double.getVector2((Double)getProperty(args, "width"),
							(Double)getProperty(args, "height"));
					}
					
					int absFlags = Boolean.TRUE.equals(getProperty(args, "abspos"))? Primitive.ABSOLUTE_POSITION : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "abssize"))? Primitive.ABSOLUTE_SIZE : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "absrot"))? Primitive.ABSOLUTE_ROTATION : 0;
					
					IParsedExpression exp = (IParsedExpression)getProperty(args, "drawcondition");
					return new Primitive(Primitive.PRIMITIVE_TYPE_ELLIPSE, position, rotation, size, absFlags, getProperty(args, "color"), exp);
					//return new Ellipse(position, rotation, size, absFlags, getProperty(args, "color"), exp);
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawcondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "text")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("x", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("y", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("position", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("font", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.STRING_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("style", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.INTEGER_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("size", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.INTEGER_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("abspos", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("abssize", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atcolconv),
			new AttributeInfo(new AccessInfo("layer", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attintconv),
			new AttributeInfo(new AccessInfo("text", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.STRING_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("align", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.STRING_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					Object position = getProperty(args, "position");
					if(position==null)
					{
						position = Vector2Double.getVector2((Double)getProperty(args, "x"),
							(Double)getProperty(args, "y"));
					}
					
					String fontname = (String) getProperty(args, "font");
					if(fontname==null)
					{
						fontname = "Default";
					}
					Integer fontstyle = (Integer) getProperty(args, "style");
					if (fontstyle==null)
					{
						fontstyle = new Integer(Font.PLAIN);
					}
					Integer fontsize = (Integer) getProperty(args, "size");
					if (fontsize==null)
					{
						fontsize = new Integer(12);
					}
					Font font = new Font(fontname, fontstyle.intValue(), fontsize.intValue());
					
					String text = (String) getProperty(args, "text");
					text = String.valueOf(text);
					
					// Hack! Upstream needs to provide _proper_ newlines not a \n literal
					// Attributes are probably not the right place for text...
					text = text.replaceAll("\\\\n", "\n").replaceAll("\\\\\\\\", "\\");
					
					String aligntxt = String.valueOf(getProperty(args, "align"));
					int align = Text.ALIGN_LEFT;
					if (aligntxt.equals("right"))
						align = Text.ALIGN_RIGHT;
					else if (aligntxt.equals("center"))
						align = Text.ALIGN_CENTER;
					
					int absFlags = Boolean.TRUE.equals(getProperty(args, "abspos"))? Primitive.ABSOLUTE_POSITION : 0;
					absFlags |= Boolean.TRUE.equals(getProperty(args, "abssize"))? Primitive.ABSOLUTE_SIZE : 0;
					
					IParsedExpression exp = (IParsedExpression)getProperty(args, "drawcondition");
					return new Text(position, font, (Color)getProperty(args, "color"), text, align, absFlags, exp);
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "drawcondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "gridlayer")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attcolconv),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("type", null, null, "gridlayer", new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IVector2 size = Vector2Double.getVector2((Double)getProperty(args, "width"),
							(Double)getProperty(args, "height"));
					return new GridLayer(size, getProperty(args, "color"));
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "tiledlayer")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("imagepath", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attcolconv),
			new AttributeInfo(new AccessInfo("type", null, null, "tiledlayer", new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					IVector2 size = Vector2Double.getVector2((Double)getProperty(args, "width"),
						(Double)getProperty(args, "height"));
					return new TiledLayer(size, getProperty(args, "color"), (String)getProperty(args, "imagepath"));
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "colorlayer")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("color", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attcolconv),
			new AttributeInfo(new AccessInfo("type", null, null, "colorlayer", new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("creator", null, null, new IObjectCreator()
			{
				public Object createObject(Map args) throws Exception
				{
					return new Layer(Layer.LAYER_TYPE_COLOR, getProperty(args, "color"));
				}
			}, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "spaceexecutor")}), new ObjectInfo(MultiCollection.class), 
			new MappingInfo(ti_po, null, new AttributeInfo(new AccessInfo("expression", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv)
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "envspacetype"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "envspace"), new QName(uri, "property")}), new ObjectInfo(HashMap.class),
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "processtype"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "tasktype"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "actiontype"), new QName(uri, "property")}), new ObjectInfo(HashMap.class),
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "percepttype"), new QName(uri, "componenttypes"), new QName(uri, "componenttype")}), new ObjectInfo(HashMap.class),			
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "perceptgenerator"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "perceptprocessor"), new QName(uri, "property")}), new ObjectInfo(HashMap.class),
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "dataview"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "perspective"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "object"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "avatar"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "process"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "objecttype"), new QName(uri, "property")}), new ObjectInfo(MObjectTypeProperty.class),
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value"), atexconv),
			new AttributeInfo[] {
				new AttributeInfo(new AccessInfo("class", "type", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
				new AttributeInfo(new AccessInfo("dynamic"), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
				new AttributeInfo(new AccessInfo("event"), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			}
//			
//			new AttributeInfo[]{
//			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
//			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
//			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null)),
//			new AttributeInfo(new AccessInfo("event", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
//			}
		)));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "drawable"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "spaceexecutor"), new QName(uri, "property")}), new ObjectInfo(HashMap.class),
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "dataconsumer"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "plugin"), new QName(uri, "property")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			new AttributeInfo(new AccessInfo("dynamic", null, null, Boolean.FALSE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			})));

		// type instance declarations.
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "envspace")}), 
			new ObjectInfo(MEnvSpaceInstance.class, new IPostProcessor()
		{
				public Object postProcess(IContext context, Object object)
				{
					MEnvSpaceInstance	si	= (MEnvSpaceInstance)object;
					IModelInfo	model	= (IModelInfo)context.getRootObject();
					Object[] extypes = model.getExtensionTypes();
					for(int i=0; i<extypes.length; i++)
					{
						if(extypes[i] instanceof MEnvSpaceType && ((MEnvSpaceType)extypes[i]).getName().equals(si.getTypeName()))
						{
							si.setType((MEnvSpaceType)extypes[i]);
							break;
						}
					}
					
					return null;
				}
				
				public int getPass()
				{
					return 1;
				}
		}),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("type", "typeName")),
			new AttributeInfo(new AccessInfo("width", null, null, null, new BeanAccessInfo("property")), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("height", null, null, null, new BeanAccessInfo("property")), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("depth", null, null, null, new BeanAccessInfo("property")), new AttributeConverter(BasicTypeConverter.DOUBLE_CONVERTER, null)),
			new AttributeInfo(new AccessInfo("border", null, null, null, new BeanAccessInfo("property"))),
			new AttributeInfo(new AccessInfo("neighborhood", null, null, null, new BeanAccessInfo("property")))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "property"), "properties", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "object"), "objects", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "avatar"), "avatars", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "process"), "processes", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "spaceaction"), "spaceactions", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "dataprovider"), "dataproviders", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "dataconsumer"), "dataconsumers", null, null, new BeanAccessInfo("property"))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "observer"), "observers", null, null, new BeanAccessInfo("property")))			
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "object")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("type", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("number", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.INTEGER_CONVERTER, null))
			})));
			
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "avatar")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("type", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("owner", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			})));
			
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "process")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(ti_po, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("type", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "spaceaction")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("type", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "parameter"), "parameters", null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "spaceaction"), new QName(uri, "parameter")}), new ObjectInfo(HashMap.class),
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("value", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "observer")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("dataview", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("perspective", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("killonexit", null, null, Boolean.TRUE, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "plugin"), "plugins", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			})));
			
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "plugin")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			//new BeanAccessInfo("class", null, null, typeconv, null, ""),
			new AttributeInfo(new AccessInfo("class", "clazz", null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv),
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "property"), "properties", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "dataprovider")}), new ObjectInfo(MultiCollection.class), 
			new MappingInfo(null,
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS)))
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "source"), "source", null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new SubobjectInfo(new AccessInfo(new QName(uri, "data"), "data", null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "data")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("content", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			})));

		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "source")}), new ObjectInfo(HashMap.class), 
			new MappingInfo(null, null, new AttributeInfo(new AccessInfo("content", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), atexconv),
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("objecttype", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("aggregate", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), new AttributeConverter(BasicTypeConverter.BOOLEAN_CONVERTER, null))},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "includecondition"), null, null, null, new BeanAccessInfo(AccessInfo.THIS)), suexconv)	
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "dataconsumer")}), new ObjectInfo(MultiCollection.class),
			new MappingInfo(null, new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("name", null, null, null, new BeanAccessInfo(AccessInfo.THIS))),
			new AttributeInfo(new AccessInfo("class", null, null, null, new BeanAccessInfo(AccessInfo.THIS)), attypeconv)
			},
			new SubobjectInfo[]{
			new SubobjectInfo(new AccessInfo(new QName(uri, "property"), "properties", null, null, new BeanAccessInfo(AccessInfo.THIS)))
			})));
		
		TYPES	= Collections.unmodifiableSet(types);
	}
	
	/**
	 *  Get the XML mapping.
	 */
	public static Set getXMLMapping()
	{
		return TYPES;
	}
		
	/**
	 *  Parse class names.
	 */
	static class ExpressionConverter implements IStringObjectConverter
	{
		// Hack!!! Should be configurable.
		protected static IExpressionParser	exp_parser	= new JavaCCExpressionParser();

		/**
		 *  Convert a string value to a type.
		 *  @param val The string value to convert.
		 */
		public Object convertString(String val, IContext context)
		{
			Object ret = null;
			
//			System.out.println("Found expression: "+val);
			try
			{
				ret = exp_parser.parseExpression((String)val, ((IModelInfo)
					context.getRootObject()).getAllImports(), null, context.getClassLoader());
			}
			catch(Exception e)
			{
				reportError(context, e.toString());
			}
			
			return ret;
		}
	}
	
	/**
	 *  Parse class names.
	 */
	public static class ClassConverter	implements IStringObjectConverter
	{
		/**
		 *  Convert a string value to a type.
		 *  @param val The string value to convert.
		 */
		public Object convertString(String val, IContext context)
		{
			Object ret = val;
			if(val instanceof String)
			{
				ret = SReflect.findClass0((String)val, ((IModelInfo)
					context.getRootObject()).getAllImports(), context.getClassLoader());
				if(ret==null)
				{
					reportError(context, "Class not found: "+val);
				}
			}
			return ret;
		}
	}
	
	/**
	 *  Parse class names.
	 */
	static class ColorConverter	implements IStringObjectConverter
	{
		/**
		 *  Convert a string value to a type.
		 *  @param val The string value to convert.
		 */
		public Object convertString(String val, IContext context)
		{
			Object ret = val;
		
//			if(!(val instanceof String))
//				throw new RuntimeException("Source value must be string: "+val);
		
			if(val instanceof String)
			{
				Color c = SGUI.stringToColor(val);
				if (c != null)
					ret = c;
			}
			
//			System.out.println("tolerant conv: "+val+" "+ret+" "+(ret!=null? ""+ret.getClass(): ""));
			
			return ret;
		}
	}
	
	/**
	 *  String -> Double/String converter.
	 *  Converts to a double if possible. Otherwise string will be kept.
	 */
	static class TolerantDoubleTypeConverter implements IStringObjectConverter
	{
		/**
		 *  Convert a string value to another type.
		 *  @param val The string value to convert.
		 */
		public Object convertString(String val, IContext context)
		{
			Object ret = val;
			if(val instanceof String)
			{
				try{ret = new Double((String)val);}
				catch(Exception e){}
			}
			return ret;
		}
	}
	
	/**
	 *  String -> Double/String converter.
	 *  Converts to a integer if possible. Otherwise string will be kept.
	 */
	static class TolerantIntegerTypeConverter implements IStringObjectConverter
	{
		/**
		 *  Convert a string value to another type.
		 *  @param val The string value to convert.
		 */
		public Object convertString(String val, IContext context)
		{
			Object ret = val;
			if(val instanceof String)
			{
				try{ret = new Integer((String)val);}
				catch(Exception e){}
			}
			return ret;
		}
	}
	
	/**
	 *  String -> Double/String converter.
	 *  Converts to a integer if possible. Otherwise string will be kept.
	 */
	static class TolerantColorConverter implements IStringObjectConverter
	{
		/**
		 *  Convert a string value to another type.
		 *  @param val The string value to convert.
		 */
		public Object convertString(String val, IContext context)
		{
			Object ret = val;
			if(val instanceof String)
			{
				ret = new ColorConverter().convertString(val, context);
			}
			
			return ret;
		}
		
	}
	
	/**
	 *  Name attribute to object converter.
	 */
	static class NameAttributeToObjectConverter implements IObjectObjectConverter
	{
		/**
		 *  Test if a converter accepts a specific input type.
		 *  @param inputtype The input type.
		 *  @return True, if accepted.
		 */
		public Object convertObject(Object val, IContext context)
		{
			Object ret = null;
			if(val instanceof Map)
			{
				ret = (String)((Map)val).get("name");
			}
			return ret;
		}
	}

	/**
	 *  Report an error including the line and column.
	 */
	protected static void reportError(IContext context, String error)
	{
		Map	user	= (Map)context.getUserContext();
		MultiCollection	report	= (MultiCollection)user.get(ComponentXMLReader.CONTEXT_ENTRIES);
		String	pos;
		Tuple	stack	= new Tuple(((ReadContext)context).getStack());
		if(stack.getEntities().length>0)
		{
			StackElement	se	= (StackElement)stack.get(stack.getEntities().length-1);
			pos	= " (line "+se.getLocation().getLineNumber()+", column "+se.getLocation().getColumnNumber()+")";
		}
		else
		{
			pos	= " (line 0, column 0)";			
		}
		report.put(stack, error+pos);
	}
	
	/**
	 *  Set properties on a IPropertyObject.
	 *  @param object The IPropertyObject.
	 *  @param properties A list properties (containing maps with "name", "value" keys).
	 *  @param fetcher The fetcher for parsing the Java expression (can provide
	 *  predefined values to the expression)
	 */
	public static void setProperties(IPropertyObject object, List properties, IValueFetcher fetcher)
	{
		if(properties!=null)
		{
			for(int i=0; i<properties.size(); i++)
			{
				Map prop = (Map)properties.get(i);
				IParsedExpression exp = (IParsedExpression)prop.get("value");
				boolean dyn = ((Boolean)prop.get("dynamic")).booleanValue();
				if(dyn)
					object.setProperty((String)prop.get("name"), exp);
				else
					object.setProperty((String)prop.get("name"), exp==null? null: exp.getValue(fetcher));
			}
		}
	}
	
	/**
	 *  Set properties on a map.
	 *  @param properties A list properties (containing maps with "name", "value" keys).
	 *  @param fetcher The fetcher for parsing the Java expression (can provide
	 *  predefined values to the expression)
	 */
	public static Map convertProperties(List properties, IValueFetcher fetcher)
	{
		HashMap ret = null;
		if(properties!=null)
		{
			ret = new HashMap();
			for(int i=0; i<properties.size(); i++)
			{
				Map prop = (Map)properties.get(i);
				IParsedExpression exp = (IParsedExpression)prop.get("value");
				boolean dyn = ((Boolean)prop.get("dynamic")).booleanValue();
				if(dyn)
					ret.put((String)prop.get("name"), exp);
				else
					ret.put((String)prop.get("name"), exp==null? null: exp.getValue(fetcher));
			}
		}
		return ret;
	}
	
	/**
	 *  Get a property from a (multi)map.
	 *  @param map The map.
	 *  @param name The name.
	 *  @return The property.
	 */
	public static Object getProperty(Map map, String name)
	{
		try
		{
			Object tmp = map.get(name);
			return (tmp instanceof List)? ((List)tmp).get(0): tmp; 
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
