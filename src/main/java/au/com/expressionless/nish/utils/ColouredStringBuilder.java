package au.com.expressionless.nish.utils;

import org.apache.commons.lang3.StringUtils;

public class ColouredStringBuilder  {
    StringBuilder sb;

    public ColouredStringBuilder() {
        this(null, null);
    }

    public ColouredStringBuilder(String initial) {
        this(initial, null);
    }

    public ColouredStringBuilder(String initial, ANSIColour colour) {
        sb = new StringBuilder();
        if(!StringUtils.isBlank(initial)) {
            if(colour != null) {
                sb.append(ANSIColour.doColour(initial, colour));
            } else {
                sb.append(initial);
            }
        }
    }

    public ColouredStringBuilder append(String text) {
        return append(text, null);
    }

    public ColouredStringBuilder append(String text, ANSIColour colour) {
        text = colour != null ? ANSIColour.doColour(text, colour) : text;
        sb.append(text);
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    public String toString(ANSIColour colour) {
        return ANSIColour.doColour(toString(), colour);
    }
}
