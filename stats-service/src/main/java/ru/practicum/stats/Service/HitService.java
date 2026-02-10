package ru.practicum.stats.Service;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import ru.practicum.stats.JPA.Hit;
import ru.practicum.stats.Repo.HitRepository;
import ru.practicum.statsdto.EndpointHitDto;

@Service
public class HitService {
    private final HitRepository hitRepository;

    public HitService(HitRepository hitRepository) {
        this.hitRepository = hitRepository;
    }

    @Transactional
    public void saveHit(EndpointHitDto endpointHitDto) {
        Hit hit = new Hit();
        hit.setApp(endpointHitDto.getApp());
        hit.setUri(endpointHitDto.getUri());
        hit.setIp(endpointHitDto.getIp());
        hit.setTimestamp(endpointHitDto.getTimestamp());

        hitRepository.save(hit);
    }
}
