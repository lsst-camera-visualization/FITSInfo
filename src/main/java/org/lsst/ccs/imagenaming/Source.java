package org.lsst.ccs.imagenaming;

/**
 * An enumeration of possible image sources.
 */
public enum Source {
    MainCamera("MC"), ComCam("CC"), AuxTel("AT"), TestStand("TS");
    private final String code;

    private Source(String code) {
        this.code = code;
    }

    /**
     * The two-letter code for the image source
     *
     * @return The code.
     */
    public String getCode() {
        return code;
    }

    static Source fromCode(String code) {
        for (Source s : Source.values()) {
            if (s.getCode().equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid source code: " + code);
    }
    
}
