package com.zhenduanqi.aspect;

import com.zhenduanqi.dto.ExecuteRequest;
import com.zhenduanqi.entity.SysAuditLog;
import com.zhenduanqi.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogTimezoneIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void savedAuditLog_createdAt_usesAsiaShanghaiTimezone() {
        SysAuditLog log = new SysAuditLog();
        log.setUsername("testuser");
        log.setAction("TEST_ACTION");
        log.setResult("SUCCESS");

        LocalDateTime beforeSave = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        SysAuditLog saved = auditLogRepository.save(log);
        LocalDateTime afterSave = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(afterSave);

        ZonedDateTime createdAtShanghai = saved.getCreatedAt().atZone(ZoneId.of("Asia/Shanghai"));
        ZonedDateTime expectedShanghai = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        long minutesDiff = Math.abs(java.time.Duration.between(createdAtShanghai, expectedShanghai).toMinutes());
        assertThat(minutesDiff).isLessThan(1);
    }
}
