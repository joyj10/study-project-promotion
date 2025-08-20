package com.shop.timesaleservice.repository;

import com.shop.timesaleservice.domain.TimeSale;
import com.shop.timesaleservice.domain.TimeSaleStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TimeSaleRepository extends JpaRepository<TimeSale, Long> {
    @Query("SELECT ts FROM TimeSale ts WHERE ts.startAt <= :now AND ts.endAt > :now AND ts.status = :status")
    Page<TimeSale> findAllByStartAtBeforeAndEndAtAfterAndStatus(
            @Param("now") LocalDateTime now,
            @Param("status") TimeSaleStatus status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TimeSale ts WHERE ts.id = :id")
    Optional<TimeSale> findByIdWithPessimisticLock(@Param("id") Long id);
}
