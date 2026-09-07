package com.pet.service;

import com.pet.vo.AdminDashboardVO;

public interface AdminDashboardService {
    AdminDashboardVO getDashboard(int days);
}
