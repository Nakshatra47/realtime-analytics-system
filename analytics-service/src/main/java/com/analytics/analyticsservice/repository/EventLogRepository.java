package com.analytics.analyticsservice.repository;

import com.analytics.analyticsservice.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {

    @Query("SELECT SUM(e.price) FROM EventLog e WHERE e.eventType = 'CONFIRMED'")
    Double getTotalRevenue();

    @Query("SELECT COUNT(e) FROM EventLog e WHERE e.eventType = :eventType")
    Long countByEventType(String eventType);
}