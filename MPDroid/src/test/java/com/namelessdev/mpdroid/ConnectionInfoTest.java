package com.namelessdev.mpdroid;

import com.anpmech.mpd.MPDCommand;
import com.namelessdev.mpdroid.ConnectionInfo.Builder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests the {@link ConnectionInfo} class.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ConnectionInfoTest {

    /**
     * This is the error message given when a notification inverse persistence error exists.
     */
    private static final String NOTIFICATION_PERSISTENCE_INVERSE
            = "isNotificationPersistent() result is inverse.";

    /**
     * This pattern matches a literal "1".
     */
    private static final Pattern PATTERN_ONE = Pattern.compile("1", Pattern.LITERAL);

    /**
     * This is the error message for when a notification persistence status setting method is not
     * called prior to calling the prior build method.
     */
    private static final String PERSISTENCE_NOT_CALLED
            = "Builder cannot succeed without calling a notification persistence status method.";

    /**
     * This is a test hostname for use in this class.
     */
    private static final String TEST_HOSTNAME = "127.0.0.1";

    /**
     * This field is a test password for use in this class.
     */
    private static final String TEST_PASSWORD = "<password>";

    /**
     * This field is a test stream host for use in this class.
     */
    private static final String TEST_STREAM_HOST = "http://user:password@" + TEST_HOSTNAME + ':'
            + MPDCommand.DEFAULT_MPD_PORT + "/mpd.mp3";

    /**
     * This field allows the expected exception to be changed from none to a specific one.
     */
    @Rule
    public final ExpectedException mException = ExpectedException.none();

    /**
     * Sole constructor.
     */
    public ConnectionInfoTest() {
        super();
    }

    /**
     * This is a commonly used builder for a standard ConnectionInfo object.
     *
     * @return A standard test ConnectionInfo object.
     */
    private static Builder getStandardBuilder() {
        return new Builder(TEST_HOSTNAME, MPDCommand.DEFAULT_MPD_PORT,
                TEST_PASSWORD);
    }

    /**
     * This method tests to ensure an exception is thrown if trying to build a ConnectionInfo
     * Object without giving a previous ConnectionInfo object.
     */
    @Test
    public void testBuildWithoutPreviousBuildException() {
        final Builder builder = getStandardBuilder();
        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);

        mException.expect(IllegalStateException.class);
        mException.reportMissingExceptionWithMessage(
                "Builder cannot succeed without previous build.");
        builder.build();
    }

    /**
     * This method checks to ensure {@link ConnectionInfo#hasHostnameChanged()} is {@code true}
     * upon password change.
     */
    @Test
    public void testChangedHost() {
        Builder builder = getStandardBuilder();

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        final ConnectionInfo previous = builder.build();

        builder = new Builder(PATTERN_ONE.matcher(TEST_HOSTNAME).replaceAll("2"),
                MPDCommand.DEFAULT_MPD_PORT, TEST_PASSWORD);

        builder.setNotificationNotPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(previous);

        assertTrue("Hostname changed but not reflected by hasServerChanged().",
                builder.build().hasHostnameChanged());
    }

    /**
     * This method checks to ensure {@link ConnectionInfo#hasHostPasswordChanged()} is {@code true}
     * upon password change.
     */
    @Test
    public void testChangedPassword() {
        Builder builder = new Builder(TEST_HOSTNAME,
                MPDCommand.DEFAULT_MPD_PORT, "different");

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        final ConnectionInfo previous = builder.build();

        builder = getStandardBuilder();
        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(previous);

        assertTrue("hasHostPortChanged() did not change after port change.",
                builder.build().hasHostPasswordChanged());
    }

    /**
     * This method checks to ensure {@link ConnectionInfo#hasHostPortChanged()} is {@code true}
     * upon password change.
     */
    @Test
    public void testChangedPort() {
        Builder builder = getStandardBuilder();

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        final ConnectionInfo previous = builder.build();

        builder = new Builder(TEST_HOSTNAME, MPDCommand.DEFAULT_MPD_PORT + 1,
                TEST_PASSWORD);

        builder.setNotificationNotPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(previous);

        assertTrue("Host port changed but not reflected by hasServerChanged().",
                builder.build().hasHostPortChanged());
    }

    /**
     * This method tests to ensure an exception is thrown if trying to build a ConnectionInfo
     * Object without calling {@link Builder#setNotificationPersistent()} or
     * {@link Builder#setNotificationNotPersistent()} prior to calling {@link Builder#build()}.
     */
    @Test
    public void testIncorrectOrderMissingNotification() {
        final Builder builder = getStandardBuilder();

        builder.setStreamingServer(TEST_STREAM_HOST);
        mException.expect(IllegalStateException.class);
        mException.reportMissingExceptionWithMessage(PERSISTENCE_NOT_CALLED);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);
        builder.build();
    }

    /**
     * This method tests to ensure an exception is thrown if trying to build a ConnectionInfo
     * Object without calling the {@link Builder#setStreamingServer(String)} prior to calling
     * {@link Builder#build()}.
     */
    @Test
    public void testIncorrectOrderMissingStreaming() {
        final Builder builder = getStandardBuilder();

        builder.setNotificationNotPersistent();
        mException.expect(IllegalStateException.class);
        mException.reportMissingExceptionWithMessage(
                "Builder cannot succeed without calling the streaming server set method.");
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);
        builder.build();
    }

    /**
     * This method tests to make sure If the last notification was persistent, this ConnectionInfo
     * object will show that it was.
     */
    @Test
    public void testInverseNotificationPersistence() {
        Builder builder = getStandardBuilder();

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        final ConnectionInfo persistent = builder.build();

        builder = getStandardBuilder();

        builder.setNotificationNotPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(persistent);

        assertTrue("When the notification was persistent this must always be true.",
                builder.build().wasNotificationPersistent());
    }

    /**
     * This method tests to ensure if built as a non-persistent notification that the resulting
     * ConnectionInfo Object shows it as such.
     */
    @Test
    public void testNotificationNonPersistence() {
        final Builder builder = getStandardBuilder();

        builder.setNotificationNotPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        assertFalse(NOTIFICATION_PERSISTENCE_INVERSE, builder.build().isNotificationPersistent());
    }

    /**
     * This method tests to ensure if built as a persistent notification that the resulting
     * ConnectionInfo object shows it as such.
     */
    @Test
    public void testNotificationPersistence() {
        final Builder builder = getStandardBuilder();

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        assertTrue(NOTIFICATION_PERSISTENCE_INVERSE, builder.build().isNotificationPersistent());
    }

    /**
     * This method ensures that {@link ConnectionInfo#hasStreamInfoChanged()} is true upon stream
     * host URI change.
     */
    @Test
    public void testStreamInfoChanged() {
        Builder builder = getStandardBuilder();

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        final ConnectionInfo previous = builder.build();

        builder = getStandardBuilder();

        builder.setNotificationNotPersistent();
        builder.setStreamingServer(PATTERN_ONE.matcher(TEST_STREAM_HOST).replaceAll("2"));
        builder.setPreviousConnectionInfo(previous);

        assertTrue("Stream host information changed, but not reflected by hasStreamInfoChanged().",
                builder.build().hasStreamInfoChanged());
    }

    /**
     * This method ensures that {@link ConnectionInfo#hasServerChanged()} is false upon stream
     * host not changing.
     */
    @Test
    public void testStreamInfoNotChanged() {
        Builder builder = getStandardBuilder();

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        final ConnectionInfo previous = builder.build();

        builder = getStandardBuilder();

        builder.setNotificationNotPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(previous);

        assertFalse("Stream host information unchanged, but not reflected by " +
                "hasStreamInfoChanged().", builder.build().hasStreamInfoChanged());
    }

    /**
     * This method checks to ensure {@link ConnectionInfo#hasServerChanged()} is {@code false}
     * upon an unchanged server. This
     */
    @Test
    public void testUnchangedServer() {
        Builder builder = getStandardBuilder();

        builder.setNotificationPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(ConnectionInfo.EMPTY);

        final ConnectionInfo previous = builder.build();

        builder = getStandardBuilder();

        builder.setNotificationNotPersistent();
        builder.setStreamingServer(TEST_STREAM_HOST);
        builder.setPreviousConnectionInfo(previous);

        assertFalse("Server not changed not reflected by hasServerChanged().",
                builder.build().hasServerChanged());
    }
}
