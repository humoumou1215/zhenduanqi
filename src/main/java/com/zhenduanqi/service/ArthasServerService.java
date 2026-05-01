package com.zhenduanqi.service;

import com.zhenduanqi.config.TokenEncryptionUtil;
import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.repository.ArthasServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArthasServerService {

    private static final Logger log = LoggerFactory.getLogger(ArthasServerService.class);

    private final ArthasServerRepository repository;
    private final TokenEncryptionUtil encryptionUtil;

    public ArthasServerService(ArthasServerRepository repository, TokenEncryptionUtil encryptionUtil) {
        this.repository = repository;
        this.encryptionUtil = encryptionUtil;
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

    public Optional<String> findDecryptedTokenById(String id) {
        return repository.findById(id)
                .map(entity -> encryptionUtil.decrypt(entity.getToken()));
    }

    public ArthasServerDTO create(ArthasServerDTO dto) {
        ArthasServerEntity entity = dto.toEntity();
        if (entity.getToken() != null) {
            entity.setToken(encryptionUtil.encrypt(entity.getToken()));
        }
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        ArthasServerEntity saved = repository.save(entity);
        log.info("服务器创建: id={}, name={}", saved.getId(), saved.getName());
        return ArthasServerDTO.fromEntity(saved);
    }

    public ArthasServerDTO update(String id, ArthasServerDTO dto) {
        ArthasServerEntity existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + id));
        existing.setName(dto.getName());
        existing.setHost(dto.getHost());
        existing.setHttpPort(dto.getHttpPort());
        if (dto.getToken() != null && !dto.getToken().isBlank()) {
            existing.setToken(encryptionUtil.encrypt(dto.getToken()));
        }
        existing.setUpdatedAt(LocalDateTime.now());
        ArthasServerEntity saved = repository.save(existing);
        log.info("服务器更新: id={}, name={}", saved.getId(), saved.getName());
        return ArthasServerDTO.fromEntity(saved);
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("服务器不存在: " + id);
        }
        repository.deleteById(id);
        log.info("服务器删除: id={}", id);
    }
}
