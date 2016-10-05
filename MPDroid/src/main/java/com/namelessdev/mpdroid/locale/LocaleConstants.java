/*
 * Copyright 2013 two forty four a.m. LLC <http://www.twofortyfouram.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <http://www.apache.org/licenses/LICENSE-2.0>
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.namelessdev.mpdroid.locale;

import android.os.Parcelable;

/**
 * Contains LocaleConstants constants necessary for interacting with the Locale Developer Platform.
 */
@SuppressWarnings("unused")
public final class LocaleConstants {

    /**
     * {@code LocaleConstants} action {@code String} broadcast by Locale to create or edit a
     * plug-in condition. When Locale broadcasts this {@code LocaleConstants}, it will be sent
     * directly to the package and class of the plug-in's {@code Activity}. The
     * {@code LocaleConstants} may contain a store-and-forward {@link #EXTRA_BUNDLE} that was
     * previously set by the {@code Activity} result of {@link #ACTION_EDIT_CONDITION}.
     *
     * <p>There SHOULD be only one {@code Activity} per APK that implements this {@code
     * LocaleConstants}. If a single APK wishes to export multiple plug-ins, it MAY implement
     * multiple Activity instances that implement this {@code LocaleConstants}, however there must
     * only be a single {@link #ACTION_QUERY_CONDITION} receiver. In this scenario, it is the
     * responsibility of the Activities to store enough data in {@link #EXTRA_BUNDLE} to allow this
     * receiver to disambiguate which "plug-in" is being queried. To avoid user confusion, it is
     * recommended that only a single plug-in be implemented per APK.</p>
     *
     * @see LocaleConstants#EXTRA_BUNDLE
     * @see LocaleConstants#EXTRA_STRING_BREADCRUMB
     */
    public static final String ACTION_EDIT_CONDITION
            = "com.twofortyfouram.locale.intent.action.EDIT_CONDITION"; //$NON-NLS-1$

    /**
     * {@code LocaleConstants} action {@code String} broadcast by Locale to create or edit a
     * plug-in setting. When Locale broadcasts this {@code LocaleConstants}, it will be sent
     * directly to the package and class of the plug-in's {@code Activity}. The
     * {@code LocaleConstants} may contain a {@link #EXTRA_BUNDLE} that was previously set by the
     * {@code Activity} result of {@link #ACTION_EDIT_SETTING}.
     *
     * <p>There SHOULD be only one {@code Activity} per APK that implements this {@code
     * LocaleConstants}. If a single APK wishes to export multiple plug-ins, it MAY implement
     * multiple Activity instances that implement this {@code LocaleConstants}, however there must
     * only be a single {@link #ACTION_FIRE_SETTING} receiver. In this scenario, it is the
     * responsibility of the Activities to store enough data in {@link #EXTRA_BUNDLE} to allow this
     * receiver to disambiguate which "plug-in" is being fired. To avoid user confusion, it is
     * recommended that only a single plug-in be implemented per APK.</p>
     *
     * @see LocaleConstants#EXTRA_BUNDLE
     * @see LocaleConstants#EXTRA_STRING_BREADCRUMB
     */
    public static final String ACTION_EDIT_SETTING
            = "com.twofortyfouram.locale.intent.action.EDIT_SETTING"; //$NON-NLS-1$

    /**
     * {@code LocaleConstants} action {@code String} broadcast by Locale to fire a plug-in setting.
     * When Locale broadcasts this {@code LocaleConstants}, it will be sent directly to the package
     * and class of the plug-in's {@code BroadcastReceiver}. The {@code LocaleConstants} will
     * contain a {@link #EXTRA_BUNDLE} that was previously set by the {@code Activity} result of
     * {@link #ACTION_EDIT_SETTING}.
     *
     * <p>There MUST be only one {@code BroadcastReceiver} per APK that implements this {@code
     * LocaleConstants}.</p>
     *
     * @see LocaleConstants#EXTRA_BUNDLE
     */
    public static final String ACTION_FIRE_SETTING
            = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"; //$NON-NLS-1$

    /**
     * Ordered {@code LocaleConstants} action {@code String} broadcast by Locale to query a plug-in
     * condition. When Locale broadcasts this {@code LocaleConstants}, it will be sent directly to
     * the package and class of the plug-in's {@code BroadcastReceiver}. The {@code
     * LocaleConstants} will contain a {@link #EXTRA_BUNDLE} that was previously set by the
     * {@code Activity} result of {@link #ACTION_EDIT_CONDITION}.
     *
     * <p>Since this is an ordered broadcast, the receiver is expected to set an appropriate result
     * code from {@link #RESULT_CONDITION_SATISFIED}, {@link #RESULT_CONDITION_UNSATISFIED}, and
     * {@link #RESULT_CONDITION_UNKNOWN}.</p>
     *
     * <p>There MUST be only one {@code BroadcastReceiver} per APK that implements this {@code
     * LocaleConstants}.</p>
     *
     * @see LocaleConstants#EXTRA_BUNDLE
     * @see LocaleConstants#RESULT_CONDITION_SATISFIED
     * @see LocaleConstants#RESULT_CONDITION_UNSATISFIED
     * @see LocaleConstants#RESULT_CONDITION_UNKNOWN
     */
    public static final String ACTION_QUERY_CONDITION
            = "com.twofortyfouram.locale.intent.action.QUERY_CONDITION"; //$NON-NLS-1$

