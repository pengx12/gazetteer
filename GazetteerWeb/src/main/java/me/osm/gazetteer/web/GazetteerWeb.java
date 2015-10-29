package me.osm.gazetteer.web;

import java.io.FileNotFoundException;
import java.io.IOException;

import me.osm.gazetteer.web.postprocessor.AllowOriginPP;
import me.osm.gazetteer.web.postprocessor.LastModifiedHeaderPostprocessor;
import me.osm.gazetteer.web.postprocessor.MarkHeaderPostprocessor;
import me.osm.gazetteer.web.serialization.SerializationProvider;
import me.osm.gazetteer.web.utils.OSMDocSinglton;
import me.osm.osmdoc.localization.L10n;

import org.restexpress.RestExpress;
import org.restexpress.util.Environment;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class GazetteerWeb {
	
	private static class ShutDownListener implements Runnable
	{
	    @Override
	    public void run()
	    {
	    	ESNodeHodel.stopNode();
	    	server.shutdown();
	    }
	}
	
	private static RestExpress server;
	private volatile static Configuration config = new Configuration();
	private static final Injector injector = Guice.createInjector(new AppInjector());
	
	public static void main(String[] args) throws Exception {
		
		initLog();
		
		config = loadEnvironment(args);
		ESNodeHodel.getClient();

		if(!"jar".equals(config.getPoiCatalogPath())) {
			L10n.setCatalogPath(config.getPoiCatalogPath());
		}

		OSMDocSinglton.initialize(config.getPoiCatalogPath());
		
		RestExpress.setSerializationProvider(new SerializationProvider());
		
		server = new RestExpress()
				.setUseSystemOut(false)
				.setName(config.getName())
				.addPostprocessor(new LastModifiedHeaderPostprocessor())
				.addPostprocessor(new AllowOriginPP())
				.addPostprocessor(new MarkHeaderPostprocessor())
				.addPreprocessor(new BasikAuthPreprocessor(null));

		Routes.defineRoutes(server);
		
		server.addMessageObserver(new HttpLogger());
		
		server.bind(config.getPort());
		Runtime runtime = Runtime.getRuntime();
		Thread thread = new Thread(new ShutDownListener());
        runtime.addShutdownHook(thread);
        
        LoggerFactory.getLogger(GazetteerWeb.class)
        	.info("{} server listening on port {}", config.getName(), config.getPort());
	}

	private static void initLog() {
		// assume SLF4J is bound to logback in the current environment
	    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	    
		try {
	      JoranConfigurator configurator = new JoranConfigurator();
	      configurator.setContext(context);
	      // Call context.reset() to clear any previous configuration, e.g. default 
	      // configuration. For multi-step configuration, omit calling context.reset().
	      context.reset(); 
	      configurator.doConfigure("config/logback.xml");
	    } catch (JoranException je) {
	      // StatusPrinter will handle this
	    }
		
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);

	}

	private static Configuration loadEnvironment(String[] args)
			throws FileNotFoundException, IOException {
		if (args.length > 0) {
			return Environment.from(args[0], Configuration.class);
		}

		try {
			return Environment.fromDefault(Configuration.class);
		}
		catch (Exception e) {
			return new Configuration();
		}
	}
	
	public static Configuration config() {
		return config;
	}

	public static Injector injector() {
		return injector;
	}
	
}
