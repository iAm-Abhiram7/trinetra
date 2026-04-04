package com.trinetra.project.exam.model;

import com.trinetra.project.exam.model.embedded.Question;
import com.trinetra.project.exam.model.embedded.ScheduledWindow;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "exams")
public class Exam {

    @Id
    private String id;

    @Field("title")
    private String title;

    @Field("type")
    private String type;

    @Field("createdBy")
    private String createdBy;

    @Field("state")
    private String state;

    @Field("college")
    private String college;

    @Field("branch")
    private String branch;

    @Field("yearOfPassing")
    private Integer yearOfPassing;

    @Field("scheduledWindow")
    private ScheduledWindow scheduledWindow;

    @Field("durationMinutes")
    private Integer durationMinutes;

    @Field("totalQuestions")
    private Integer totalQuestions;

    @Field("totalMarks")
    private Integer totalMarks;

    @Field("negativeMarking")
    private Double negativeMarking;

    @Field("shuffleQuestions")
    private Boolean shuffleQuestions;

    @Field("shuffleOptions")
    private Boolean shuffleOptions;

    @Field("isPublished")
    private Boolean isPublished;

    @Field("questions")
    private List<Question> questions;

    @Field("isDeleted")
    private Boolean isDeleted;

    @Field("deletedAt")
    private Instant deletedAt;

    @Field("deletedBy")
    private String deletedBy;

    @Field("createdAt")
    private Instant createdAt;
}
