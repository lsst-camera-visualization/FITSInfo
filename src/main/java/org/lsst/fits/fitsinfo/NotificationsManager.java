package org.lsst.fits.fitsinfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;
import org.lsst.ccs.imagenaming.ImageName;
import org.lsst.ccs.imagenaming.Source;

/**
 * A singleton class which routes new image notifications to clients using SSE
 * to receive notifications of new images.
 *
 * @author tonyj
 */
class NotificationsManager {

    private static NotificationsManager singleton;
    private final OutboundSseEvent.Builder eventBuilder;
    private final Map<Source, SseBroadcaster> sourceBroadcaster = new ConcurrentHashMap<>();
    private final Sse sse;

    private NotificationsManager(Sse sse) {
        this.sse = sse;
        this.eventBuilder = sse.newEventBuilder();
    }

    static NotificationsManager connect(Sse sse) {
        if (singleton == null) {
            singleton = new NotificationsManager(sse);
        }
        return singleton;
    }

    void register(Source source, SseEventSink sseEventSink) {
        sourceBroadcaster.putIfAbsent(source, sse.newBroadcaster());
        sourceBroadcaster.get(source).register(sseEventSink);
    }

    void notify(ImageName image, Map<String, String> data) {

        SseBroadcaster broadCaster = sourceBroadcaster.get(image.getSource());
        if (broadCaster != null) {
            OutboundSseEvent sseEvent = this.eventBuilder
                    .name("newImage")
                    .id(image.toString())
                    .mediaType(MediaType.APPLICATION_JSON_TYPE)
                    .reconnectDelay(4000)
                    .data(data)
                    .comment("new image")
                    .build();
            broadCaster.broadcast(sseEvent);
        }
    }

}
