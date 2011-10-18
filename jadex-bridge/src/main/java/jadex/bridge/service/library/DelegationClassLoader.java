package jadex.bridge.service.library;

import jadex.commons.SUtil;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DelegationClassLoader extends ClassLoader
{
	protected Map<URL, ClassLoader> delegates;
	
	public DelegationClassLoader(ClassLoader basecl)
	{
		super(basecl);
		this.delegates = Collections.synchronizedMap(new HashMap<URL, ClassLoader>());
//		System.out.println("d1 : "+this);
	}
	
	public DelegationClassLoader(ClassLoader basecl, Map<URL, ClassLoader> delegates)
	{
		super(basecl);
		this.delegates = delegates;
//		System.out.println("d2 : "+this);
	}
	
	public DelegationClassLoader(ClassLoader basecl, Object[] urls)
	{
		super(basecl);
//		System.out.println("d3 : "+this);
		
		this.delegates = new HashMap<URL, ClassLoader>();
		
		if(urls != null)
		{
			for (int i = 0; i < urls.length; ++i)
			{
				URL url = SUtil.toURL(urls[i]);
				delegates.put(url, new URLClassLoader(new URL[] {url}));
			}
		}
	}
	
	public Map<URL, ClassLoader> getDelegates()
	{
		synchronized(delegates)
		{
			return new HashMap<URL, ClassLoader>(delegates);
		}
	}
	
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		synchronized(delegates)
		{
			for (Iterator<ClassLoader> it = delegates.values().iterator(); it.hasNext();)
			{
				try
				{
					Class<?> clazz = it.next().loadClass(name);
					return clazz;
				}
				catch (ClassNotFoundException e)
				{
				}
			}
		}
		return super.findClass(name);
	}
	
	protected URL findResource(String name)
	{
		synchronized(delegates)
		{
			for (Iterator<ClassLoader> it = delegates.values().iterator(); it.hasNext();)
			{
				URL url = it.next().getResource(name);
				if (url != null)
					return url;
			}
		}
		return super.findResource(name);
	}
	
	protected Enumeration<URL> findResources(String name) throws IOException
	{
		Set<URL> res = new HashSet<URL>();
		synchronized(delegates)
		{
			for (Iterator<ClassLoader> it = delegates.values().iterator(); it.hasNext();)
				res.addAll(Collections.list(it.next().getResources(name)));
		}
		res.addAll(Collections.list(super.findResources(name)));
		return Collections.enumeration(res);
	}
}
