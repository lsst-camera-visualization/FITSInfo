package org.lsst.fits.fitsinfo;

/**
 *
 * @author tonyj
 */
public class Group {
    private String selector;
    private boolean desc;
    private boolean isExpanded;
    
    public Group() {
    }
    
    public Group(String selector, boolean desc, boolean isExpanded) {
        this.selector = selector;
        this.desc = desc;
        this.isExpanded = isExpanded;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public boolean isDesc() {
        return desc;
    }

    public void setDesc(boolean desc) {
        this.desc = desc;
    }

    public boolean isIsExpanded() {
        return isExpanded;
    }

    public void setIsExpanded(boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    @Override
    public String toString() {
        return "Group{" + "selector=" + selector + ", desc=" + desc + ", isExpanded=" + isExpanded + '}';
    }
    
}
