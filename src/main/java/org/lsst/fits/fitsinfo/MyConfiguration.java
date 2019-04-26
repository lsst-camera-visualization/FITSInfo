package org.lsst.fits.fitsinfo;


import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author tonyj
 */
@ApplicationPath("/rest")
public final class MyConfiguration extends ResourceConfig {

    public MyConfiguration() {
        register(FitsService.class);
        register(JacksonFeature.class);
        register(CORSResponseFilter.class);
    }
}