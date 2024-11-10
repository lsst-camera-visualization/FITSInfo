package org.lsst.ccs.fits.headers.restdatasource;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.lsst.ccs.imagenaming.ImageName;

/**
 *
 * @author tonyj
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class FitsHeaderDataSource {
    
    @GET
    @Path("/{site}/headers/{id}")
    public Object headers(@PathParam(value = "site") String siteName, @PathParam(value = "id") String id) throws IOException {
        ImageName imageName = new ImageName(id);
        java.nio.file.Path path = java.nio.file.Paths.get("/home/tonyj/Data/");
        JsonReader reader = new JsonReader(path);
        return reader.getComponents(imageName);
    }

    @GET
    @Path("/{site}/headers/{id}/{component}")
    public Object headers(@PathParam(value = "site") String siteName, @PathParam(value = "id") String id, @PathParam(value = "component") String component) throws IOException {
        ImageName imageName = new ImageName(id);
        java.nio.file.Path path = java.nio.file.Paths.get("/home/tonyj/Data/");
        JsonReader reader = new JsonReader(path);
        return reader.getHeadersForComponent(imageName, component);
    }
    
}
