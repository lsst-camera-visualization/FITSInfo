package org.lsst.fits.fitsinfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.lsst.ccs.imagenaming.ImageName;
import org.lsst.fits.dao.Image;
import org.lsst.fits.dao.ImageDAO;

/**
 *
 * @author tonyj
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class FitsDataSource {

    private final ImageDAO dao = new ImageDAO();

    @GET
    @Path("/images")
    public Object images(
            @DefaultValue("0") @QueryParam(value = "skip") int skip,
            @DefaultValue("0") @QueryParam(value = "take") int take,
            @DefaultValue("false") @QueryParam(value = "requireTotalCount") boolean requireTotalCount,
            @DefaultValue("false") @QueryParam(value = "requireGroupCount") boolean requireGroupCount,
            @DefaultValue("[]") @QueryParam(value = "group") Group groups,
            @DefaultValue("[]") @QueryParam(value= "sort") Sort sort,
            @DefaultValue("[]") @QueryParam(value= "filter") Filter filter,
            @DefaultValue("[]") @QueryParam(value= "groupSummary") GroupSummary groupSummary) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        if (groups != null) {
            List groupData = new ArrayList();
            for (Group.GroupEntry group : groups.getEntries()) {
                final String selector = group.getSelector();
                List<Object> resultSet = dao.getImageGroup(selector, group.isDesc(), skip, take, filter);
                for (Object rs : resultSet) {
                    Map<String, Object> groupResult = new LinkedHashMap<>();
                    Object[] rsa = (Object[]) rs;
                    groupResult.put("key", rsa[0]);
                    if (group.isExpanded()) {
                        throw new UnsupportedOperationException("group isExpanded not implemented yet");
                    } else {
                        groupResult.put("items", null);
                        groupResult.put("count", rsa[1]);
                    }
                    groupData.add(groupResult);
                    if (groupSummary != null) {
                        groupResult.put("summary", new Long[]{(Long)rsa[1]});
                    }
                }
                if (requireGroupCount) {
                    result.put("groupCount", dao.getImageGroupCount(selector, filter));
                }
            }
            result.put("data", groupData);
        } else {
            final List<Image> images = dao.getImages(skip, take, filter, sort);
            result.put("data", images);
        }
        if (requireTotalCount) {
            result.put("totalCount", dao.getTotalImageCount(filter));
        }

        return result;
    }

    @GET
    @Path("/image/{id}")
    public Image image(@PathParam(value = "id") String id) {
        return dao.getImage(new ImageName(id));
    }

    @GET
    @Path("/imageInfo/{id}")
    public Map<String, Object> imageInfo(@PathParam(value = "id") String id) {
        Image image;
        ImageName in;
        if ("latest".equalsIgnoreCase(id)) {
            image = dao.getLatestImage();
            in = new ImageName(image.getObsId());
        } else {
            in = new ImageName(id);
            image = dao.getImage(in);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("image", image);
        result.put("next", dao.getNextImage(in));
        result.put("previous", dao.getPreviousImage(in));
        return result;
    }
}
