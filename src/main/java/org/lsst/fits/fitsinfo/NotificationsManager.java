package org.lsst.fits.fitsinfo;

import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import org.lsst.ccs.imagenaming.ImageName;

/**
 *
 * @author tonyj
 */
class NotificationsManager {

    private static NotificationsManager singleton;
    private final SseBroadcaster sseBroadcaster;
    private final OutboundSseEvent.Builder eventBuilder;

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

    void register(SseEventSink sseEventSink) {
        sseBroadcaster.register(sseEventSink);
    }

    void notify(ImageName image, Map<String, String> data) {

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
