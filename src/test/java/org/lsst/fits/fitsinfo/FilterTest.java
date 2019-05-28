package org.lsst.fits.fitsinfo;

import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author tonyj
 */
public class FilterTest {

    @Test
    public void testEmpty() {
        List<Object> input = Arrays.asList();
        Filter result = Filter.fromObjects(input);
        assertNull(result);
    }

    @Test
    public void testBinary() {
        List<Object> input = Arrays.asList("field", "=", 3);
        Filter result = Filter.fromObjects(input);
        assertTrue(result instanceof Filter.SimpleFilter);
    }

    @Test
    public void testUnary() {
        List<Object> input = Arrays.asList("!", Arrays.asList("field", "=", 3));
        Filter result = Filter.fromObjects(input);
        assertTrue(result instanceof Filter.UnaryFilter);
    }

    @Test
    public void testComplex() {
        List<Object> input = Arrays.asList(
                Arrays.asList("field", "=", 3),
                "and",
                Arrays.asList(
                        Arrays.asList("otherfield", "<", 3),
                        "or",
                        Arrays.asList("otherfield", ">", 11)
                ));
        Filter result = Filter.fromObjects(input);
        assertTrue(result instanceof Filter.ComplexFilter);
    }

    @Test
    public void testComplex2() {
        List<Object> input = Arrays.asList(
                Arrays.asList("runNumber", "=", "6473D"),
                "or",
                Arrays.asList("runNumber", "=", "6474D"),
                "or",
                Arrays.asList("runNumber", "=", "6476D")
        );
        Filter result = Filter.fromObjects(input);
        assertTrue(result instanceof Filter.ComplexFilter);
    }
}
