package com.senyer.senyer_opc.datacenter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfluxDBProperties {

    @Value("${influxdb.url}")
    private String url ;
    @Value("${influxdb.user}")
    private String user ;
    @Value("${influxdb.password}")
    private String password ;
    @Value("${influxdb.database}")
    private String database ;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
