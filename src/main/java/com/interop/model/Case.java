package com.interop.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record Case(
        @JsonProperty("case_id") long caseId,
        @JsonProperty("customer_id") long customerId,
        String title,
        String description,
        String status,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {}
