package org.a0z.mpd;

import java.util.List;

/**
 * Class representing MPD Server status.
 * 
 * @author Felipe Gustavo de Almeida
 * @version $Id: MPDStatus.java 2941 2005-02-09 02:34:21Z galmeida $
 */
public class MPDStatus {
	/**
	 * MPD State: playing.
	 */
	public static final String MPD_STATE_PLAYING = "play";

	/**
	 * MPD State: stopped.
	 */
	public static final String MPD_STATE_STOPPED = "stop";

	/**
	 * MPD State: paused.
	 */
	public static final String MPD_STATE_PAUSED = "pause";

	/**
	 * MPD State: unknown.
	 */
	public static final String MPD_STATE_UNKNOWN = "unknown";

	
	private int playlistVersion;
	private int playlistLength;
	
	private int song;
	private int songId;
	private int nextSong;
	private int nextSongId;

	private boolean repeat;
	private boolean random;
	private boolean updating;
	private boolean single;
	private boolean consume;

	private String state;
	private String error;

	private long elapsedTime;
	private long totalTime;

	private int crossfade;

	private int volume;
	private long bitrate;
	private int sampleRate;
	private int bitsPerSample;
	private int channels;


	MPDStatus() {
		volume = 0;
		bitrate = 0;
		playlistVersion = 0;
		playlistLength = 0;
		song = 0;
		songId = 0;
		repeat = false;
		random = false;
		state = null;
		error = null;
		elapsedTime = 0;
		totalTime = 0;
		crossfade = 0;
		sampleRate = 0;
		channels = 0;
		bitsPerSample = 0;
		updating = false;
	}

	/**
	 * Updates the state of the MPD Server...
	 * 
	 * @param response
	 */
	public void updateStatus(List<String> response) {
		// reset values
		this.updating = false;
		if (response == null)
			return;
		
		for (String line : response) {
			try {
				if (line.startsWith("volume:")) {
					this.volume = Integer.parseInt(line.substring("volume: ".length()));
				} else if (line.startsWith("bitrate:")) {
					this.bitrate = Long.parseLong(line.substring("bitrate: ".length()));
				} else if (line.startsWith("playlist:")) {
					this.playlistVersion = Integer.parseInt(line.substring("playlist: ".length()));
				} else if (line.startsWith("playlistlength:")) {
					this.playlistLength = Integer.parseInt(line.substring("playlistlength: ".length()));
				} else if (line.startsWith("song:")) {
					this.song = Integer.parseInt(line.substring("song: ".length()));
				} else if (line.startsWith("songid:")) {
					this.songId = Integer.parseInt(line.substring("songid: ".length()));
				} else if (line.startsWith("repeat:")) {
					this.repeat = "1".equals(line.substring("repeat: ".length())) ? true : false;
				} else if (line.startsWith("random:")) {
					this.random = "1".equals(line.substring("random: ".length())) ? true : false;
				} else if (line.startsWith("state:")) {
					String state = line.substring("state: ".length());

					if (MPD_STATE_PAUSED.equals(state)) {
						this.state = MPD_STATE_PAUSED;
					} else if (MPD_STATE_PLAYING.equals(state)) {
						this.state = MPD_STATE_PLAYING;
					} else if (MPD_STATE_STOPPED.equals(state)) {
						this.state = MPD_STATE_STOPPED;
					} else {
						this.state = MPD_STATE_UNKNOWN;
					}
				} else if (line.startsWith("error:")) {
					this.error = line.substring("error: ".length());
				} else if (line.startsWith("time:")) {
					String[] time = line.substring("time: ".length()).split(":");
					elapsedTime = Long.parseLong(time[0]);
					totalTime = Long.parseLong(time[1]);
				} else if (line.startsWith("audio:")) {
					String[] audio = line.substring("audio: ".length()).split(":");
					try {
						sampleRate = Integer.parseInt(audio[0]);
						bitsPerSample = Integer.parseInt(audio[1]);
						channels = Integer.parseInt(audio[2]);
					} catch (NumberFormatException e) {
						// Sometimes mpd sends "?" as a sampleRate or bitsPerSample, etc ... hotfix for a bugreport I had.
					}
				} else if (line.startsWith("xfade:")) {
					this.crossfade = Integer.parseInt(line.substring("xfade: ".length()));
				} else if (line.startsWith("updating_db:")) {
					this.updating = true;
				} else if (line.startsWith("nextsong:")) {
					this.nextSong = Integer.parseInt(line.substring("nextsong: ".length()));
				} else if (line.startsWith("nextsongid:")) {
					this.nextSongId = Integer.parseInt(line.substring("nextsongid: ".length()));
				} else if (line.startsWith("consume:")) {
					this.consume = "1".equals(line.substring("consume: ".length())) ? true : false;
				} else if (line.startsWith("single:")) {
					this.single = "1".equals(line.substring("single: ".length())) ? true : false;
				} else {
					// TODO : This floods logcat too much, will fix later
					// (new InvalidResponseException("unknown response: " + line)).printStackTrace();
				}
			} catch (RuntimeException e) {
				// Do nothing, these should be harmless
				e.printStackTrace();
			}
		}
	}

