package ctbrec.event;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ctbrec.OS;
import ctbrec.event.EventHandlerConfiguration.ActionConfiguration;
import ctbrec.io.StreamRedirectThread;

public class ExecuteProgram extends Action {

    private static final transient Logger LOG = LoggerFactory.getLogger(ExecuteProgram.class);

    private String executable;

    public ExecuteProgram() {
        name = "execute program";
    }

    public ExecuteProgram(String executable) {
        this();
        this.executable = executable;
    }

    @Override
    public void accept(Event evt) {
        Runtime rt = Runtime.getRuntime();
        Process process = null;
        try {
            String[] args = evt.getExecutionParams();
            String[] cmd = new String[args.length+1];
            cmd[0] = executable;
            System.arraycopy(args, 0, cmd, 1, args.length);
            LOG.debug("Executing {}", Arrays.toString(cmd));
            process = rt.exec(cmd, OS.getEnvironment());

            // create threads, which read stdout and stderr of the player process. these are needed,
            // because otherwise the internal buffer for these streams fill up and block the process
            Thread std = new Thread(new StreamRedirectThread(process.getInputStream(), System.out));
            std.setName("Player stdout pipe");
            std.setDaemon(true);
            std.start();
            Thread err = new Thread(new StreamRedirectThread(process.getErrorStream(), System.err));
            err.setName("Player stderr pipe");
            err.setDaemon(true);
            err.start();

            process.waitFor();
            LOG.debug("Executing {} finished", executable);
        } catch (Exception e) {
            LOG.error("Error while executing {}", executable, e);
        }
    }

    @Override
    public void configure(ActionConfiguration config) {
        executable = (String) config.getConfiguration().get("file");
    }

    @Override
    public String toString() {
        return "execute " + executable;
    }
}
