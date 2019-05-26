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
import org.lsst.fits.dao.TablePage;

/**
 *
 * @author tonyj
 */
@Path("/dataSource")
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
            @QueryParam(value = "group") List<Map> groups) {
        Map<String, Object> result = new LinkedHashMap<>();
        final TablePage<Image> images = dao.getImages(0, 0, null);
        System.out.println("group="+groups);
//        System.out.println("filter="+filter);
//        System.out.println("sort="+sort);
        
        if (!groups.isEmpty()) {
            List groupData = new ArrayList();
            for (Map group : groups) {
                final String selector = (String) group.get("selector");
                List<Object> resultSet = dao.getImageGroup(selector, (boolean) group.get("desc"), skip, take);
                for (Object rs : resultSet) {
                    Map<String, Object> groupResult = new LinkedHashMap<>();
                    Object[] rsa = (Object[]) rs;
                    groupResult.put("key", rsa[0]);
                    if ((Boolean) group.get("isExpanded")) {
                        groupResult.put("items", null);
                    } else {
                        groupResult.put("items", null);
                        groupResult.put("count", rsa[1]);
                    }
                    groupData.add(groupResult);
                }
                if (requireGroupCount) {
                    result.put("groupCount", dao.getImageGroupCount(selector));
                }
            }
            result.put("data", groupData);
        } else {
            result.put("data", images.getItems());
        }
        if (requireTotalCount) {
            result.put("totalCount", images.getTotalCount());
        }

        return result;
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
        return dao.getImage(new ImageName(id));
    }

    @GET
    @Path("/imageInfo/{id}")
    public Map<String, Object> imageInfo(@PathParam(value = "id") String id) {
        ImageName in = new ImageName(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("image", dao.getImage(in));
        result.put("next", dao.getNextImage(in));
        result.put("previous", dao.getPreviousImage(in));
        return result;
    }
}
