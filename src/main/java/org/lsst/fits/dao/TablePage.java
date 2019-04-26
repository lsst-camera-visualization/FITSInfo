package org.lsst.fits.dao;

import java.util.List;

/**
 *
 * @author tonyj
 * @param <T>
 */
public class TablePage<T> {
    private final long totalCount;
    private final List<T> items;

    public TablePage(long totalCount, List<T> items) {
        this.totalCount = totalCount;
        this.items = items;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public List<T> getItems() {
        return items;
    }
}
