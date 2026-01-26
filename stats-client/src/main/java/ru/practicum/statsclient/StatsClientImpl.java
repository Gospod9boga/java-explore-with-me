package ru.practicum.statsclient;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsclient.config.StatsClientConfig;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StatsClientImpl implements StatsClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final StatsClientConfig config;

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EndpointHitDto> request = new HttpEntity<>(endpointHitDto, headers);
        String url = config.getUrl() + "/hit";
        restTemplate.postForEntity(url, request, Void.class);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        String baseUrl = config.getUrl() + "/stats?start={start}&end={end}&unique={unique}";

        Map<String, Object> params = new HashMap<>();
        params.put("start", start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("end", end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            baseUrl += "&uris={uris}";
            params.put("uris", String.join(",", uris));
        }

        ResponseEntity<ViewStatsDto[]> response = restTemplate.getForEntity(
                baseUrl,
                ViewStatsDto[].class,
                params
        );

        return Arrays.asList(response.getBody());
    }
}