package ctbrec.recorder.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.Config;
import ctbrec.Version;
import ctbrec.event.EventBusHolder;
import ctbrec.event.EventHandler;
import ctbrec.event.EventHandlerConfiguration;
import ctbrec.recorder.LocalRecorder;
import ctbrec.recorder.OnlineMonitor;
import ctbrec.recorder.Recorder;
import ctbrec.sites.Site;
import ctbrec.sites.bonga.BongaCams;
import ctbrec.sites.cam4.Cam4;
import ctbrec.sites.camsoda.Camsoda;
import ctbrec.sites.chaturbate.Chaturbate;
import ctbrec.sites.mfc.MyFreeCams;

public class HttpServer {

    private static final transient Logger LOG = LoggerFactory.getLogger(HttpServer.class);
    private Recorder recorder;
    private OnlineMonitor onlineMonitor;
    private Config config;
    private Server server = new Server();
    private List<Site> sites = new ArrayList<>();

    public HttpServer() throws Exception {
        logEnvironment();
        createSites();
        System.setProperty("ctbrec.server.mode", "1");
        if(System.getProperty("ctbrec.config") == null) {
            System.setProperty("ctbrec.config", "server.json");
        }
        try {
            Config.init(sites);
        } catch (Exception e) {
            LOG.error("Couldn't load config", e);
            System.exit(1);
        }

        addShutdownHook(); // for graceful termination

        registerAlertSystem();

        config = Config.getInstance();
        if(config.getSettings().key != null) {
            LOG.info("HMAC authentication is enabled");
        }
        recorder = new LocalRecorder(config);
        OnlineMonitor monitor = new OnlineMonitor(recorder);
        monitor.start();
        for (Site site : sites) {
            if(site.isEnabled()) {
                site.init();
            }
        }
        startHttpServer();
    }

    private void createSites() {
        sites.add(new Chaturbate());
        sites.add(new MyFreeCams());
        sites.add(new Camsoda());
        sites.add(new Cam4());
        sites.add(new BongaCams());
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOG.info("Shutting down");
                if(onlineMonitor != null) {
                    onlineMonitor.shutdown();
                }
                if(recorder != null) {
                    recorder.shutdown();
                }
                try {
                    server.stop();
                } catch (Exception e) {
                    LOG.error("Couldn't stop HTTP server", e);
                }
                try {
                    Config.getInstance().save();
                } catch (IOException e) {
                    LOG.error("Couldn't save configuration", e);
                }
                LOG.info("Good bye!");
            }
        });
    }

    private void startHttpServer() throws Exception {
        server = new Server();

        HttpConfiguration config = new HttpConfiguration();
        config.setSendServerVersion(false);
        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(config));
        http.setPort(this.config.getSettings().httpPort);
        http.setIdleTimeout(this.config.getSettings().httpTimeout);
        server.addConnector(http);

        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { handler });
        server.setHandler(handlers);

        RecorderServlet recorderServlet = new RecorderServlet(recorder, sites);
        ServletHolder holder = new ServletHolder(recorderServlet);
        handler.addServletWithMapping(holder, "/rec");

        HlsServlet hlsServlet = new HlsServlet(this.config);
        holder = new ServletHolder(hlsServlet);
        handler.addServletWithMapping(holder, "/hls/*");

        try {
            server.start();
            server.join();
        } catch (BindException e) {
            LOG.error("Port {} is already in use", http.getPort(), e);
            System.exit(1);
        }
    }

    private void registerAlertSystem() {
        for (EventHandlerConfiguration config : Config.getInstance().getSettings().eventHandlers) {
            EventHandler handler = new EventHandler(config);
            EventBusHolder.register(handler);
            LOG.debug("Registered event handler for {} {}", config.getEvent(), config.getName());
        }
        LOG.debug("Alert System registered");
    }

    private void logEnvironment() {
        LOG.debug("OS:\t{} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        LOG.debug("Java:\t{} {} {}", System.getProperty("java.vendor"), System.getProperty("java.vm.name"), System.getProperty("java.version"));
        try {
            LOG.debug("ctbrec server {}", getVersion().toString());
        } catch (IOException e) {}
    }

    private Version getVersion() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("version")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String versionString = reader.readLine();
            Version version = Version.of(versionString);
            return version;
        }
    }

    public static void main(String[] args) throws Exception {
        new HttpServer();
    }
}
