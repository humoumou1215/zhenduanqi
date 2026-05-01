package com.zhenduanqi.repository;

import com.zhenduanqi.entity.ArthasSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ArthasSessionRepository extends JpaRepository<ArthasSession, Long>,
        JpaSpecificationExecutor<ArthasSession> {

    List<ArthasSession> findByStatusOrderByCreatedAtDesc(String status);

    List<ArthasSession> findByServerIdAndStatusOrderByCreatedAtDesc(String serverId, String status);

    List<ArthasSession> findByLastActiveAtBeforeAndStatus(LocalDateTime beforeTime, String status);
}