    /**
     * {@code LocaleConstants} action {@code String} to notify Locale that a plug-in condition is
     * requesting that Locale query it via {@link #ACTION_QUERY_CONDITION}.
     *
     * <p>This merely serves as a hint to Locale that a condition wants to be queried. There is no
     * guarantee as to when or if the plug-in will be queried after this {@code LocaleConstants}
     * is broadcast. If Locale does not respond to the plug-in condition after a
     * {@code ACTION_REQUEST_QUERY} LocaleConstants is sent, the plug-in SHOULD shut itself down
     * and stop requesting queries. A lack of response from Locale indicates that Locale is not
     * currently interested in this plug-in. When Locale becomes interested in the plug-in again,
     * Locale will send {@link #ACTION_QUERY_CONDITION}.</p>
     *
     * <p>The extra {@link #EXTRA_ACTIVITY} MUST be included, otherwise Locale will ignore this
     * {@code LocaleConstants}.</p>
     *
     * <p>Plug-in conditions SHOULD NOT use this unless there is some sort of asynchronous event
     * that has occurred, such as a broadcast {@code LocaleConstants} being received by the
     * plug-in. Plug-ins SHOULD NOT periodically request a requery as a way of implementing
     * polling behavior.</p>
     *
     * @see LocaleConstants#EXTRA_ACTIVITY
     */
    public static final String ACTION_REQUEST_QUERY
            = "com.twofortyfouram.locale.intent.action.REQUEST_QUERY"; //$NON-NLS-1$

    /**
     * Type: {@code String}.
     *
     * <p>Maps to a {@code String} that represents the name of a plug-in's {@code Activity}.</p>
     *
     * @see LocaleConstants#ACTION_REQUEST_QUERY
     */
    public static final String EXTRA_ACTIVITY = "com.twofortyfouram.locale.intent.extra.ACTIVITY";

    /**
     * Type: {@code Bundle}.
     *
     * <p>Maps to a {@code Bundle} that contains all of a plug-in's extras.</p>
     *
     * <p>Plug-ins MUST NOT store {@link Parcelable} objects in this {@code Bundle}, because
     * {@code Parcelable} is not a long-term storage format. Also, plug-ins MUST NOT store any
     * serializable object that is not exposed by the Android SDK.</p>
     *
     * <p>The maximum size of a Bundle that can be sent across process boundaries is on the order
     * of 500 kilobytes (base-10), while Locale further limits plug-in Bundles to about 100
     * kilobytes (base-10). Although the maximum size is about 100 kilobytes, plug-ins SHOULD keep
     * Bundles much smaller for performance and memory usage reasons.</p>
     */
    public static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";

    /**
     * Type: {@code String}.
     *
     * <p>Maps to a {@code String} that represents a blurb. This is returned as an {@code Activity}
     * result extra from {@link #ACTION_EDIT_CONDITION} or {@link #ACTION_EDIT_SETTING}.</p>
     *
     * <p>The blurb is a concise description displayed to the user of what the plug-in is
     * configured to do.</p>
     */
    public static final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";

    /**
     * Type: {@code String}.
     *
     * <p>Maps to a {@code String} that represents the {@code Activity} bread crumb path.</p>
     *
     * @see LocaleConstants
     */
    public static final String EXTRA_STRING_BREADCRUMB
            = "com.twofortyfouram.locale.intent.extra.BREADCRUMB"; //$NON-NLS-1$

    /**
     * Ordered broadcast result code indicating that a plug-in condition's state is satisfied
     * (true).
     *
     * @see LocaleConstants#ACTION_QUERY_CONDITION
     */
    public static final int RESULT_CONDITION_SATISFIED = 16;

    /**
     * Ordered broadcast result code indicating that a plug-in condition's state is unknown
     * (neither true nor false).
     *
     * <p>If a condition returns UNKNOWN, then Locale will use the last known return value on a
     * best-effort basis. Best-effort means that Locale may not persist known values forever (e.g.
     * last known values could hypothetically be cleared after a device reboot or a restart of the
     * Locale process. If there is no last known return value, then unknown is treated as not
     * satisfied (false).</p>
     *
     * <p>The purpose of an UNKNOWN result is to allow a plug-in condition more than 10 seconds to
     * process a query. A {@code BroadcastReceiver} must return within 10 seconds, otherwise it
     * will be killed by Android. A plug-in that needs more than 10 seconds might initially return
     * {@code #RESULT_CONDITION_UNKNOWN}, subsequently request a requery, and then return either
     * {@link #RESULT_CONDITION_SATISFIED} or {@link #RESULT_CONDITION_UNSATISFIED}.</p>
     *
     * @see LocaleConstants#ACTION_QUERY_CONDITION
     */
    public static final int RESULT_CONDITION_UNKNOWN = 18;
    //$NON-NLS-1$

    /**
     * Ordered broadcast result code indicating that a plug-in condition's state is not satisfied
     * (false).
     *
     * @see LocaleConstants#ACTION_QUERY_CONDITION
     */
    public static final int RESULT_CONDITION_UNSATISFIED = 17;
    //$NON-NLS-1$

    /**
     * Private constructor prevents instantiation.
     *
     * @throws UnsupportedOperationException because this class cannot be instantiated.
     */
    private LocaleConstants() {
        super();
        throw new UnsupportedOperationException("This class is non-instantiable"); //$NON-NLS-1$
    }
    //$NON-NLS-1$
}