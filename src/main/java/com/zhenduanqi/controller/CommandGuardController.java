package com.zhenduanqi.controller;

import com.zhenduanqi.annotation.RequireRole;
import com.zhenduanqi.entity.CommandGuardRule;
import com.zhenduanqi.service.CommandGuardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/command-guard/rules")
public class CommandGuardController {

    private final CommandGuardService guardService;

    public CommandGuardController(CommandGuardService guardService) {
        this.guardService = guardService;
    }

    @GetMapping
    @RequireRole("ADMIN")
    public List<CommandGuardRule> getRules() {
        return guardService.getAllRules();
    }

    @PostMapping
    @RequireRole("ADMIN")
    public ResponseEntity<CommandGuardRule> addRule(@RequestBody CommandGuardRule rule) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardService.addRule(rule));
    }

    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<CommandGuardRule> updateRule(@PathVariable Long id, @RequestBody CommandGuardRule rule) {
        try {
            return ResponseEntity.ok(guardService.updateRule(id, rule));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        guardService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
