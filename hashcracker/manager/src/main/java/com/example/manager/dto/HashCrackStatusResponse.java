package com.example.manager.dto;

import java.util.Set;

public record HashCrackStatusResponse(
        String status,
        Set<String> data
) {}