package com.mahitotsu.tsukumogami.app.tools.workorder;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkResultRepository extends JpaRepository<WorkResultEntity, UUID> {
    
}
