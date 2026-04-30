package com.zhenduanqi.repository;

import com.zhenduanqi.entity.SysAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<SysAuditLog, Long> {
}
