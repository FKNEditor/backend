package au.com.expressionless.nish.utils.python;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import au.com.expressionless.nish.utils.GeneralUtils;

/**
 * PyScriptRunner is able to run scripts in the directory associated with the environment variable
 * "PY_SCRIPT_DIR". It handles the creation and destruction of the python process, as well as 
 * capturing of results.
 */
public class PyScriptRunner {
    private static final String PY_EXECUTABLE = "python3"; 
    private static final String PY_SCRIPT_DIR = GeneralUtils.getSystemEnv("PY_SCRIPT_DIR");
    private static final int DEFAULT_TIMEOUT = 5;

    private List<String> requiredArgs;
    private List<String> optionalArgs;
    private byte[] stdinData;
    private long timeout;


    /**
     * PyScriptRunner Constructor.
     * Creates a PyScriptRunner that will run a script in the directory
     * specified by the environment variable "PY_SCRIPT_DIR"
     * @param scriptName Name of script
     */
    public PyScriptRunner(String scriptName) {

        // set default arguments for running python script
        this.requiredArgs = new ArrayList<>();
        this.requiredArgs.add(PY_EXECUTABLE);
        this.requiredArgs.add(PY_SCRIPT_DIR + scriptName);

        // set other variables to defaults
        this.timeout = DEFAULT_TIMEOUT;
        this.optionalArgs = new ArrayList<>();
        this.stdinData = null;
    }

    /**
     * Sets data to be written to the stdin of the python process. Data is NOT copied
     * @param data Byte array with data to send to the python process
     */
    public PyScriptRunner setStdinData(byte[] data) {
        this.stdinData = data; 
        return this;
    }

    /**
     * Sets the list of arguments that will be run alongside the python script. Space delimited 
     * arguments should be added as separate elements i.e. "-f example.txt" should be written out as
     * runner.setArgs("-f", "example.txt")
     * @param args Space delimited arguments to be run with the python process
     */
    public PyScriptRunner setArgs(String... args) {
        return setArgs(args);
    }

    /**
     * Sets the list of arguments that will be run alongside the python script. Space delimited 
     * arguments must be added as separate args in the collection i.e. "-f test.txt" should be 
     * {"-f", "-test.txt"}
     * @param args A collection of space delimited args to be run with the python process
     */
    public PyScriptRunner setArgs(Collection<String> args) {
        this.optionalArgs.clear();
        this.optionalArgs.addAll(args);
        return this;
    }

    /**
     * Sets the timeout of the process. If seconds <= 0, the timeout of the process will be set to
     * the amount of seconds in Long.MAX_VALUE.
     * @param seconds Number of seconds to wait for a python script to run
     */
    public PyScriptRunner setTimeout(long seconds) {
        if (seconds <= 0)
            this.timeout = Long.MAX_VALUE;
        else
            this.timeout = seconds;
        return this;
    }

    /**
     * Runs the python script with the specified options. Returns results of the python script as 
     * {@link PyScriptResult}. Returns null if the python script times out.
     */
    public PyScriptResult run() throws IOException, InterruptedException {

        // compose the strings needed to run script
        List<String> commandList = new ArrayList<>();
        commandList.addAll(requiredArgs);
        commandList.addAll(optionalArgs);

        // Create a new process using commandList
        Process p = new ProcessBuilder()
        .command(commandList)
        .start();


        // write any stdin data to the process
        if (stdinData != null) {
            OutputStream stdin = p.getOutputStream();
            stdin.write(stdinData);
            stdin.flush();
            stdin.close();
        }

        // check if the script is taking too long
        // default timeout is 5 seconds
        boolean hasTimedOut = !p.waitFor(timeout, TimeUnit.SECONDS);
  
        // a timed out process should be destroyed, and return null 
        if (hasTimedOut) {

            // try to destroy normally      
            // a process that continues to run after this call
            // should be destroyed forcibly
            p.destroy();
            if (p.isAlive())
                p.destroyForcibly();
            return null;
        }

        // if the script timed out, return null, otherwise, return the result
        return new PyScriptResult(
            p.exitValue(), 
            p.getInputStream(), 
            p.getErrorStream()
        );
    }
}
