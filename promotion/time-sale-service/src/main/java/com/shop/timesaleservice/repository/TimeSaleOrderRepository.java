package com.shop.timesaleservice.repository;

import com.shop.timesaleservice.domain.TimeSaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSaleOrderRepository extends JpaRepository<TimeSaleOrder, Long> {
}
