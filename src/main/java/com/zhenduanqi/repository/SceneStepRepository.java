package com.zhenduanqi.repository;

import com.zhenduanqi.entity.SceneStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SceneStepRepository extends JpaRepository<SceneStep, Long> {
    List<SceneStep> findBySceneIdOrderByStepOrderAsc(Long sceneId);
}
