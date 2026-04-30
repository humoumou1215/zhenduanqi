package com.zhenduanqi.repository;

import com.zhenduanqi.entity.CommandGuardRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandGuardRuleRepository extends JpaRepository<CommandGuardRule, Long> {
    List<CommandGuardRule> findByRuleTypeAndEnabledTrue(String ruleType);
}
