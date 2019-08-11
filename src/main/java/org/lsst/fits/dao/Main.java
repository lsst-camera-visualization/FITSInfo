package org.lsst.fits.dao;

import java.util.List;
import org.lsst.ccs.imagenaming.ImageName;

/**
 *
 * @author tonyj
 */
public class Main {

    public static void main(String[] args) {
        ImageDAO dao = new ImageDAO();
        List<Object> imageGroup = (List<Object>) dao.getImageGroup("runNumber", false, 0, 20, null);
        System.out.println(imageGroup);
        for (Object o : imageGroup) {
            Object[] oo = (Object[]) o;
            System.out.println(oo[0]+","+oo[1]);
        }
        
        System.out.println(dao.getImageGroupCount("runNumber", null));
        System.out.println(dao.getTotalImageCount(null));
        
        Image image = dao.getImage(new ImageName("CC_C_20190527_000888"));
        System.out.println(image);
        System.out.println(image.getObsId());
        System.out.println(image.getObsDate().getTime());
    }
}
