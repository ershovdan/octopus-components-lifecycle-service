package org.octopusden.octopus.lifecycle.db;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(nullable=false, updatable=false)
    private Integer id;

    @Column(unique=true, nullable=false)
    public String name;

    @Column(nullable=false)
    public String type; // global, component, build

    @Column(nullable=false)
    public String putLifecycleStage; // active, maintenance, unsupported

    public String dateFormat;
    public LocalDate minDate;
    public LocalDate maxDate;
    public Integer timeGap;

    public String versionRange;

    public Rule() {}

    public Rule(String name, String type, String putLifecycleStage) {
        this.name = name;
        this.type = type;
        this.putLifecycleStage = putLifecycleStage;
    }

    public Integer getId() {
        return id;
    }
}
