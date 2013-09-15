package org.musicpd.android.tools;

import org.a0z.mpd.exception.MPDServerException;

public class Function
{
	public interface MPDAction0
	{
		void apply() throws MPDServerException;
	}
	public interface MPDFunction0<R>
	{
		R apply() throws MPDServerException;
	}
}
