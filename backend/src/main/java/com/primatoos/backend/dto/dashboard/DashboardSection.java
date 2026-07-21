package com.primatoos.backend.dto.dashboard;

import java.util.List;

public record DashboardSection<T>(long count, List<T> items) {
}
