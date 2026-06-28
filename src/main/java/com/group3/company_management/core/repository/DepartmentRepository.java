package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Department;
import com.group3.company_management.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    @Query("""
select d
from Department d
where d.isDeleted = false
and (:keyword = '' or lower(d.name) like lower(concat('%', :keyword, '%'))
    or lower(d.code) like lower(concat('%', :keyword, '%')))
and (d.status = :filter or :filter = 'all')
""")
    Page<Department> search(
            @Param("keyword") String keyword,
            @Param("filter") String filter,
            Pageable pageable
    );
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
    Optional<Department> findByName(String name);
    @Query("""
select d
from Department d
where d.isDeleted = false
""")
    Page<Department> findAllNotDeleted(Pageable pageable);
    Optional<Department> findByIdAndIsDeletedFalse(Long id);
}