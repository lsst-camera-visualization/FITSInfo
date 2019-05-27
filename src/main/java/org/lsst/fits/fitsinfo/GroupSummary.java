package org.lsst.fits.fitsinfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tonyj
 */
class GroupSummary {

    private final List<GroupSummaryEntry> summaryList = new ArrayList<>();
    
    public static GroupSummary fromObjects(List<Map<String, Object>> input) {
        if (input.isEmpty()) {
            return null;
        } else {
            GroupSummary groupSummary = new GroupSummary();
            input.forEach((o) -> {
                groupSummary.summaryList.add(new GroupSummary.GroupSummaryEntry((String) o.get("selector"), (String) o.get("summaryType")));
            });
            return groupSummary;
        }
    }    

    @Override
    public String toString() {
        return "GroupSummary{" + "summaryList=" + summaryList + '}';
    }
    
    
    
    private static class GroupSummaryEntry {
        private final String selector;
        private final String type;

        public GroupSummaryEntry(String selector, String type) {
            this.selector = selector;
            this.type = type;
        }

        @Override
        public String toString() {
            return "GroupSummaryEntry{" + "selector=" + selector + ", type=" + type + '}';
        }
        
    }
}
