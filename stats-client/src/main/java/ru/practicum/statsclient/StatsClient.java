package ru.practicum.statsclient;


import ru.practicum.statsdto.EndpointHitDto;
import ru.practicum.statsdto.ViewStatsDto;
import java.time.LocalDateTime;
import java.util.List;

public interface StatsClient {
    void hit(EndpointHitDto endpointHitDto);

    List<ViewStatsDto> getStats(LocalDateTime start,
                                LocalDateTime end,
                                List<String> uris,
                                boolean unique);
}
