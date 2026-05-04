package com.zhenduanqi.service;

import com.zhenduanqi.client.ArthasHttpClient;
import com.zhenduanqi.config.TokenEncryptionUtil;
import com.zhenduanqi.dto.ArthasServerDTO;
import com.zhenduanqi.dto.ServerStatusDTO;
import com.zhenduanqi.entity.ArthasServerEntity;
import com.zhenduanqi.model.ServerInfo;
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
    private final ArthasHttpClient arthasHttpClient;

    public ArthasServerService(ArthasServerRepository repository, TokenEncryptionUtil encryptionUtil, ArthasHttpClient arthasHttpClient) {
        this.repository = repository;
        this.encryptionUtil = encryptionUtil;
        this.arthasHttpClient = arthasHttpClient;
    }

    public ServerStatusDTO checkConnection(String id) {
        Optional<ServerInfo> serverInfoOpt = findServerInfoById(id);
        if (serverInfoOpt.isEmpty()) {
            return ServerStatusDTO.failure("服务器不存在");
        }
        ServerInfo serverInfo = serverInfoOpt.get();
        ArthasHttpClient.ServerStatusResult result = arthasHttpClient.checkConnectionDetailed(serverInfo);
        if (result.isConnected()) {
            return ServerStatusDTO.success(result.getMessage());
        } else {
            return ServerStatusDTO.failure(result.getError());
        }
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
                .map(entity -> entity.getToken() != null ? encryptionUtil.decrypt(entity.getToken()) : null);
    }

    public Optional<ServerInfo> findServerInfoById(String id) {
        return repository.findById(id)
                .map(this::toServerInfo);
    }

    private ServerInfo toServerInfo(ArthasServerEntity entity) {
        ServerInfo info = new ServerInfo();
        info.setId(entity.getId());
        info.setName(entity.getName());
        info.setHost(entity.getHost());
        info.setHttpPort(entity.getHttpPort());
        
        if (entity.getToken() != null) {
            try {
                info.setToken(encryptionUtil.decrypt(entity.getToken()));
            } catch (Exception e) {
                log.warn("Token decryption failed, using raw value for server {}", entity.getId());
                info.setToken(entity.getToken());
            }
        }
        
        info.setUsername(entity.getUsername());
        
        if (entity.getPassword() != null) {
            try {
                info.setPassword(encryptionUtil.decrypt(entity.getPassword()));
            } catch (Exception e) {
                log.warn("Password decryption failed, using raw value for server {}", entity.getId());
                info.setPassword(entity.getPassword());
            }
        }
        
        return info;
    }

    public ArthasServerDTO create(ArthasServerDTO dto) {
        if (repository.existsById(dto.getId())) {
            throw new RuntimeException("服务器 ID 已存在: " + dto.getId());
        }
        validateServerDTO(dto);
        ArthasServerEntity entity = dto.toEntity();
        if (entity.getToken() != null) {
            entity.setToken(encryptionUtil.encrypt(entity.getToken()));
        }
        if (entity.getPassword() != null) {
            entity.setPassword(encryptionUtil.encrypt(entity.getPassword()));
        }
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        ArthasServerEntity saved = repository.save(entity);
        log.info("服务器创建: id={}, name={}", saved.getId(), saved.getName());
        return ArthasServerDTO.fromEntity(saved);
    }

    private void validateServerDTO(ArthasServerDTO dto) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            throw new RuntimeException("服务器 ID 不能为空");
        }
        if (!dto.getId().matches("^[a-zA-Z0-9_-]{1,50}$")) {
            throw new RuntimeException("服务器 ID 格式无效，仅允许字母、数字、下划线、连字符，长度 1-50");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new RuntimeException("服务器名称不能为空");
        }
        if (dto.getHost() == null || dto.getHost().isBlank()) {
            throw new RuntimeException("主机地址不能为空");
        }
        if (!isValidHost(dto.getHost())) {
            throw new RuntimeException("主机地址格式无效");
        }
        if (dto.getHttpPort() < 1 || dto.getHttpPort() > 65535) {
            throw new RuntimeException("端口范围必须在 1-65535 之间");
        }
    }

    private boolean isValidHost(String host) {
        if (host.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            String[] parts = host.split("\\.");
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        }
        return host.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*$");
    }

    public ArthasServerDTO update(String id, ArthasServerDTO dto) {
        ArthasServerEntity existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("服务器不存在: " + id));
        existing.setName(dto.getName());
        existing.setHost(dto.getHost());
        existing.setHttpPort(dto.getHttpPort());
        existing.setUsername(dto.getUsername());
        if (dto.getToken() != null && !dto.getToken().isBlank()) {
            existing.setToken(encryptionUtil.encrypt(dto.getToken()));
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPassword(encryptionUtil.encrypt(dto.getPassword()));
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
