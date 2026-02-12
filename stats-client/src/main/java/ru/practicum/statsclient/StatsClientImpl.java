package ru.practicum.statsclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.statsclient.config.StatsClientConfig;
import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsClientImpl implements StatsClient {

    private final RestTemplate restTemplate;
    private final StatsClientConfig config;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EndpointHitDto> request = new HttpEntity<>(endpointHitDto, headers);
            String url = config.getUrl() + "/hit";

            log.info("Отправка POST запроса на URL: {}", url);
            log.info("Тело запроса: {}", endpointHitDto);

            restTemplate.postForEntity(url, request, Void.class);
            log.info("Хит успешно отправлен");
        } catch (Exception e) {
            log.error("Ошибка при отправке хита: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(config.getUrl() + "/stats")
                    .queryParam("start", start.format(FORMATTER))
                    .queryParam("end", end.format(FORMATTER))
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    builder.queryParam("uris", uri);
                }
            }

            String url = builder.build().encode().toUriString();
            log.info("Запрос статистики: {}", url);

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );

            List<ViewStatsDto> stats = response.getBody();
            log.info("Получено записей статистики: {}", stats != null ? stats.size() : 0);

            return stats != null ? stats : Collections.emptyList();

        } catch (Exception e) {
            log.error("Ошибка при получении статистики: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}