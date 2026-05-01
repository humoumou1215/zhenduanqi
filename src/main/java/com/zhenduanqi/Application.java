package com.zhenduanqi;

import com.zhenduanqi.entity.SysRole;
import com.zhenduanqi.entity.SysUser;
import com.zhenduanqi.repository.SysRoleRepository;
import com.zhenduanqi.repository.SysUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    CommandLineRunner initAdmin(PasswordEncoder passwordEncoder,
                                 SysUserRepository userRepository,
                                 SysRoleRepository roleRepository) {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                SysRole adminRole = roleRepository.findByRoleCode("ADMIN")
                        .orElseThrow(() -> new RuntimeException("管理员角色不存在"));
                SysUser admin = new SysUser();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("Abcd1234"));
                admin.setRealName("系统管理员");
                admin.setStatus("ACTIVE");
                admin.getRoles().add(adminRole);
                userRepository.save(admin);
                System.out.println("默认管理员账号已创建: admin / Abcd1234");
            }
        };
    }
}
