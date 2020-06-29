package org.lsst.fits.dao;

import java.sql.Timestamp;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 *
 * @author tonyj
 */
@StaticMetamodel(Image.class)
public class Image_ {

    public static volatile SingularAttribute<Image, String> telCode;
    public static volatile SingularAttribute<Image, String> controller;
    public static volatile SingularAttribute<Image, String> dayobs;
    public static volatile SingularAttribute<Image, Integer> seqnum;

    public static volatile SingularAttribute<Image, String> imgType;
    public static volatile SingularAttribute<Image, String> testType;
    public static volatile SingularAttribute<Image, String> runNumber;
    public static volatile SingularAttribute<Image, Integer> tseqnum;
    public static volatile SingularAttribute<Image, String> tstand;
    public static volatile SingularAttribute<Image, String> fileLocation;
    
    public static volatile SingularAttribute<Image, Timestamp> obsDate;
    public static volatile SingularAttribute<Image, Integer> raftMask;   
    public static volatile SingularAttribute<Image, String> obsId;
}
