package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    List<Opportunity> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    Page<Opportunity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    List<Opportunity> findByStageOrderByCreatedAtDesc(String stage);

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    Page<Opportunity> findByStageOrderByCreatedAtDesc(String stage, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    Page<Opportunity> findByAssignedToUsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    Page<Opportunity> findByStageAndAssignedToUsernameOrderByCreatedAtDesc(String stage, String username, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    @Query("""
            SELECT o FROM Opportunity o
            WHERE (:stage IS NULL OR :stage = '' OR o.stage = :stage)
            AND (:assignedUsername IS NULL OR :assignedUsername = ''
                OR LOWER(o.assignedTo.username) = LOWER(:assignedUsername))
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(o.opportunityCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(o.customer.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(o.assignedTo.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(o.assignedTo.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY o.createdAt DESC
            """)
    Page<Opportunity> searchPipeline(
            @Param("keyword") String keyword,
            @Param("stage") String stage,
            @Param("assignedUsername") String assignedUsername,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    @Query("SELECT o FROM Opportunity o WHERE o.id = :id")
    Optional<Opportunity> findDetailById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "assignedTo"})
    Optional<Opportunity> findByCustomerId(Long customerId);

    long countByStage(String stage);

    long countByStageAndAssignedToUsername(String stage, String username);
    List<Opportunity> findByCustomerId(Long customerId);
}
