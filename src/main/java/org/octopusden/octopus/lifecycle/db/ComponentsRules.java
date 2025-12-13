package org.octopusden.octopus.lifecycle.db;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class ComponentsRules {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)

    @Column(nullable=false)
    public String componentId;

    @ManyToMany(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
        },
        fetch = FetchType.EAGER
    )
    @JoinTable(
            name = "component_rule",
            joinColumns = @JoinColumn(name = "components_rules_id"),
            inverseJoinColumns = @JoinColumn(name = "rule_id")
    )
    @OrderColumn(name = "position")
    public List<Rule> rules;

    public ComponentsRules(String componentId) {
        this.componentId = componentId;
        this.rules = new ArrayList<>();
    }

    public ComponentsRules() {
        this.rules = new ArrayList<>();
    }
}
