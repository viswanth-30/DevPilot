package com.devpilot.backend.repository;

import com.devpilot.backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Project entity.
 *
 * By extending JpaRepository<Project, Long>, we automatically get:
 *   - save(entity)         → INSERT or UPDATE
 *   - findById(id)         → SELECT by PK
 *   - findAll()            → SELECT all rows
 *   - deleteById(id)       → DELETE by PK
 *   - existsById(id)       → SELECT count for existence check
 * No SQL or JPQL queries need to be written for standard CRUD.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // Custom query methods will be added here when needed (e.g., for GitHub integration).
}
