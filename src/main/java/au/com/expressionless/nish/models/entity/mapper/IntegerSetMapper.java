package au.com.expressionless.nish.models.entity.mapper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;

import au.com.expressionless.nish.constants.StringConstants;

/**
 * Map a database column containing a string of comma-delimited integers to an array of integers
 * This does not cater for the case: [1,2,3] or [] (for that matter any cases with array start, end)
 * This mapper is intended to be used exclusively for cases where the string is only comma-delimited.
 * <pre>
 * // Examples
 * "1,2,3"
 * "1,2"
 * "1"
 * ""
 * </pre>
 */
public class IntegerSetMapper implements AttributeConverter<Set<Integer>, String> {

    /**
     * helper method to keep set type consistent
     */
    public Set<Integer> set() {
        return new TreeSet<>();
    }

    /**
     * Convert from an integer array to a comma-delimited string
     * @param attribute integer array to convert
     * @return the comma-delimited string representative of the integer array
     */
    @Override
    public String convertToDatabaseColumn(Set<Integer> attribute) {
        if(attribute == null || attribute.isEmpty()) {
            return StringConstants.STR_BLANK;
        }

        StringBuilder sb = new StringBuilder();
        Iterator<Integer> iter = attribute.iterator();

        while(iter.hasNext()) {
            Integer pageNum = iter.next();
            sb.append(pageNum);

            // only add comma if there is another element to go
            if(iter.hasNext()) {
                sb.append(StringConstants.COMMA);
            }
        }

        return sb.toString();
    }


    /**
     * Convert from a comma-delimited string to an integer array
     * @param attribute the comma-delimited string to convert
     * @return the integer array representative of the comma-delimited string
     */
    @Override
    public Set<Integer> convertToEntityAttribute(String dbData) {
        if(StringUtils.isBlank(dbData) || StringConstants.NULL.equals(dbData)) {
            return set();
        }

        String[] splits = dbData.split(StringConstants.COMMA);
        Set<Integer> nums = set();
        for(int i = 0; i < splits.length; i++) {
            String s = splits[i];
            nums.add(Integer.parseInt(s));
        }

        return nums;
    }
    
}
