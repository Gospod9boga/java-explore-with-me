package ru.practicum.stats.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.stats.Repo.HitRepository;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final HitRepository hitRepository;

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {

        log.info("Getting stats from {} to {}, uris={}, unique={}", start, end, uris, unique);

        List<Object[]> results;

        if (unique) {
            results = hitRepository.findUniqueStats(start, end, uris);
        } else {
            results = hitRepository.findStats(start, end, uris);
        }

        List<ViewStatsDto> dtos = results.stream()
                .map(result -> ViewStatsDto.builder()
                        .app((String) result[0])
                        .uri((String) result[1])
                        .hits((Long) result[2])
                        .build())
                .collect(Collectors.toList());

        log.info("Found {} stats records", dtos.size());
        return dtos;
    }
}