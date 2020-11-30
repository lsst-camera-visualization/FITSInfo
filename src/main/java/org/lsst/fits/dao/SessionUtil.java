package org.lsst.fits.dao;

import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class SessionUtil {

    private final static ConcurrentHashMap<String, SessionFactory> sessionFactories = new ConcurrentHashMap<>();

    public static Session getSession(String name) {
        SessionFactory sessionFactory = sessionFactories.computeIfAbsent(name, (k) -> {
            Configuration configuration = new Configuration();
            configuration.configure(k+".cfg.xml");
            return configuration.buildSessionFactory();
        });
        Session session = sessionFactory.openSession();
        session.setDefaultReadOnly(true);
        return session;
    }
}
