package com.harddisk.module.dashboard.dto;

import lombok.Data;

@Data
public class DashboardStats {
    private long totalDisks;
    private long idleDisks;
    private long inUseDisks;

    private long totalRecords;
    private long outboundRecords;

    private long inboundPendingRecords;
    private long inboundDoneRecords;

    private long monthOutboundCount;
    private long monthInboundCount;

    private long totalViolations;
    private long pendingViolations;
}
