package com.zhenduanqi.service;

import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.repository.ArthasServerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArthasServerService {

    private final ArthasServerRepository repository;

    public ArthasServerService(ArthasServerRepository repository) {
        this.repository = repository;
    }

    public List<ArthasServerDTO> findAll() {
        return repository.findAll().stream()
                .map(ArthasServerDTO::fromEntity)
                .toList();
    }

    public Optional<ArthasServerDTO> findById(String id) {
        return repository.findById(id).map(ArthasServerDTO::fromEntity);
    }

    public Optional<ArthasServerEntity> findEntityById(String id) {
        return repository.findById(id);
    }

    public ArthasServerDTO create(ArthasServerDTO dto) {
        ArthasServerEntity entity = dto.toEntity();
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        ArthasServerEntity saved = repository.save(entity);
        return ArthasServerDTO.fromEntity(saved);
    }

    public ArthasServerDTO update(String id, ArthasServerDTO dto) {
        ArthasServerEntity existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + id));
        existing.setName(dto.getName());
        existing.setHost(dto.getHost());
        existing.setHttpPort(dto.getHttpPort());
        if (dto.getToken() != null && !dto.getToken().isBlank()) {
            existing.setToken(dto.getToken());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        ArthasServerEntity saved = repository.save(existing);
        return ArthasServerDTO.fromEntity(saved);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("服务器不存在: " + id);
        }
        repository.deleteById(id);
    }
}
