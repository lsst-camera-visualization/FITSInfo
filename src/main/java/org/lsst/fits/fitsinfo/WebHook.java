package org.lsst.fits.fitsinfo;

import java.util.Collections;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.Sse;
import org.lsst.ccs.imagenaming.ImageName;

/**
 * Webhook called to notify application that a new image is available.
 *
 * @author tonyj
 */
@Path("webhook")
public class WebHook {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response webhook(@Context Sse sse, Map<String, String> data) {
        try {
            ImageName image = new ImageName(data.get("image"));
            NotificationsManager.connect(sse).notify(image, data);
            return Response.ok().build();
        } catch (RuntimeException x) {
            return Response.serverError().entity(Collections.singletonMap("error", x.getMessage())).build();
        }
    }
}
