package com.namelessdev.mpdroid.data.model;

public class ConnectionProfile {

    private Long id;
    private String name;
    private String hostname;
    private Integer port;
    private String password;
    private String streamingHostname;
    private Integer streamingPort;
    private String streamingSuffix;
    private String musicPath;
    private String coverFilename;
    private Boolean useDatabaseCache;
    
    public ConnectionProfile() {
        this.id = 0l;
    }
    
    public ConnectionProfile(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStreamingHostname() {
        return streamingHostname;
    }

    public void setStreamingHostname(String streamingHostname) {
        this.streamingHostname = streamingHostname;
    }

    public Integer getStreamingPort() {
        return streamingPort;
    }

    public void setStreamingPort(Integer streamingPort) {
        this.streamingPort = streamingPort;
    }

    public String getStreamingSuffix() {
        return streamingSuffix;
    }

    public void setStreamingSuffix(String streamingSuffix) {
        this.streamingSuffix = streamingSuffix;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    public String getCoverFilename() {
        return coverFilename;
    }

    public void setCoverFilename(String coverFilename) {
        this.coverFilename = coverFilename;
    }

    public Boolean usesDatabaseCache() {
        return useDatabaseCache;
    }

    public void setUseDatabaseCache(Boolean useDatabaseCache) {
        this.useDatabaseCache = useDatabaseCache;
    }
    
}
