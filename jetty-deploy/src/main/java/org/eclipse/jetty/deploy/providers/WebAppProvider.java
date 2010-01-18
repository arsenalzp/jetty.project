package org.eclipse.jetty.deploy.providers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.deploy.App;
import org.eclipse.jetty.deploy.ConfigurationManager;
import org.eclipse.jetty.deploy.WebAppDeployer;
import org.eclipse.jetty.deploy.util.FileID;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;


/* ------------------------------------------------------------ */
/** Context directory App Provider.
 * <p>This specialization of {@link MonitoredDirAppProvider} is the
 * replacement for {@link WebAppDeployer} and it will scan a directory
 * only for war files or directories files.</p>
 * @see WebAppDeployer
 */
public class WebAppProvider extends ScanningAppProvider
{
    private boolean _extractWars = false;
    private boolean _parentLoaderPriority = false;
    private String _defaultsDescriptor;
    private Filter _filter;

    private static class Filter implements FilenameFilter
    {
        private File _contexts;
        
        public boolean accept(File dir, String name)
        {
            if (!dir.exists())
                return false;
            String lowername = name.toLowerCase();
            
            File file = new File(dir,name);
            // is it not a directory and not a war ?
            if (!file.isDirectory() && !lowername.endsWith(".war"))
                return false;
            
            // is it a directory for an existing war file?
            if (file.isDirectory() && 
                    (new File(dir,name+".war").exists() ||
                     new File(dir,name+".WAR").exists()))
            {
                return false;
            }
            
            // is there a contexts config file
            if (_contexts!=null)
            {
                String context=name;
                if (!file.isDirectory())
                    context=context.substring(0,context.length()-4);
                if (new File(_contexts,context+".xml").exists() ||
                    new File(_contexts,context+".XML").exists() )
                {
                    return false;
                }
            }
               
            return true;
        }
    }
    
    /* ------------------------------------------------------------ */
    public WebAppProvider()
    {
        super(new Filter());
        _filter=(Filter)_filenameFilter;
        setScanInterval(0);
    }

    /* ------------------------------------------------------------ */
    /** Get the extractWars.
     * @return the extractWars
     */
    public boolean isExtractWars()
    {
        return _extractWars;
    }

    /* ------------------------------------------------------------ */
    /** Set the extractWars.
     * @param extractWars the extractWars to set
     */
    public void setExtractWars(boolean extractWars)
    {
        _extractWars = extractWars;
    }

    /* ------------------------------------------------------------ */
    /** Get the parentLoaderPriority.
     * @return the parentLoaderPriority
     */
    public boolean isParentLoaderPriority()
    {
        return _parentLoaderPriority;
    }

    /* ------------------------------------------------------------ */
    /** Set the parentLoaderPriority.
     * @param parentLoaderPriority the parentLoaderPriority to set
     */
    public void setParentLoaderPriority(boolean parentLoaderPriority)
    {
        _parentLoaderPriority = parentLoaderPriority;
    }

    /* ------------------------------------------------------------ */
    /** Get the defaultsDescriptor.
     * @return the defaultsDescriptor
     */
    public String getDefaultsDescriptor()
    {
        return _defaultsDescriptor;
    }

    /* ------------------------------------------------------------ */
    /** Set the defaultsDescriptor.
     * @param defaultsDescriptor the defaultsDescriptor to set
     */
    public void setDefaultsDescriptor(String defaultsDescriptor)
    {
        _defaultsDescriptor = defaultsDescriptor;
    }

    /* ------------------------------------------------------------ */
    public String getContextXmlDir()
    {
        return _filter._contexts==null?null:_filter._contexts.toString();
    }

    /* ------------------------------------------------------------ */
    /**
     * Set the directory in which to look for context XML files.
     * <p>
     * If a webapp call "foo/" or "foo.war" is discovered in the monitored
     * directory, then the ContextXmlDir is examined to see if a foo.xml
     * file exists.  If it does, then this deployer will not deploy the webapp
     * and the ContextProvider should be used to act on the foo.xml file.
     * @see ContextProvider
     * @param contextsDir
     */
    public void setContextXmlDir(String contextsDir)
    {
        try
        {
            _filter._contexts=Resource.newResource(contextsDir).getFile();
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public ContextHandler createContextHandler(final App app) throws Exception
    {
        Resource resource = Resource.newResource(app.getOriginId());
        File file = resource.getFile();
        if (!resource.exists())
            throw new IllegalStateException("App resouce does not exist "+resource);

        String context = file.getName();
        
        if (file.isDirectory())
        {
            // must be a directory
        }
        else if (FileID.isWebArchiveFile(file))
        {
            // Context Path is the same as the archive.
            context = context.substring(0,context.length() - 4);
        }
        else
            throw new IllegalStateException("unable to create ContextHandler for "+app);
        
        // special case of archive (or dir) named "root" is / context
        if (context.equalsIgnoreCase("root") || context.equalsIgnoreCase("root/"))
            context = URIUtil.SLASH;

        // Ensure "/" is Prepended to all context paths.
        if (context.charAt(0) != '/')
            context = "/" + context;

        // Ensure "/" is Not Trailing in context paths.
        if (context.endsWith("/") && context.length() > 0)
            context = context.substring(0,context.length() - 1);

        WebAppContext wah = new WebAppContext();
        wah.setContextPath(context);
        wah.setWar(file.getAbsolutePath());
        if (_defaultsDescriptor != null)
            wah.setDefaultsDescriptor(_defaultsDescriptor);
        wah.setExtractWAR(_extractWars);
        wah.setParentLoaderPriority(_parentLoaderPriority);

        return wah; 
    }
    
}
