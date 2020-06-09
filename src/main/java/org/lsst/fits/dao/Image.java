package org.lsst.fits.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import org.hibernate.annotations.Formula;

/**
 *
 * @author tonyj
 */
@Entity
@Table(name="ccs_image")
public class Image implements Serializable {
    
    @Id
    private String telCode;
    @Id
    private String controller;
    @Id
    private String dayobs;
    @Id
    private int seqnum;

    private String imgType;
    private String testType;
    private String runNumber;
    private Integer tseqnum;
    private String tstand;
    private String fileLocation;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date obsDate;
    private int raftMask;
    
    @Formula("concat(telcode,'_',controller,'_',dayobs,'_',lpad(seqnum,6,'0'))")
    private String obsId;

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.telCode);
        hash = 89 * hash + Objects.hashCode(this.controller);
        hash = 89 * hash + Objects.hashCode(this.dayobs);
        hash = 89 * hash + this.seqnum;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Image other = (Image) obj;
        if (this.seqnum != other.seqnum) {
            return false;
        }
        if (!Objects.equals(this.telCode, other.telCode)) {
            return false;
        }
        if (!Objects.equals(this.controller, other.controller)) {
            return false;
        }
        return Objects.equals(this.dayobs, other.dayobs);
    }

    public String getTelCode() {
        return telCode;
    }

    public String getController() {
        return controller;
    }

    public String getDayobs() {
        return dayobs;
    }

    public int getSeqnum() {
        return seqnum;
    }

    public String getImgType() {
        return imgType;
    }

    public String getTestType() {
        return testType;
    }

    public String getRunNumber() {
        return runNumber;
    }

    public int getTseqnum() {
        return tseqnum == null ? 0 : tseqnum;
    }

    public String getTstand() {
        return tstand;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public Date getObsDate() {
        return obsDate;
    }

    public int getRaftMask() {
        return raftMask;
    }
    
    public String getObsId() {
        return obsId;
    }

    @Override
    public String toString() {
        return "Image{" + "telCode=" + telCode + ", controller=" + controller + ", dayobs=" + dayobs + ", seqnum=" + seqnum + ", imgType=" + imgType + ", testType=" + testType + ", runNumber=" + runNumber + ", tseqnum=" + tseqnum + ", tstand=" + tstand + ", fileLocation=" + fileLocation + ", obsDate=" + obsDate + ", raftMask=" + raftMask + '}';
    }
}
