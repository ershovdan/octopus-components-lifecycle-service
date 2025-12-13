package org.octopusden.octopus.lifecycle.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Integer> {
    Rule findByName(String name);
    boolean existsByName(String name);
}
