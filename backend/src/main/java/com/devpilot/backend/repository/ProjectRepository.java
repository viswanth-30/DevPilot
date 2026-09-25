package com.devpilot.backend.repository;

import com.devpilot.backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    /**
     * Finds a project by its GitHub repository URL.
     * Used before connecting a new repository to check for duplicates —
     * the same GitHub URL must not be connected to two different projects.
     *
     * Spring Data JPA auto-implements this from the method name.
     */
    Optional<Project> findByGithubUrl(String githubUrl);
}
