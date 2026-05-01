package com.zhenduanqi.repository;

import com.zhenduanqi.entity.ArthasSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArthasSessionRepository extends JpaRepository<ArthasSession, Long> {

    List<ArthasSession> findByServerIdAndStatusOrderByCreatedAtDesc(String serverId, String status);

    List<ArthasSession> findBySceneIdAndStatusOrderByCreatedAtDesc(Long sceneId, String status);

    List<ArthasSession> findByUsernameAndStatusOrderByCreatedAtDesc(String username, String status);

    List<ArthasSession> findByStatusOrderByCreatedAtDesc(String status);

    Optional<ArthasSession> findByIdAndStatus(Long id, String status);

    List<ArthasSession> findByLastActiveAtBeforeAndStatus(LocalDateTime before, String status);
}
