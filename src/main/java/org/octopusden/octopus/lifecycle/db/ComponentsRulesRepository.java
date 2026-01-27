package org.octopusden.octopus.lifecycle.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComponentsRulesRepository extends JpaRepository<ComponentsRules, Integer> {
    ComponentsRules findByComponentId(String componentId);
    Boolean existsByComponentId(String componentId);
}
