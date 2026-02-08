package ru.practicum.stats.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import ru.practicum.stats.Repo.HitRepository;
import ru.practicum.statsdto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final HitRepository hitRepository;

    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {

        List<Object[]> results;

        if (unique) {
            results = hitRepository.findUniqueStats(start, end, uris);
        } else {
            results = hitRepository.findStats(start, end, uris);
        }

        return results.stream()
                .map(result -> ViewStatsDto.builder()
                        .app((String) result[0])
                        .uri((String) result[1])
                        .hits((Long) result[2])
                        .build())
                .collect(Collectors.toList());
    }
}