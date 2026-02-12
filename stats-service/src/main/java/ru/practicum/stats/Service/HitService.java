package ru.practicum.stats.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.JPA.Hit;
import ru.practicum.stats.Repo.HitRepository;
import ru.practicum.statsdto.EndpointHitDto;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HitService {
    private final HitRepository hitRepository;

    @Transactional
    public void saveHit(EndpointHitDto endpointHitDto) {
        try {
            log.info("Сохранение хита: app={}, uri={}, ip={}, timestamp={}",
                    endpointHitDto.getApp(),
                    endpointHitDto.getUri(),
                    endpointHitDto.getIp(),
                    endpointHitDto.getTimestamp());

            Hit hit = new Hit();
            hit.setApp(endpointHitDto.getApp());
            hit.setUri(endpointHitDto.getUri());
            hit.setIp(endpointHitDto.getIp());

            // Защита от null timestamp
            if (endpointHitDto.getTimestamp() != null) {
                hit.setTimestamp(endpointHitDto.getTimestamp());
            } else {
                hit.setTimestamp(LocalDateTime.now());
                log.warn("Timestamp был null, установлено текущее время");
            }

            Hit savedHit = hitRepository.save(hit);
            log.info("Хит сохранен с ID: {}", savedHit.getId());

        } catch (Exception e) {
            log.error("Ошибка при сохранении хита: {}", e.getMessage(), e);
            throw e;
        }
    }
}