package org.lsst.fits.fitsinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import org.lsst.fits.dao.Image;

/**
 *
 * @author tonyj
 */
public class Sort {

    private final List<SortEntry> sortList = new ArrayList<>();

    public static Sort fromObjects(List<Map<String, Object>> input) {
        if (input.isEmpty()) {
            return null;
        } else {
            Sort sort = new Sort();
            input.forEach((o) -> {
                sort.sortList.add(new SortEntry((String) o.get("selector"), (boolean) o.get("desc")));
            });
            return sort;
        }
    }
    
    public static Sort fromString(String input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> readValue = mapper.readValue(input,List.class);        
        return Sort.fromObjects(readValue);
    }

    @Override
    public String toString() {
        return "Sort{" + "sortList=" + sortList + '}';
    }

    public Order[] buildQuery(CriteriaBuilder builder, Root<Image> root) {
        Order[] result = new Order[sortList.size()];
        int i = 0;
        for (SortEntry se : sortList) {
            if (se.desc) {
                result[i++] = builder.desc(root.get(se.selector));
            } else {
                result[i++] = builder.asc(root.get(se.selector));
            }
        }
        return result;
    }

    private static class SortEntry {

        private final String selector;
        private final boolean desc;

        public SortEntry(String selector, boolean desc) {
            this.selector = selector;
            this.desc = desc;
        }

        @Override
        public String toString() {
            return "SortEntry{" + "selector=" + selector + ", desc=" + desc + '}';
        }
    }
}
