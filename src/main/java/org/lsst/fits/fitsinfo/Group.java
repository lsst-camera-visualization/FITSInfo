package org.lsst.fits.fitsinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tonyj
 */
public class Group {

    private final List<Group.GroupEntry> groupList = new ArrayList<>();

    public static Group fromObjects(List<Map<String, Object>> input) {
        if (input.isEmpty()) {
            return null;
        } else {
            Group group = new Group();
            input.forEach((o) -> {
                group.groupList.add(new Group.GroupEntry((String) o.get("selector"), (boolean) o.get("desc"), (boolean) o.get("isExpanded")));
            });
            return group;
        }
    }

    public static Group fromString(String input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> readValue = mapper.readValue(input,List.class);        
        return Group.fromObjects(readValue);
    }
    
    @Override
    public String toString() {
        return "Group{" + "groupList=" + groupList + '}';
    }

    Iterable<GroupEntry> getEntries() {
        return groupList;
    }

    static class GroupEntry {

        private final String selector;
        private final boolean desc;
        private final boolean isExpanded;

        public GroupEntry(String selector, boolean desc, boolean isExpanded) {
            this.selector = selector;
            this.desc = desc;
            this.isExpanded = isExpanded;
        }

        public String getSelector() {
            return selector;
        }

        public boolean isDesc() {
            return desc;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        @Override
        public String toString() {
            return "Group{" + "selector=" + selector + ", desc=" + desc + ", isExpanded=" + isExpanded + '}';
        }
    }
}
