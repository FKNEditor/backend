package au.com.expressionless.nish.models.entity.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import au.com.expressionless.nish.constants.StringConstants;
import au.com.expressionless.nish.exception.BadDataException;

public class KeywordEditionMapper implements AttributeConverter<List<Map.Entry<Double, Long>>, String> {
    private static final Logger log = Logger.getLogger(KeywordEditionMapper.class);

    public static final String DELIMITER = ":";
    static Jsonb jsonb = JsonbBuilder.create();

    private String elementToString(Entry<Double, Long> element) {
        return new StringBuilder().append(element.getValue()).append(DELIMITER).append(element.getKey()).toString();
    }

    // this can be optimised (rather not though)
    private Map.Entry<Double, Long> fromString(String attribute) {
        if(StringUtils.isBlank(attribute))
            return null;
        String[] splits = attribute.split(DELIMITER);
        if(splits.length == 1)
            throw new BadDataException(attribute + ", expected: a \"" + DELIMITER + "\" delimited string");
        double dub = 0.0;
        try {
            dub = Double.parseDouble(splits[1]);
        } catch (NumberFormatException e) {
            log.error("Error parsing value to ratio: " + splits[1]);
            throw e;
        }

        try {
            long editionId = Long.parseLong(splits[0]);
            return Map.entry(dub, editionId);
        } catch(NumberFormatException e) {
            log.error("Error parsing value to id: " + splits[0]);
            throw e;
        }
    }

    @Override
    public String convertToDatabaseColumn(List<Entry<Double, Long>> attribute) {
        StringBuilder str = new StringBuilder();
        if(attribute == null || attribute.isEmpty()) {
            return StringConstants.STR_BLANK;
        }

        int i;
        for(i = 0; i < attribute.size() - 1; i++) {
            Map.Entry<Double, Long> edition = attribute.get(i);
            str.append(elementToString(edition)).append(",");
        }

        return str.append(elementToString(attribute.get(i))).toString();
    }

    @Override
    public List<Entry<Double, Long>> convertToEntityAttribute(String dbData) {
        List<Entry<Double, Long>> editions = new ArrayList<>();
        if(StringUtils.isBlank(dbData)) {
            return editions;
        }

        String[] splits = dbData.split(",");

        for(String tuple : splits) {
            editions.add(fromString(tuple));
        }

        return editions;
    }
    
}
