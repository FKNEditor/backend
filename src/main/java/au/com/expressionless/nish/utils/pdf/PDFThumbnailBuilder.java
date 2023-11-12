package au.com.expressionless.nish.utils.pdf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.awt.image.BufferedImage;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.imgscalr.Scalr;

/**
 * PDFThumbnailBuilder is a builder that generates PDF thumbnails
 */
public class PDFThumbnailBuilder {

    // default arguments
    private static final int DEFAULT_PAGE_INDEX = 0;
    private static final int DEFAULT_PAGE_SIZE = 600;
    private static final int DEFAULT_DPI = 300;

    // fields
    private final byte[] pdfData;
    private int pageIndex;
    private int size;
    private int dpi;

    /**
     * PDFThumbnailBuilder constructor. File must be a proper PDF or else builder().build() will 
     * throw an Exception.
     * @param file A PDF file.
     */
    private PDFThumbnailBuilder(File file) throws IOException {
        pdfData = Files.readAllBytes(file.toPath());
        pageIndex = DEFAULT_PAGE_INDEX;
        size = DEFAULT_PAGE_SIZE;
        dpi = DEFAULT_DPI;
    }

    /**
     * PDFThumbnailBuilder constructor. Byte array must be data referring to a proper PDF file or
     * else builder.build() with throw an Exception.
     * @param bytes Byte array holding PDF data.
     * */
    private PDFThumbnailBuilder(byte[] bytes) {
        pdfData = bytes;
        pageIndex = DEFAULT_PAGE_INDEX;
        size = DEFAULT_PAGE_SIZE;
        dpi = DEFAULT_DPI;
    }

    /**
     * Static class constructor for PDFThumbnailBuilder
     * @see {@link #PDFThumbnailBuilder(File)}
     */
    public static PDFThumbnailBuilder builder(File file) throws IOException {
        return new PDFThumbnailBuilder(file); 
    }

    /**
     * Static class constructor for PDFThumbnailBuilder
     * @see {@link #PDFThumbnailBuilder(byte[])}
     */
    public static PDFThumbnailBuilder builder(byte[] bytes) {
        return new PDFThumbnailBuilder(bytes);
    }

    /**
     * Sets the page to generate an image of the PDF thumbnail
     * @param pageIndex Index of page (0 based) to generate PDF thumbnail from.
     */
    public PDFThumbnailBuilder fromPage(int pageIndex) {
        this.pageIndex = pageIndex;
        return this;
    }

    /**
     * Sets the size of the PDF thumbnail in pixels. Size refers to the width
     * of the thumbnail, to which the height of the thumbnail will be scaled
     * accordingly to retain the image's aspect ratio.
     * @param size Size of the PDF thumbnail, referring to its width.
     */
    public PDFThumbnailBuilder setSize(int size) {
        this.size = size;
        return this;
    }

    /**
     * Sets the dpi of the PDF thumbnail. The higher the dpi, the more time  needed to generate 
     * the thumbnail.
     * @param dpi DPI of the PDF thumbnail
     */
    public PDFThumbnailBuilder setDPI(int dpi) {
        this.dpi = dpi;
        return this;
    }

    /**
     * Generates the PDF thumbnail. Returns a BufferedImage with jpeg image data of the PDF 
     * thumbnail. 
     */
    public BufferedImage build() throws IOException {

        // Try to generate a pdf
        try(PDDocument document = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage thumbnailBI = renderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
            return Scalr.resize(
                thumbnailBI, 
                Scalr.Method.ULTRA_QUALITY, 
                Scalr.Mode.FIT_TO_WIDTH, 
                size
            );        
        } 
    } 
}
