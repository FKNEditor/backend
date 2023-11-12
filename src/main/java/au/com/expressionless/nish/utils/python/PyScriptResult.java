package au.com.expressionless.nish.utils.python;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.utils.IOUtils;

/**
 * PyScriptResult captures any data emitted as a result of running a python script through
 * PyScriptRunner.
 */
public class PyScriptResult {

    private final int exitValue;
    private final InputStream stdout;
    private final InputStream stderr;

    /**
     * PyScriptResult constructor.
     */
    PyScriptResult(int exitValue, InputStream stdout, InputStream stderr) {
        this.exitValue = exitValue;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Returns byte data of the finished process's stdout.
     */
    public byte[] getStdoutData() throws IOException {
        return IOUtils.toByteArray(this.stdout);
    }
    /**
     * Returns byte data of the finished process's stderr.
     */
    public byte[] getStderrData() throws IOException{
        return IOUtils.toByteArray(this.stderr);
    }

    /**
     * Returns InputStream corresponding to the stdout of the finished process
     */
    public InputStream getStdout() {
        return this.stdout;
    }

    /**
     * Returns InputStream corresponding to the stderr of the finished process
     */
    public InputStream getStderr() {
        return this.stderr;
    }

    /**
     * Returns the exit value of the finished process.
     */
    public int getExitValue() {
        return this.exitValue;
    }

    public String toString() {
        
        return
        "PyScriptResult@" + this.hashCode() + "[" +
        "exitValue=" + String.valueOf(exitValue) + "]";
    }
}


