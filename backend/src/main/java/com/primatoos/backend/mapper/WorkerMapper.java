package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.WorkerResponse;
import com.primatoos.backend.model.Worker;
import org.springframework.stereotype.Component;

@Component
public class WorkerMapper {

    private final UserMapper userMapper;

    public WorkerMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public WorkerResponse toResponse(Worker worker) {
        return new WorkerResponse(
                worker.getId(),
                worker.getName(),
                worker.getFunction(),
                worker.getPhone(),
                worker.isActive(),
                userMapper.toSummary(worker.getUser()),
                worker.getCreatedAt()
        );
    }
}
