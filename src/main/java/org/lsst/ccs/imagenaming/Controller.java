package org.lsst.ccs.imagenaming;

/**
 * An enumeration of possible image name controllers.
 */
public enum Controller {
    OCS("O"), CCS("C"), AuxCCS("A"), Simulation("S"), Playback("P"), DAQ("D");
    private final String code;

    private Controller(String code) {
        this.code = code;
    }

    /**
     * The one-letter code from the controller
     *
     * @return The code
     */
    public String getCode() {
        return code;
    }

    static Controller fromCode(String code) {
        for (Controller s : Controller.values()) {
            if (s.getCode().equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid controller code: " + code);
    }
    
}
