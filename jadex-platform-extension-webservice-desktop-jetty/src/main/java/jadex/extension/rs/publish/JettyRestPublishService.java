package jadex.extension.rs.publish;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.PublishInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.publish.IPublishService;
import jadex.commons.Tuple2;
import jadex.commons.collection.MultiCollection;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.AgentCreated;

/**
 *  Publish service without Jersey directly using Jetty container.
 *  
 *  todo: make abstract base class without Jetty deps
 *  
 *  note: Jetty releases are Java 1.8 only.
 *  
 */
@Service
public class JettyRestPublishService extends AbstractRestPublishService
{
	// Jetty requires 1.8
//	static
//	{
//		String ver = System.getProperty("java.version");
//		
//		if(Float.parseFloat(ver.substring(0,3)) < 1.8f)
//		{
//			System.out.println("WARNING: Jetty requires Java 1.8");
//		}
//	}
	
	// Hack constant for enabling multi-part :-(
	private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

    /** The servers per service id. */
    protected Map<IServiceIdentifier, Server> sidservers;

    /** The servers per port. */
    protected Map<Integer, Server> portservers;
    
    /** Infos for unpublishing. */
    protected Map<IServiceIdentifier, Tuple2<Server, ContextHandler>> unpublishinfos = new HashMap<IServiceIdentifier, Tuple2<Server,ContextHandler>>();
    
    @AgentCreated
    public void start()
    {
    	System.out.println("Jetyy sta");
    }
    
    /**
     *  Test if publishing a specific type is supported (e.g. web service).
     *  @param publishtype The type to test.
     *  @return True, if can be published.
     */
    public IFuture<Boolean> isSupported(String publishtype)
    {
        return IPublishService.PUBLISH_RS.equals(publishtype) ? IFuture.TRUE : IFuture.FALSE;
    }

    /**
     *  Publish a service.
     *  @param cl The classloader.
     *  @param service The original service.
     *  @param pid The publish id (e.g. url or name).
     */
    public IFuture<Void> publishService(final IServiceIdentifier serviceid, final PublishInfo info)
    {
        try
        {
        	//final IService service = (IService) SServiceProvider.getService(component, serviceid).get();
        	
            final URI uri = new URI(getCleanPublishId(info.getPublishId()));
            Server server = (Server)getHttpServer(uri, info);
            System.out.println("Adding http handler to server: "+uri.getPath());

            ContextHandlerCollection collhandler = (ContextHandlerCollection)server.getHandler();

            final MultiCollection<String, MappingInfo> mappings = evaluateMapping(serviceid, info);

            ContextHandler ch = new ContextHandler()
            {
            	protected IService service = null;
            	
                public void doHandle(String target, Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
                    throws IOException, ServletException
                {
                	if(service==null)
                		service = (IService)SServiceProvider.getService(component.getExternalAccess(), serviceid).get();
                	
                    // Hack to enable multi-part
                    // http://dev.eclipse.org/mhonarc/lists/jetty-users/msg03294.html
                    if(request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) 
                    	baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
                	
                	handleRequest(service, mappings, request, response, new Object[]{target, baseRequest});
                	
//                  System.out.println("handler is: "+uri.getPath());
                    baseRequest.setHandled(true);
                }
            };
            ch.setContextPath(uri.getPath());
            collhandler.addHandler(ch);
            unpublishinfos.put(serviceid, new Tuple2<Server,ContextHandler>(server, ch));
            ch.start(); // must be started explicitly :-(((

            if(sidservers==null)
                sidservers = new HashMap<IServiceIdentifier, Server>();
            sidservers.put(serviceid, server);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
        return IFuture.DONE;
    }

    /**
     *  Get or start an api to the http server.
     */
    public Object getHttpServer(URI uri, PublishInfo info)
    {
        Server server = null;

        try
        {
//            URI baseuri = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
            server = portservers==null? null: portservers.get(uri.getPort());

            if(server==null)
            {
                System.out.println("Starting new server: "+uri.getPort());
                server = new Server(uri.getPort());
                server.dumpStdErr();

                ContextHandlerCollection collhandler = new ContextHandlerCollection();
                server.setHandler(collhandler);

                server.start();
//              server.join();

                if(portservers==null)
                    portservers = new HashMap<Integer, Server>();
                portservers.put(uri.getPort(), server);
            }
        }
        catch(RuntimeException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }

        return server;
    }

    /**
     *  Unpublish a service.
     *  @param sid The service identifier.
     */
    public IFuture<Void> unpublishService(IServiceIdentifier sid)
    {
    	Tuple2<Server,ContextHandler> unpublish = unpublishinfos.remove(sid);
    	if (unpublish != null)
    		((ContextHandlerCollection)unpublish.getFirstEntity().getHandler()).removeHandler(unpublish.getSecondEntity());
//        throw new UnsupportedOperationException();
    	return IFuture.DONE;
    }

    /**
     *  Publish a static page (without ressources).
     */
    public IFuture<Void> publishHMTLPage(String pid, String vhost, final String html)
    {
    	try
        {
    		String clpid = pid.replace("[", "").replace("]", "");
    		URI uri = new URI(clpid);
        	//final IService service = (IService) SServiceProvider.getService(component, serviceid).get();
        	
            Server server = (Server)getHttpServer(uri, null);
            System.out.println("Adding http handler to server: "+uri.getPath());

            ContextHandlerCollection collhandler = (ContextHandlerCollection)server.getHandler();

            ContextHandler ch = new ContextHandler()
            {
                public void doHandle(String target, Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
                    throws IOException, ServletException
                {
                	response.getWriter().write(html);
                	
//                  System.out.println("handler is: "+uri.getPath());
                    baseRequest.setHandled(true);
                }
            };
            ch.setContextPath(uri.getPath());
            collhandler.addHandler(ch);
            ch.start(); // must be started explicitly :-(((
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
        
        return IFuture.DONE;
    }

    /**
     *  Publish file resources from the classpath.
     */
    public IFuture<Void> publishResources(URI uri, String rootpath)
    {
        throw new UnsupportedOperationException();
    }

    /**
     *  Publish file resources from the file system.
     */
    public IFuture<Void> publishExternal(URI uri, String rootpath)
    {
        throw new UnsupportedOperationException();
    }

	
	public IFuture<Void> publishRedirect(URI uri, String html)
	{
        throw new UnsupportedOperationException();
	}


	public IFuture<Void> unpublish(String vhost, URI uri)
	{
        throw new UnsupportedOperationException();
	}

	
	public IFuture<Void> mirrorHttpServer(URI sourceserveruri, URI targetserveruri, PublishInfo info)
	{
        throw new UnsupportedOperationException();
	}


	public IFuture<Void> shutdownHttpServer(URI uri)
	{
        throw new UnsupportedOperationException();
	}
}

