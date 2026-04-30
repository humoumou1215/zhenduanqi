package com.zhenduanqi.service;

import com.zhenduanqi.entity.SysAuditLog;
import com.zhenduanqi.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuditLogServiceTest {

    @Autowired
    private AuditLogRepository repository;

    @Test
    void queryWithPagination_returnsCorrectPage() {
        SysAuditLog log1 = new SysAuditLog();
        log1.setUsername("zhangsan");
        log1.setAction("EXECUTE_COMMAND");
        log1.setResult("SUCCESS");
        repository.save(log1);

        SysAuditLog log2 = new SysAuditLog();
        log2.setUsername("lisi");
        log2.setAction("LOGIN");
        log2.setResult("SUCCESS");
        repository.save(log2);

        AuditLogService service = new AuditLogService(repository);

        Page<SysAuditLog> page = service.query(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void queryWithUsernameFilter_returnsFilteredResults() {
        SysAuditLog log1 = new SysAuditLog();
        log1.setUsername("zhangsan");
        log1.setAction("EXECUTE_COMMAND");
        log1.setResult("SUCCESS");
        repository.save(log1);

        SysAuditLog log2 = new SysAuditLog();
        log2.setUsername("lisi");
        log2.setAction("LOGIN");
        log2.setResult("SUCCESS");
        repository.save(log2);

        AuditLogService service = new AuditLogService(repository);

        Page<SysAuditLog> page = service.query(
                "zhangsan", null, null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUsername()).isEqualTo("zhangsan");
    }

    @Test
    void queryWithActionFilter_returnsFilteredResults() {
        SysAuditLog log1 = new SysAuditLog();
        log1.setUsername("zhangsan");
        log1.setAction("EXECUTE_COMMAND");
        log1.setResult("SUCCESS");
        repository.save(log1);

        SysAuditLog log2 = new SysAuditLog();
        log2.setUsername("lisi");
        log2.setAction("LOGIN");
        log2.setResult("SUCCESS");
        repository.save(log2);

        AuditLogService service = new AuditLogService(repository);

        Page<SysAuditLog> page = service.query(
                null, "LOGIN", null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getAction()).isEqualTo("LOGIN");
    }
}
