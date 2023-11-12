package au.com.expressionless.nish.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

public class GeneralUtils {
    
    /**
     * Get a System Environment Variable, with an optional default supplied if it
     * is missing. If no default is supplied, this method will throw a {@link RuntimeException}
     * @param key - key of the environment variable
     * @param fallback - fallback value to use if there is no environment variable under the key
     * @return the value of the environment variable, or the fallback if the env is missing
     * @throws RuntimeException - if there is no fallback supplied and the env is missing
     */
    public static String getSystemEnv(String key) {
        return getSystemEnv(key, null);
    }

    /**
     * Get a System Environment Variable, with an optional default supplied if it
     * is missing. If no default is supplied, this method will throw a {@link RuntimeException}
     * @param key - key of the environment variable
     * @param fallback - fallback value to use if there is no environment variable under the key
     * @return the value of the environment variable, or the fallback if the env is missing
     * @throws RuntimeException - if there is no fallback supplied and the env is missing
     */
    public static String getSystemEnv(String key, String fallback) throws RuntimeException {
        String value = System.getenv(key);
        if(value == null) {
            if(fallback == null)
                throw new RuntimeException("Could not find environment variable: " + key) {};
            return fallback;
        }

        return value;
    }

    // write a file
    public static File writeFile(byte[] content, String filename) throws IOException {

        File file = new File(filename);
        FileOutputStream fop = new FileOutputStream(file);

        fop.write(content);
        fop.flush();
        fop.close();

        return file;
    }

    public static JsonObject JsonDeserialise(byte[] content) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content);
            JsonReader reader = Json.createReader(bais)) {
            return reader.readObject();
        }
    }

    public static byte[] JsonSerialise(JsonObject object) throws IOException {
        try (ByteArrayOutputStream oos = new ByteArrayOutputStream(); 
            JsonWriter writer = Json.createWriter(oos)) {
            writer.writeObject(object);
            writer.close();
            oos.flush();
            return oos.toByteArray();
        }
    }
    
    public static String makeLike(String str) {
        StringBuilder sb = new StringBuilder();
        if (str.charAt(0) != '%') {
           sb.append("%");
        }
  
        sb.append(str);
        if (!str.endsWith("%")) {
           sb.append("%");
        }
  
        return sb.toString();
    }  
}
