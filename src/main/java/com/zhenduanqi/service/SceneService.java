package com.zhenduanqi.service;

import com.zhenduanqi.entity.DiagnoseScene;
import com.zhenduanqi.entity.SceneStep;
import com.zhenduanqi.repository.DiagnoseSceneRepository;
import com.zhenduanqi.repository.SceneStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SceneService {

    private static final Logger log = LoggerFactory.getLogger(SceneService.class);

    private final DiagnoseSceneRepository sceneRepository;
    private final SceneStepRepository stepRepository;

    public SceneService(DiagnoseSceneRepository sceneRepository,
                        SceneStepRepository stepRepository) {
        this.sceneRepository = sceneRepository;
        this.stepRepository = stepRepository;
    }

    public List<DiagnoseScene> getAllScenes() {
        return sceneRepository.findByEnabledTrueOrderBySortOrderAsc();
    }

    public List<DiagnoseScene> getScenesByCategory(String category) {
        return sceneRepository.findByCategoryAndEnabledTrueOrderBySortOrderAsc(category);
    }

    public Optional<DiagnoseScene> getSceneById(Long id) {
        return sceneRepository.findByIdAndEnabledTrue(id);
    }

    public List<SceneStep> getStepsBySceneId(Long sceneId) {
        return stepRepository.findBySceneIdOrderByStepOrderAsc(sceneId);
    }

    @Transactional
    public DiagnoseScene createScene(DiagnoseScene scene) {
        log.info("创建场景: {}", scene.getName());
        if (scene.getSteps() != null) {
            for (SceneStep step : scene.getSteps()) {
                step.setScene(scene);
            }
        }
        return sceneRepository.save(scene);
    }

    @Transactional
    public DiagnoseScene updateScene(Long id, DiagnoseScene scene) {
        log.info("更新场景: id={}, name={}", id, scene.getName());
        DiagnoseScene existing = sceneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        existing.setName(scene.getName());
        existing.setDescription(scene.getDescription());
        existing.setCategory(scene.getCategory());
        existing.setBusinessScenario(scene.getBusinessScenario());
        existing.setIcon(scene.getIcon());
        existing.setSortOrder(scene.getSortOrder());
        existing.setEnabled(scene.isEnabled());
        return sceneRepository.save(existing);
    }

    @Transactional
    public void deleteScene(Long id) {
        log.info("删除场景: id={}", id);
        sceneRepository.deleteById(id);
    }

    @Transactional
    public SceneStep addStep(Long sceneId, SceneStep step) {
        log.info("添加步骤: sceneId={}, title={}", sceneId, step.getTitle());
        DiagnoseScene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new RuntimeException("场景不存在"));
        step.setScene(scene);
        return stepRepository.save(step);
    }

    @Transactional
    public SceneStep updateStep(Long stepId, SceneStep step) {
        log.info("更新步骤: stepId={}", stepId);
        SceneStep existing = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("步骤不存在"));
        existing.setStepOrder(step.getStepOrder());
        existing.setTitle(step.getTitle());
        existing.setDescription(step.getDescription());
        existing.setCommand(step.getCommand());
        existing.setExpectedHint(step.getExpectedHint());
        existing.setContinuous(step.isContinuous());
        existing.setMaxExecTime(step.getMaxExecTime());
        existing.setExtractRules(step.getExtractRules());
        return stepRepository.save(existing);
    }

    @Transactional
    public void deleteStep(Long stepId) {
        log.info("删除步骤: stepId={}", stepId);
        stepRepository.deleteById(stepId);
    }

    @Transactional
    public void reorderSteps(Long sceneId, List<Long> stepIds) {
        log.info("重新排序步骤: sceneId={}, stepIds={}", sceneId, stepIds);
        for (int i = 0; i < stepIds.size(); i++) {
            Long stepId = stepIds.get(i);
            SceneStep step = stepRepository.findById(stepId)
                    .orElseThrow(() -> new RuntimeException("步骤不存在"));
            step.setStepOrder(i + 1);
            stepRepository.save(step);
        }
    }
}
