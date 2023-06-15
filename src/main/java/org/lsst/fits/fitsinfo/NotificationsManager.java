package org.lsst.fits.fitsinfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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

/* Some hints in case this appears to not be working:
 * To test at the summit try:
 *   curl -N http://ccs.lsst.org/FITSInfo/rest/auxtel/notify
 *   curl -X POST -H "Content-Type: application/json" -d '{"image": "AT_C_20230613_000001"}' http://ccs.lsst.org/FITSInfo/rest/webhook
 *  
 * To test at SLAC try:
 *   curl -N https://lsst-camera-dev.slac.stanford.edu/FITSInfo/rest/maincamera/notify
 *   curl -X POST -H "Content-Type: application/json" -d '{"image": "MC_C_20230613_000001"}' https://lsst-camera-dev.slac.stanford.edu/FITSInfo/rest/webhook
 * 
 * To test bypassing nginx try:
 *    curl -N http://lsstlnx-v02:8180/FITSInfo/rest/maincamera/notify
 * 
 * Note that nginx will block SSE events unless it is configured appropriately, suggestion seems to be:
 *      proxy_set_header Connection '';
 *      proxy_http_version 1.1;
 *      proxy_buffering off;
 *      proxy_cache off;
 *      chunked_transfer_encoding off;
 * 
*/
class NotificationsManager {

    private static final Logger LOG = Logger.getLogger(NotificationsManager.class.getName());
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
        LOG.log(Level.INFO, "Register {0}", source);
        sourceBroadcaster.putIfAbsent(source, sse.newBroadcaster());
        sourceBroadcaster.get(source).register(sseEventSink);
    }

    void notify(ImageName image, Map<String, String> data) {

        SseBroadcaster broadCaster = sourceBroadcaster.get(image.getSource());
        LOG.log(Level.INFO, "Notify {0} with data {1} to {2}", new Object[]{image, data, broadCaster});
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
