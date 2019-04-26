package org.lsst.ccs.imagenaming;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing an image name in the standard format.
 * @author tonyj
 */
public class ImageName implements Serializable, Comparable<ImageName> {

    private final static Pattern namePattern = Pattern.compile("(\\w\\w)_(\\w)_(\\d{8})_(\\d+)");
    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long serialVersionUID = 8767898207051188380L;
    private final Source source;
    private final Controller controller;
    private final LocalDate date;
    private final int number;

    /**
     * Create an image name from a string in the standard image name format.
     *
     * @param name The string to parse
     * @throws IllegalArgumentException if the string is not a valid image name
     */
    public ImageName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Invalid null image name ");
        }
        Matcher matcher = namePattern.matcher(name);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid image name " + name);
        }
        source = Source.fromCode(matcher.group(1));
        controller = Controller.fromCode(matcher.group(2));
        number = Integer.parseInt(matcher.group(4));
        if (number <= 0) {
            throw new IllegalArgumentException("Image number must be > 0");
        } 
        try {
            date = LocalDate.parse(matcher.group(3), formatter);
        } catch (DateTimeParseException x) {
            throw new IllegalArgumentException("Invalid date field", x);
        }
    }

    ImageName(Source source, Controller controller, LocalDate date, int imageNumber) {
        this.source = source;
        this.controller = controller;
        this.date = date;
        this.number = imageNumber;
    }

    /**
     * Get the source of the image
     * @return The source
     */
    public Source getSource() {
        return source;
    }

    /**
     * Get the controller that initiated the image
     * @return The controller
     */
    public Controller getController() {
        return controller;
    }

    /**
     * Get the data of the image. Note that this is the date allocated
     * using LSST naming conventions, and not necessarily the actual date
     * on which the image was taken.
     * @return The date
     */
    public LocalDate getDate() {
        return date;
    }
    
    /**
     * Get the formatted date of the image
     * @return The date formatted in the standard way
     */
    public String getDateString() {
        return formatter.format(date);
    }

    /**
     * Get the image sequence number
     * @return The number
     */
    public int getNumber() {
        return number;
    }

    /**
     * Get the formatted image number
     * @return The sequence number formatted in the standard way
     */
    public String getNumberString() {
        return String.format("%06d", number);
    }
    
    @Override
    public String toString() {
        return String.format("%s_%s_%s_%06d", source.getCode(), controller.getCode(), formatter.format(date), number);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.source);
        hash = 97 * hash + Objects.hashCode(this.controller);
        hash = 97 * hash + Objects.hashCode(this.date);
        hash = 97 * hash + this.number;
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
        final ImageName other = (ImageName) obj;
        if (this.number != other.number) {
            return false;
        }
        if (this.source != other.source) {
            return false;
        }
        if (this.controller != other.controller) {
            return false;
        }
        return Objects.equals(this.date, other.date);
    }

    @Override
    public int compareTo(ImageName other) {
        return this.toString().compareTo(other.toString());
    }
}
