package au.com.expressionless.nish.models.entity.mapper;

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
public class IntegerListMapper implements AttributeConverter<Integer[], String> {

    /**
     * Convert from an integer array to a comma-delimited string
     * @param attribute integer array to convert
     * @return the comma-delimited string representative of the integer array
     */
    @Override
    public String convertToDatabaseColumn(Integer[] attribute) {
        if(attribute == null || attribute.length == 0) {
            return StringConstants.STR_BLANK;
        }

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < attribute.length - 1; i++) {
            Integer pageNum = attribute[i];
            sb.append(pageNum).append(StringConstants.COMMA);
        }

        return sb.toString();
    }


    /**
     * Convert from a comma-delimited string to an integer array
     * @param attribute the comma-delimited string to convert
     * @return the integer array representative of the comma-delimited string
     */
    @Override
    public Integer[] convertToEntityAttribute(String dbData) {
        if(StringUtils.isBlank(dbData)) {
            return new Integer[] {};
        }

        String[] splits = dbData.split(StringConstants.COMMA);
        Integer[] nums = new Integer[splits.length];
        for(int i = 0; i < splits.length; i++) {
            String s = splits[i];
            nums[i] = Integer.parseInt(s);
        }

        return nums;
    }
    
}
