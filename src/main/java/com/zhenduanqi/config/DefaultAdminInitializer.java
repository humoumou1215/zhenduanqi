package com.zhenduanqi.config;

import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysRoleRepository;
import com.zhenduanqi.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DefaultAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);

    private final SysUserRepository userRepository;
    private final SysRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminInitializer(SysUserRepository userRepository,
                                   SysRoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            log.info("Creating default admin user...");

            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("Abcd1234"));
            admin.setRealName("系统管理员");
            admin.setStatus("ACTIVE");

            Set<SysRole> adminRoles = new HashSet<>();
            roleRepository.findByRoleCode("ADMIN").ifPresentOrElse(
                    adminRoles::add,
                    () -> log.warn("ADMIN role not found, user will have no roles")
            );
            admin.setRoles(adminRoles);

            userRepository.save(admin);
            log.info("Default admin user created successfully with roles: {}", 
                    adminRoles.stream().map(SysRole::getRoleCode).toList());
        }

        if (userRepository.findByUsername("readonly").isEmpty()) {
            log.info("Creating default readonly user...");

            SysUser readonly = new SysUser();
            readonly.setUsername("readonly");
            readonly.setPassword(passwordEncoder.encode("Abcd1234"));
            readonly.setRealName("只读用户");
            readonly.setStatus("ACTIVE");

            Set<SysRole> readonlyRoles = new HashSet<>();
            roleRepository.findByRoleCode("READONLY").ifPresentOrElse(
                    readonlyRoles::add,
                    () -> log.warn("READONLY role not found, user will have no roles")
            );
            readonly.setRoles(readonlyRoles);

            userRepository.save(readonly);
            log.info("Default readonly user created successfully with roles: {}", 
                    readonlyRoles.stream().map(SysRole::getRoleCode).toList());
        }

        if (userRepository.findByUsername("operator").isEmpty()) {
            log.info("Creating default operator user...");

            SysUser operator = new SysUser();
            operator.setUsername("operator");
            operator.setPassword(passwordEncoder.encode("Abcd1234"));
            operator.setRealName("操作员");
            operator.setStatus("ACTIVE");

            Set<SysRole> operatorRoles = new HashSet<>();
            roleRepository.findByRoleCode("OPERATOR").ifPresentOrElse(
                    operatorRoles::add,
                    () -> log.warn("OPERATOR role not found, user will have no roles")
            );
            operator.setRoles(operatorRoles);

            userRepository.save(operator);
            log.info("Default operator user created successfully with roles: {}", 
                    operatorRoles.stream().map(SysRole::getRoleCode).toList());
        }
    }
}
