package org.lsst.fits.fitsinfo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import org.lsst.ccs.imagenaming.Source;

/**
 *
 * @author tonyj
 */
@Path("{site}/notify")
public class EventSender {


    @GET
    @Produces("text/event-stream")
    public void getImageNotifications(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam(value = "site") String siteName) {
        Source source;
        if ("auxtel".equalsIgnoreCase(siteName)) source =  Source.AuxTel;
        else if ("comcam".equalsIgnoreCase(siteName)) source =  Source.ComCam;
        else if ("main".equalsIgnoreCase(siteName)) source =  Source.MainCamera;
        else if ("maincamera".equalsIgnoreCase(siteName)) source =  Source.MainCamera;
        else {
            source = Source.valueOf(siteName);
        }
        NotificationsManager.connect(sse).register(source, sseEventSink);
    }
}
