package org.lsst.fits.dao;

import java.util.List;
import javax.ws.rs.GET;

/**
 *
 * @author tonyj
 */
public class Main {

    public static void main(String[] args) {
        ImageDAO dao = new ImageDAO();
        List<Object> imageGroup = (List<Object>) dao.getImageGroup("runNumber", false, 0, 20);
        System.out.println(imageGroup);
        for (Object o : imageGroup) {
            Object[] oo = (Object[]) o;
            System.out.println(oo[0]+","+oo[1]);
        }
        
        System.out.println(dao.getImageGroupCount("runNumber"));
    }
}
