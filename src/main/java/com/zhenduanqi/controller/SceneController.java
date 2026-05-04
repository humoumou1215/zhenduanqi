package com.zhenduanqi.controller;

import com.zhenduanqi.annotation.RequireRole;
import com.zhenduanqi.entity.DiagnoseScene;
import com.zhenduanqi.entity.SceneStep;
import com.zhenduanqi.service.SceneService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenes")
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @GetMapping
    public ResponseEntity<List<DiagnoseScene>> getScenes(
            @RequestParam(required = false) String category) {
        List<DiagnoseScene> scenes;
        if (category != null && !category.isEmpty()) {
            scenes = sceneService.getScenesByCategory(category);
        } else {
            scenes = sceneService.getAllScenes();
        }
        return ResponseEntity.ok(scenes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiagnoseScene> getScene(@PathVariable Long id) {
        return sceneService.getSceneById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<SceneStep>> getSteps(@PathVariable Long id) {
        return ResponseEntity.ok(sceneService.getStepsBySceneId(id));
    }

    @PostMapping
    @RequireRole("ADMIN")
    public ResponseEntity<DiagnoseScene> createScene(@RequestBody DiagnoseScene scene) {
        DiagnoseScene created = sceneService.createScene(scene);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<DiagnoseScene> updateScene(
            @PathVariable Long id,
            @RequestBody DiagnoseScene scene) {
        try {
            return ResponseEntity.ok(sceneService.updateScene(id, scene));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<Void> deleteScene(@PathVariable Long id) {
        try {
            sceneService.deleteScene(id);
            return ResponseEntity.noContent().build();
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/steps")
    @RequireRole("ADMIN")
    public ResponseEntity<SceneStep> addStep(
            @PathVariable Long id,
            @RequestBody SceneStep step) {
        try {
            SceneStep created = sceneService.addStep(id, step);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/steps/{stepId}")
    @RequireRole("ADMIN")
    public ResponseEntity<SceneStep> updateStep(
            @PathVariable Long stepId,
            @RequestBody SceneStep step) {
        try {
            return ResponseEntity.ok(sceneService.updateStep(stepId, step));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/steps/{stepId}")
    @RequireRole("ADMIN")
    public ResponseEntity<Void> deleteStep(@PathVariable Long stepId) {
        try {
            sceneService.deleteStep(stepId);
            return ResponseEntity.noContent().build();
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/steps/reorder")
    @RequireRole("ADMIN")
    public ResponseEntity<Void> reorderSteps(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {
        try {
            sceneService.reorderSteps(id, request.get("stepIds"));
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
