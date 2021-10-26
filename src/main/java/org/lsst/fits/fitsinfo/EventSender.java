package org.lsst.fits.fitsinfo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

/**
 *
 * @author tonyj
 */
@Path("{site}/notify")
public class EventSender {


    @GET
    @Produces("text/event-stream")
    public void getImageNotifications(@Context Sse sse, @Context SseEventSink sseEventSink, @PathParam(value = "site") String siteName) {
        NotificationsManager.connect(sse).register(siteName, sseEventSink);
    }
}
