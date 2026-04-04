package com.trinetra.project.exam.model.embedded;

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
public class ScheduledWindow {

    @Field("start")
    private Instant start;

    @Field("end")
    private Instant end;
}
