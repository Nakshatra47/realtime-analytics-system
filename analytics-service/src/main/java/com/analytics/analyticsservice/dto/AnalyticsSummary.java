package com.analytics.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsSummary {
    private Long totalOrders;
    private Long confirmedOrders;
    private Long cancelledOrders;
    private Long stockFailedOrders;
    private Double totalRevenue;
}