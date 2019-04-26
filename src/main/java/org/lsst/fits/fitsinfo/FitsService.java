package org.lsst.fits.fitsinfo;

import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.lsst.fits.dao.Image;
import org.lsst.fits.dao.ImageDAO;
import org.lsst.fits.dao.TablePage;

/**
 *
 * @author tonyj
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class FitsService {

    private final ImageDAO dao = new ImageDAO();

    @GET
    @Path("/images")
    public List<Image> images() {
        return dao.getImages(0, 0, null).getItems();
    }

    /**
     *
     * @param skip
     * @param take
     * @param orderBy
     * @return
     */
    @GET
    @Path("/imageTable")
    public TablePage<Image> imageTable(
            @DefaultValue("0") @QueryParam(value = "skip") int skip,
            @DefaultValue("0") @QueryParam(value = "take") int take,
            @QueryParam(value = "orderby") String orderBy) {

        return dao.getImages(skip, take, orderBy);
    }

    @GET
    @Path("/image/{id}")
    public Image image(@PathParam(value = "id") String id) {
        return dao.getImage(id);
    }
}
