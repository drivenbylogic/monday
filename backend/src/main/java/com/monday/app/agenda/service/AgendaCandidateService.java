package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service that identifies and ranks eligible recommendation candidates from domain data.
 */
@Service
public class AgendaCandidateService {

    public List<TaskSummaryDto> rankCandidateTasks(List<TaskSummaryDto> overdue,
                                                   List<TaskSummaryDto> dueToday,
                                                   List<TaskSummaryDto> highPriority) {
        List<TaskSummaryDto> combined = new ArrayList<>();
        if (overdue != null) combined.addAll(overdue);
        if (dueToday != null) combined.addAll(dueToday);
        if (highPriority != null) {
            for (TaskSummaryDto hp : highPriority) {
                if (combined.stream().noneMatch(t -> t.id().equals(hp.id()))) {
                    combined.add(hp);
                }
            }
        }

        // Rank by priority weight and due date
        combined.sort(Comparator.comparing(this::priorityWeight).reversed()
                .thenComparing(TaskSummaryDto::dueAt, Comparator.nullsLast(Comparator.naturalOrder())));

        return combined;
    }

    private int priorityWeight(TaskSummaryDto task) {
        if (task.priority() == null) return 1;
        return switch (task.priority().toUpperCase()) {
            case "CRITICAL", "URGENT" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 1;
        };
    }
}
