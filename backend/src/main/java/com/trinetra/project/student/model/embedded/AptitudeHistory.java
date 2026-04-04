package com.trinetra.project.student.model.embedded;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AptitudeHistory {

    @Field("topic")
    private String topic;

    @Field("score")
    private Double score;

    @Field("timeTaken")
    private Integer timeTaken;

    @Field("type")
    private String type;

    @Field("examId")
    private String examId;

    @Field("attemptedAt")
    private Instant attemptedAt;
}
