package org.lsst.ccs.imagenaming;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.TimeZone;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility for assigning image names according to LSST observatory naming
 * conventions.
 *
 * @author tonyj
 */
public class ImageNameAssigner implements AutoCloseable {

    private final static int MAX_RETRIES = 1;
    private Connection conn;
    private PreparedStatement stmt1;
    private PreparedStatement stmt2;
    private final String dbURL;

    /**
     * Create an instant of the image name assignment class.
     *
     * @param dbURL The database connection URL to be used.
     */
    public ImageNameAssigner(String dbURL) {
        this.dbURL = dbURL;
    }

    private void openConnection() throws SQLException {
        this.conn = DriverManager.getConnection(dbURL);
        conn.setAutoCommit(false);
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS"
                + "    ccs_counters"
                + "    ("
                + "        date VARCHAR(8) NOT NULL,"
                + "        value INT NOT NULL,"
                + "        source VARCHAR(2) NOT NULL,"
                + "        controller VARCHAR(2) NOT NULL,"
                + "        PRIMARY KEY (source, controller, date)"
                + "    )")) {
            stmt.execute();
        }

        stmt1 = conn.prepareStatement(
                "insert into ccs_counters (source, controller, date, value) values (?, ?, ?, @cur_value := ?) "
                + "on duplicate key update "
                + "value = @cur_value := (value + @cur_value)");
        stmt2 = conn.prepareStatement("select @cur_value");
    }

    /**
     * Assign a unique set of sequential indexes for the requested number of
     * images. This method is synchronized so that only one thread at a time can
     * request an index. In addition database transaction locking is used to
     * ensure that two separate instances of this class cannot get assigned the
     * same index.
     *
     * @param source The source used for the image.
     * @param controller The controller asking for the image.
     * @param date The date to be used for assigning the image index.
     * @param n The number of image numbers requested.
     * @return The unique, sequential image index.
     * @throws SQLException If an error occurs while communicating with the
     * database.
     */
    private synchronized int[] assignImageNumbers(Source source, Controller controller, LocalDate date, int n) throws SQLException {
        try {
            stmt1.setString(1, source.getCode());
            stmt1.setString(2, controller.getCode());
            stmt1.setString(3, ImageName.formatter.format(date));
            stmt1.setInt(4, n);
            stmt1.executeUpdate();

            try (ResultSet rs = stmt2.executeQuery()) {
                rs.next();
                int last = rs.getInt(1);
                int[] result = new int[n];
                // Numbers are guaranteed to be consecutive
                for (int i = n - 1; i >= 0; i--) {
                    result[i] = last--;
                }
                return result;
            }
        } catch (SQLException x) {
            conn.rollback();
            throw x;
        } finally {
            conn.commit();
        }
    }

    private LocalDate assignDate(TimeZone zone, Duration offset, Instant time) {
        Clock clock = Clock.offset(Clock.fixed(time, zone.toZoneId()), offset.negated());
        return LocalDate.now(clock);
    }

    /**
     * Assign an image name for the given source, controller, time zone and
     * offset and using the current timestamp.
     *
     * @param source The source to be used for the image name
     * @param controller The controller to be used for the image name
     * @param zone The time-zone to be used for assigning dates
     * @param offset The offset from midnight in the specified time zone when
     * the date assigned to the image will rollover.
     * @return The image name.
     * @throws SQLException If there is an error communicating with the
     * database.
     */
    public ImageName assignImageName(Source source, Controller controller, TimeZone zone, Duration offset) throws SQLException {
        return assignImageNames(source, controller, zone, offset, 1, null).get(0);
    }

    /**
     * Assign an image name for the given source, controller, time zone and
     * offset and using the given timestamp.
     *
     * @param source The source to be used for the image name
     * @param controller The controller to be used for the image name
     * @param zone The time-zone to be used for assigning dates
     * @param offset The offset from midnight in the specified time zone when
     * the date assigned to the image will rollover.
     * @param time The instant to be used for generating the image date, or
     * <code>null</code> to use the current time.
     * @return The image name.
     * @throws SQLException If there is an error communicating with the
     * database.
     */
    public ImageName assignImageName(Source source, Controller controller, TimeZone zone, Duration offset, Instant time) throws SQLException {
        return assignImageNames(source, controller, zone, offset, 1, time).get(0);
    }

    /**
     * Assign a set of sequential image names for the given source, controller,
     * time zone and offset and using the current timestamp.
     *
     * @param source The source to be used for the image name
     * @param controller The controller to be used for the image name
     * @param zone The time-zone to be used for assigning dates
     * @param offset The offset from midnight in the specified time zone when
     * the date assigned to the image will rollover.
     * @param n The number of consecutive names to assign
     * @return A list of image names.
     * @throws SQLException If there is an error communicating with the
     * database.
     */
    public List<ImageName> assignImageNames(Source source, Controller controller, TimeZone zone, Duration offset, int n) throws SQLException {
        return assignImageNames(source, controller, zone, offset, n, null);
    }

    /**
     * Assign a set of sequential image names for the given source, controller,
     * time zone and offset and using the given timestamp.
     *
     * @param source The source to be used for the image name
     * @param controller The controller to be used for the image name
     * @param zone The time-zone to be used for assigning dates
     * @param offset The offset from midnight in the specified time zone when
     * the date assigned to the image will rollover.
     * @param n The number of consecutive names to assign
     * @param time The instant to be used for generating the image date, or
     * <code>null</code> to use the current time.
     * @return The image name.
     * @throws SQLException If there is an error communicating with the
     * database.
     */
    // Marked as synchronized to protect against multiple threads dealing with connection open/close
    public synchronized List<ImageName> assignImageNames(Source source, Controller controller, TimeZone zone, Duration offset, int n, Instant time) throws SQLException {
        LocalDate date = assignDate(zone, offset, time == null ? Instant.now() : time);
        for (int i = 0;; i++) {
            if (conn == null || conn.isClosed()) {
                openConnection();
            }
            try {
                int[] imageNumbers = assignImageNumbers(source, controller, date, n);
                List<ImageName> result = new ArrayList<>();
                for (int imageNumber : imageNumbers) {
                    result.add(new ImageName(source, controller, date, imageNumber));
                }
                return result;
            } catch (SQLException x) {
                if (i < MAX_RETRIES) {
                    try {
                       close();
                    } catch (SQLException ex) {
                        // Ignored, we still want to retry
                    }
                } else {
                    throw x;
                }
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if (conn != null) {
            try (Connection temp = conn) {
                this.conn = null;
            }
        }
    }
    
    // Just fot testing
    void forceClose() throws SQLException {
        conn.close();
    }
}
