package com.aloha.project.service.inter.tasks;

import com.aloha.project.domain.users.Users;
import com.aloha.project.dto.TaskAssignRequest;

public interface TaskService {
    
    public boolean assignTask(Users leader, TaskAssignRequest request);
}
