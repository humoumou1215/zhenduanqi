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

            Set<SysRole> roles = new HashSet<>();
            roleRepository.findByRoleCode("ADMIN").ifPresentOrElse(
                    roles::add,
                    () -> log.warn("ADMIN role not found, user will have no roles")
            );
            admin.setRoles(roles);

            userRepository.save(admin);
            log.info("Default admin user created successfully with roles: {}", 
                    roles.stream().map(SysRole::getRoleCode).toList());
        }
    }
}
