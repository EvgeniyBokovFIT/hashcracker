package com.example.manager.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "tasks")
public class CrackEntity {
    @Id
    String id = UUID.randomUUID().toString();
    RequestStatus requestStatus = RequestStatus.IN_PROGRESS;
    LocalDateTime requestTime;
    Set<String> results = new HashSet<>();
    Integer workersAnswered = 0;
}
