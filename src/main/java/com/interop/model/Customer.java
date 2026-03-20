package com.interop.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record Customer(
        @JsonProperty("customer_id") long customerId,
        String name,
        String email,
        String country,
        @JsonProperty("updated_at") OffsetDateTime updatedAt
) {}