	/**
	 * Retrieves current track bitrate.
	 * 
	 * @return current track bitrate.
	 */
	public long getBitrate() {
		return bitrate;
	}

	/**
	 * Retrieves current track elapsed time.
	 * 
	 * @return current track elapsed time.
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Retrieves error message.
	 * 
	 * @return error message.
	 */
	public String getError() {
		return error;
	}

	/**
	 * Retrieves playlist version.
	 * 
	 * @return playlist version.
	 */
	public int getPlaylistVersion() {
		return playlistVersion;
	}

	/**
	 * Retrieves the length of the playlist.
	 * 
	 * @return the length of the playlist.
	 */
	public int getPlaylistLength() {
		return playlistLength;
	}

	/**
	 * If random is enabled return true, return false if random is disabled.
	 * 
	 * @return true if random is enabled, false if random is disabled
	 */
	public boolean isRandom() {
		return random;
	}

	/**
	 * If repeat is enabled return true, return false if repeat is disabled.
	 * 
	 * @return true if repeat is enabled, false if repeat is disabled.
	 */
	public boolean isRepeat() {
		return repeat;
	}

	/**
	 * Retrieves the process id of any database update task.
	 * 
	 * @return the process id of any database update task.
	 */
	public boolean isUpdating() {
		return updating;
	}
	
	public boolean isSingle() {
		return single;
	}
	
	public boolean isConsume() {
		return consume;
	}
	
	/**
	 * Retrieves current song playlist number.
	 * 
	 * @return current song playlist number.
	 */
	public int getSongPos() {
		return song;
	}

	/**
	 * Retrieves current song playlist id.
	 * 
	 * @return current song playlist id.
	 */
	public int getSongId() {
		return songId;
	}
	
	public int getNextSongPos() {
		return nextSong;
	}
	
	public int getNextSongId() {
		return nextSongId;
	}

	/**
	 * Retrieves player state. MPD_STATE_PLAYING, MPD_STATE_PAUSED, MPD_STATE_STOPPED or MPD_STATE_UNKNOWN
	 * 
	 * @return player state.
	 */
	public String getState() {
		return state;
	}

	/**
	 * Retrieves current track total time.
	 * 
	 * @return current track total time.
	 */
	public long getTotalTime() {
		return totalTime;
	}

	/**
	 * Retrieves volume (0-100).
	 * 
	 * @return volume.
	 */
	public int getVolume() {
		return volume;
	}

	/**
	 * Retrieves bits resolution from playing song.
	 * 
	 * @return bits resolution from playing song.
	 */
	public int getBitsPerSample() {
		return bitsPerSample;
	}

	/**
	 * Retrieves number of channels from playing song.
	 * 
	 * @return number of channels from playing song.
	 */
	public int getChannels() {
		return channels;
	}

	/**
	 * Retrieves current cross-fade time.
	 * 
	 * @return current cross-fade time in seconds.
	 */
	public int getCrossfade() {
		return crossfade;
	}

	/**
	 * Retrieves sample rate from playing song.
	 * 
	 * @return sample rate from playing song.
	 */
	public int getSampleRate() {
		return sampleRate;
	}
	
	/**
	 * Retrieves a string representation of the object.
	 * 
	 * @return a string representation of the object.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "volume: " + volume + ", bitrate: " + bitrate + ", playlist: " + playlistVersion + ", playlistLength: " + playlistLength
				+ ", song: " + song + ", songid: " + songId + ", repeat: " + repeat + ", random: " + random + ", state: " + state + ", error: "
				+ error + ", elapsedTime: " + elapsedTime + ", totalTime: " + totalTime;
	}

}