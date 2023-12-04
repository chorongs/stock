package com.dayone.persist;

import com.dayone.persist.entity.DividendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DividendRapository extends JpaRepository<DividendEntity, Long> {

    // DividendEntity내에서 companyId와 일치하는 엔티티들을 찾아서 List 형태로 반환
    List<DividendEntity> findAllByCompanyId(Long companyId);

    @Transactional
    void deleteAllByCompanyId(Long companyId);

    boolean existsByCompanyIdAndDate(Long companyId, LocalDateTime date);
}
