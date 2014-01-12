/*
 * Copyright 2014 Arnaud Barisain Monrose (The MPDroid Project)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public String getCoverFilename() {
        return coverFilename;
    }

    public String getHostname() {
        return hostname;
    }

    public Long getId() {
        return id;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public Integer getPort() {
        return port;
    }

    public String getStreamingHostname() {
        return streamingHostname;
    }

    public Integer getStreamingPort() {
        return streamingPort;
    }

    public String getStreamingSuffix() {
        return streamingSuffix;
    }

    public void setCoverFilename(String coverFilename) {
        this.coverFilename = coverFilename;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setStreamingHostname(String streamingHostname) {
        this.streamingHostname = streamingHostname;
    }

    public void setStreamingPort(Integer streamingPort) {
        this.streamingPort = streamingPort;
    }

    public void setStreamingSuffix(String streamingSuffix) {
        this.streamingSuffix = streamingSuffix;
    }

    public void setUseDatabaseCache(Boolean useDatabaseCache) {
        this.useDatabaseCache = useDatabaseCache;
    }

    public Boolean usesDatabaseCache() {
        return useDatabaseCache;
    }

}
