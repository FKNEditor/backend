package au.com.expressionless.nish.utils;

/**
 * A Class for storing ANSI text colours 
 * used in logging, or anything else.
 */
public enum ANSIColour {
    RED("\033[0;31m"),
    BLACK("\033[0;30m"),
    GREEN("\033[0;32m"),
    // THIS IS ACTUALLY ORANGE!!!
    YELLOW("\033[0;33m"),
    BLUE("\033[0;34m"),
    PURPLE("\033[0;35m"),
    CYAN("\033[0;36m"),
    WHITE("\033[0;37m"),
    RESET("\033[0m");

    private String colour;

    private ANSIColour(String colour) {
        this.colour = colour;
    }


    public static final String doGreen(Object msg) {
        return ANSIColour.doColour(msg, ANSIColour.GREEN);
    }

    public static final String doYellow(Object msg) {
        return ANSIColour.doColour(msg, ANSIColour.YELLOW);
    }

    public static final String doCyan(Object msg) {
        return ANSIColour.doColour(msg, ANSIColour.CYAN);
    }

    public static final String doRed(Object msg) {
        return ANSIColour.doColour(msg, ANSIColour.RED);
    }

    public static final String doPurple(Object msg) {
        return ANSIColour.doColour(msg, ANSIColour.PURPLE);
    }

    public static final String doColour(Object msg, ANSIColour colour) {
        return colour.colour + msg + ANSIColour.RESET.colour;
    }

    public static final String strip(String text) {
        for(ANSIColour col : values()) {
            text = text.replace(col.colour, "");
        }
        return text;
    }

    public String getColour() {
        return colour;
    }

    public static final String doColour(String text, String colourCode) {
        return colourCode + text + ANSIColour.RESET;
    }

}
