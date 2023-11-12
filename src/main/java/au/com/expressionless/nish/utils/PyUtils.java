package au.com.expressionless.nish.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import au.com.expressionless.nish.utils.python.PyScriptResult;
import au.com.expressionless.nish.utils.python.PyScriptRunner;
import org.jboss.logging.Logger;

public class PyUtils {

    static final Logger log = Logger.getLogger(PyUtils.class.getCanonicalName());

    private PyUtils() {

    }

    public static JsonObject tagText(byte[] data) { 

        // construct a list of arguments
        List<String> args = new ArrayList<>();
        args.add("--stdin");

        // run the script and get the results
        try {
            log.debug("Tagging text...");
            PyScriptRunner runner = new PyScriptRunner("txt_tag/txt_tag.py");        
            PyScriptResult result = runner
            .setStdinData(data)
            .setArgs(args)
            .setTimeout(5)
            .run();

            // script timed out
            if (result == null) {
                log.error("Text Tagging script timed out!");
                return null;
            }

            // script ran but there was an error in the script
            if (result.getExitValue() != 0) {
                log.error("Error occured during text tagging!");
                log.error(new String(result.getStderrData()));
                return null;
            }

            // script ran successfully  
            log.debug("Text tagging was successful!");
            log.debug(result.toString());

            JsonReader reader = Json.createReader(result.getStdout());
            return reader.readObject();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return null;
        }

    }
}
