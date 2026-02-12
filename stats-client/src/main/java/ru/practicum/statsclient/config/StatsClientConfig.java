package ru.practicum.statsclient.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "stats-server")
@Getter
@Setter
public class StatsClientConfig {
    private String url = "http://stats-server:9090";

    public String getUrl() {
        return url;
    }
}