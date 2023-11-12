package au.com.expressionless.nish.utils.pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.Json;
import javax.ws.rs.BadRequestException;

import org.jboss.logging.Logger;

import au.com.expressionless.nish.models.entity.edition.story.Bounds;
import au.com.expressionless.nish.utils.python.PyScriptResult;
import au.com.expressionless.nish.utils.python.PyScriptRunner;

/**
 *  General purpose class for dealing with PDF data, 
 *  which includes text extraction, image extraction 
 *  and thumbnail generation.
 */
public class PDFWrapper {
   
    private final File file;
    private final byte[] pdfData;

    static final Logger log = Logger.getLogger(PDFWrapper.class.getCanonicalName());

    /**
     * PDFWrapper constructor. The file must be a valid PDF file or this constructor will fail.
     * @param file A PDF file.
     */
    public PDFWrapper(File file) throws IOException, BadRequestException {
        
        // check that the PDF file is both not null and a proper PDF file
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (!isPDF(bytes))
            throw new BadRequestException("File is not a valid PDF file!");

        this.file = file;
        this.pdfData = bytes;
    }

    /**
     * PDFWrapper constructor. The file referenced by filePath must be a valid PDF file or this
     * constructor will fail.
     * @param filePath File path to a PDF file.
     */
    public PDFWrapper(String filePath) throws IOException, BadRequestException {
        this(new File(filePath)); 
    }

    /**
     * PDFWrapper constructor. This constructor should only be used when PDF data does not have an
     * associated file. Prefer to use {@link #PDFWrapper(File)} or {@link #PDFWrapper(String)} where
     * possible. The data in bytes must refer to a valid PDF file or this constructor will fail.
     * @param pdfData A byte array associated with PDF data.
     */
    public PDFWrapper(byte[] pdfData) throws BadRequestException {

        // check that the byte data refers to a proper PDF file
        if (!isPDF(pdfData))
            throw new BadRequestException("Bytes does not contain a valid PDF file");

        this.pdfData = pdfData;
        this.file = null;
    }

    /**
     * Extracts text from a  a page in the PDF. Returns a JSON object as a string. If the text 
     * extraction is unsuccessful, this method returns null.
     * @param pageIndex Index of the page (0 based) to retrieve text from.
     * @return A JSON object converted to a String, returns null on failure.
     */
    public JsonObject getText(int pageIndex) {
        return getText(pageIndex, null);
    }

    /**
     * Extracts text from a rectangular section of a page in the PDF. Returns a JSON object as a 
     * string. If text extraction is unsuccessful, this method returns null.
     * NOTE: JSON data format still in progress
     * @param pageIndex Index of the page (0 based) to retrieve text from.
     * @param bounds A list of rectangular sections in a page to extract text from. Extracted
     * text is ordered first by the column ordering in a single section, then by lists of rectangles
     * in rects.
     * @return A JSON object converted to a String, returns null on failure. 
     */
    public JsonObject getText(int pageIndex, Collection<Bounds> bounds) { 

        // construct a list of arguments
        List<String> args = new ArrayList<>();

        // if a file exists its preferable to read straight from it
        // otherwise its fine to send the data through stdin
        // --file-path file.pdf
        // --stdin
        if (file != null) {
            args.add("--file-path");
            args.add(file.toString());
        } else { 
            args.add("--stdin");
        }

        // add page number argument
        // --page-number pageNum
        args.add("--page-number");
        args.add(String.valueOf(pageIndex));

        // check if a clip is needed 
        // --clip X0 Y0 X1 Y1
        if (bounds != null) { 
            args.add("--clip");
            for (Bounds bound : bounds) {
                String clip = new StringBuilder()
                .append(String.valueOf(bound.getX()) + ",")
                .append(String.valueOf(bound.getY()) + ",")
                .append(String.valueOf(bound.getMaxX()) + ",")
                .append(String.valueOf(bound.getMaxY()))
                .toString();
                args.add(clip);
            } 
        }

        // run the script and get the results
        // TODO: Perhaps make the non-existance of a file more obvious 
        try {
            log.info("Extracting text from pdf...");
            PyScriptRunner runner = new PyScriptRunner("pdf_ext/pdf_ext.py");        
            PyScriptResult result = runner
            .setStdinData((file == null) ? pdfData : null)
            .setArgs(args)
            .setTimeout(10)
            .run();

            // script timed out
            if (result == null) {
                log.error("Text extraction script timed out!");
                return null;
            }

            // script ran but there was an error in the script
            if (result.getExitValue() != 0) {
                log.error("Error occured during text extraction!");
                log.error(new String(result.getStderrData()));
                return null;
            }

            // script ran successfully  
            log.info("Text extraction was successful!");
            log.info(result.toString());



            // parse json into jsonObject
            JsonReader reader = Json.createReader(result.getStdout());
            return reader.readObject();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return null;
        }
    }


    /**
     * Returns a Buffered image containing a jpeg thumbnail image of the PDF. Returns null on 
     * failure. 
     */
    public BufferedImage generateThumbnail() {

        // generate thumbnail
        try {
            return PDFThumbnailBuilder
            .builder(pdfData)
            .fromPage(0)
            .setSize(600)
            .setDPI(300)
            .build();
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Returns whether or not the data in the given byte array represents a 
     * PDF file. Taken from https://stackoverflow.com/a/35517156
     */
    private boolean isPDF(byte[] data) {

        log.debug("Checking pdf...");
        // scan header for %PDF-
        if (data == null || data.length < 5) {
            log.debug("Invalid Data!");
            return false;
        }

        if (data[0] == 0x25 && // %
            data[1] == 0x50 && // P
            data[2] == 0x44 && // D
            data[3] == 0x46 && // F
            data[4] == 0x2D) { // -
            
            // check last 8 bytes for %%EOF with optional white-space
            int offset = data.length - 8; 
            int count = 0;            
            boolean hasSpace = false;
            boolean hasCr = false;
            boolean hasLf = false;
            while (offset < data.length) {
                if (count == 0 && data[offset] == 0x25) count++; // % 
                if (count == 1 && data[offset] == 0x25) count++; // %
                if (count == 2 && data[offset] == 0x45) count++; // E
                if (count == 3 && data[offset] == 0x4F) count++; // O 
                if (count == 4 && data[offset] == 0x46) count++; // F 

                // optional flags for meta info
                if (count == 5 && data[offset] == 0x20) hasSpace = true; 
                if (count == 5 && data[offset] == 0x0D) hasCr    = true; 
                if (count == 5 && data[offset] == 0x0A) hasLf    = true; 
                offset++;
            }
    
            // PDF is valid if %%EOF was found at the end of the file
            // print version of PDF to debug
            if (count == 5) {
                String version = data.length <= 13 
                ? "?"
                : new StringBuilder()
                    .append((char)data[5])
                    .append((char)data[6])
                    .append((char)data[7])
                    .toString();

                log.debug(
                    "Version : " + version + " | " +
                    "Space : " + Boolean.toString(hasSpace) + " | " +
                    "CR : " + Boolean.toString(hasCr) + " | " +
                    "LF : " + Boolean.toString(hasLf) 
                );
                return true;
            }
        }
        
        log.debug("Invalid PDF");
        return false;
    }
}
