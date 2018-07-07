package ctbrec.recorder.server;

import java.io.IOException;
import java.net.BindException;

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
import ctbrec.recorder.LocalRecorder;
import ctbrec.recorder.Recorder;

public class HttpServer {

    private static final transient Logger LOG = LoggerFactory.getLogger(HttpServer.class);
    private Recorder recorder;
    private Config config;
    private Server server = new Server();

    public HttpServer() throws Exception {
        if(System.getProperty("ctbrec.config") == null) {
            System.setProperty("ctbrec.config", "server.json");
        }
        try {
            Config.init();
        } catch (Exception e) {
            LOG.error("Couldn't load config", e);
            System.exit(1);
        }

        addShutdownHook(); // for graceful termination

        config = Config.getInstance();
        if(config.getSettings().key != null) {
            LOG.info("HMAC authentication is enabled");
        }
        recorder = new LocalRecorder(config);
        startHttpServer();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                LOG.info("Shutting down");
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

        RecorderServlet recorderServlet = new RecorderServlet(recorder);
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

    public static void main(String[] args) throws Exception {
        new HttpServer();
    }
}
