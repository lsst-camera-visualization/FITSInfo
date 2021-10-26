package org.lsst.fits.fitsinfo;

import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import org.lsst.ccs.imagenaming.ImageName;
import org.lsst.ccs.imagenaming.Source;

/**
 *
 * @author tonyj
 */
class NotificationsManager {

    private static NotificationsManager singleton;
    private final SseBroadcaster sseBroadcaster;
    private final OutboundSseEvent.Builder eventBuilder;
    private String site;

    NotificationsManager(Sse sse) {
        this.sseBroadcaster = sse.newBroadcaster();
        this.eventBuilder = sse.newEventBuilder();
    }

    static NotificationsManager connect(Sse sse) {
        if (singleton == null) {
            singleton = new NotificationsManager(sse);
        }
        return singleton;
    }

    void register(String site, SseEventSink sseEventSink) {
        this.site = site;
        sseBroadcaster.register(sseEventSink);
    }

    void notify(ImageName image, Map<String, String> data) {

        if ("auxtel".equalsIgnoreCase(site) && image.getSource() != Source.AuxTel) return;
        if ("comcam".equalsIgnoreCase(site) && image.getSource() != Source.ComCam) return;
        if ("main".equalsIgnoreCase(site) && image.getSource() != Source.MainCamera) return;
        
        OutboundSseEvent sseEvent = this.eventBuilder
                .name("newImage")
                .id(image.toString())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .reconnectDelay(4000)
                .data(data)
                .comment("new image")
                .build();
        sseBroadcaster.broadcast(sseEvent);
    }

}
