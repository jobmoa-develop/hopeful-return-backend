package com.jobmoa.hopefulreturn.dashboard.service;

import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardCalendarResponse;
import com.jobmoa.hopefulreturn.dashboard.model.dto.DashboardRegionStatsResponse;

public interface DashboardService {

    DashboardRegionStatsResponse getRegionStats();

    DashboardCalendarResponse getCalendar(Integer year, Integer month);
}