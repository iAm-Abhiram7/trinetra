package com.trinetra.project.student.service;

import com.trinetra.project.student.model.embedded.AptitudeHistory;

public interface AptitudeHistoryService {

    /**
     * Appends a new aptitude history entry while keeping only the latest 50 records.
     */
    void appendToHistory(String studentId, AptitudeHistory entry);
}
