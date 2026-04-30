package com.zhenduanqi.repository;

import com.zhenduanqi.entity.ArthasServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArthasServerRepository extends JpaRepository<ArthasServerEntity, String> {
}
