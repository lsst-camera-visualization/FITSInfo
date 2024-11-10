package org.lsst.fits.fitsinfo;


import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.lsst.ccs.fits.headers.restdatasource.FitsHeaderDataSource;

/**
 *
 * @author tonyj
 */
@ApplicationPath("/rest")
public final class MyConfiguration extends ResourceConfig {

    public MyConfiguration() {
        register(FitsDataSource.class);
        register(JacksonFeature.class);
        register(CORSResponseFilter.class);
        register(WebHook.class);
        register(EventSender.class);
        register(FitsHeaderDataSource.class);
    }
}