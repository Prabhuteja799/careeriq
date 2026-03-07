package com.careeriq.model.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class MatchScoreId implements Serializable {
    private UUID candidateId;
    private UUID jobId;
}
