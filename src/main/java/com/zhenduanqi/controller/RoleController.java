package com.zhenduanqi.controller;

import com.zhenduanqi.annotation.RequireRole;
import com.zhenduanqi.dto.RoleResponse;
import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.repository.SysRoleRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final SysRoleRepository roleRepository;

    public RoleController(SysRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @RequireRole("ADMIN")
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(SysRole::getId))
                .map(r -> {
                    RoleResponse resp = new RoleResponse();
                    resp.setId(r.getId());
                    resp.setRoleCode(r.getRoleCode());
                    resp.setRoleName(r.getRoleName());
                    resp.setDescription(r.getDescription());
                    return resp;
                })
                .collect(Collectors.toList());
    }
}
