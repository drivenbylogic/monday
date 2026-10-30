package com.monday.app.agenda.service;

import com.monday.app.administration.repository.AdministrationImportantDateRepository;
import com.monday.app.administration.repository.AdministrationObligationRepository;
import com.monday.app.agenda.dto.*;
import com.monday.app.event.repository.EventRepository;
import com.monday.app.intelligence.repository.ArticleRepository;
import com.monday.app.project.entity.Task;
import com.monday.app.project.repository.MilestoneRepository;
import com.monday.app.project.repository.ProjectRepository;
import com.monday.app.project.repository.TaskRepository;
import com.monday.app.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Validates AI or candidate generated recommendations against actual database records
 * and scheduling constraints. Ensures no hallucinated IDs or conflicting time blocks across all modules.
 */
@Service
public class AgendaValidationService {

    private static final Logger log = LoggerFactory.getLogger(AgendaValidationService.class);

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final EventRepository eventRepository;
    private final VehicleRepository vehicleRepository;
    private final AdministrationObligationRepository adminObligationRepository;
    private final AdministrationImportantDateRepository adminDateRepository;
    private final ArticleRepository articleRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @Autowired
    public AgendaValidationService(TaskRepository taskRepository,
                                   ProjectRepository projectRepository,
                                   MilestoneRepository milestoneRepository,
                                   EventRepository eventRepository,
                                   VehicleRepository vehicleRepository,
                                   AdministrationObligationRepository adminObligationRepository,
                                   AdministrationImportantDateRepository adminDateRepository,
                                   ArticleRepository articleRepository,
                                   NamedParameterJdbcTemplate jdbc) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.eventRepository = eventRepository;
        this.vehicleRepository = vehicleRepository;
        this.adminObligationRepository = adminObligationRepository;
        this.adminDateRepository = adminDateRepository;
        this.articleRepository = articleRepository;
        this.jdbc = jdbc;
    }

    public AgendaValidationService(TaskRepository taskRepository,
                                   ProjectRepository projectRepository,
                                   MilestoneRepository milestoneRepository,
                                   EventRepository eventRepository) {
        this(taskRepository, projectRepository, milestoneRepository, eventRepository, null, null, null, null, null);
    }

    public List<AgendaRecommendationDto> validateRecommendations(List<AgendaRecommendationDto> recommendations,
                                                                 List<EventSummaryDto> existingEvents) {
        if (recommendations == null || recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        List<AgendaRecommendationDto> validated = new ArrayList<>();
        List<SuggestedTimeBlockDto> approvedBlocks = new ArrayList<>();

        for (AgendaRecommendationDto rec : recommendations) {
            // 1. Validate and filter source references
            List<SourceReferenceDto> validRefs = new ArrayList<>();
            boolean taskIsAlreadyDone = false;

            if (rec.sourceReferences() != null) {
                for (SourceReferenceDto ref : rec.sourceReferences()) {
                    if (ref.id() == null) continue;

                    if ("TASK".equalsIgnoreCase(ref.type()) || "CAREER_TASK".equalsIgnoreCase(ref.type())) {
                        Optional<Task> optTask = taskRepository.findById(ref.id());
                        if (optTask.isPresent()) {
                            Task t = optTask.get();
                            if ("COMPLETED".equalsIgnoreCase(t.getStatus()) || "CANCELLED".equalsIgnoreCase(t.getStatus())) {
                                log.warn("Recommendation references already completed/cancelled task: {}", t.getId());
                                taskIsAlreadyDone = true;
                            } else {
                                validRefs.add(new SourceReferenceDto(ref.type(), t.getId(), t.getTitle()));
                            }
                        } else {
                            log.warn("Discarding fabricated task ID reference: {}", ref.id());
                        }
                    } else if ("PROJECT".equalsIgnoreCase(ref.type())) {
                        projectRepository.findById(ref.id()).ifPresent(p -> {
                            if (!"COMPLETED".equalsIgnoreCase(p.getStatus())) {
                                validRefs.add(new SourceReferenceDto("PROJECT", p.getId(), p.getName()));
                            }
                        });
                    } else if ("MILESTONE".equalsIgnoreCase(ref.type()) || "CAREER_MILESTONE".equalsIgnoreCase(ref.type())) {
                        milestoneRepository.findById(ref.id()).ifPresent(m -> {
                            if (!"COMPLETED".equalsIgnoreCase(m.getStatus())) {
                                validRefs.add(new SourceReferenceDto(ref.type(), m.getId(), m.getName()));
                            }
                        });
                    } else if ("EVENT".equalsIgnoreCase(ref.type())) {
                        eventRepository.findById(ref.id()).ifPresent(e -> {
                            if (!"CANCELLED".equalsIgnoreCase(e.getStatus())) {
                                validRefs.add(new SourceReferenceDto("EVENT", e.getId(), e.getTitle()));
                            }
                        });
                    } else if ("VEHICLE".equalsIgnoreCase(ref.type())) {
                        if (vehicleRepository != null) {
                            vehicleRepository.findById(ref.id()).ifPresent(v -> {
                                if (!"INACTIVE".equalsIgnoreCase(v.getStatus())) {
                                    validRefs.add(new SourceReferenceDto("VEHICLE", v.getId(), v.getName()));
                                }
                            });
                        } else {
                            validRefs.add(ref);
                        }
                    } else if ("ADMIN_OBLIGATION".equalsIgnoreCase(ref.type())) {
                        if (adminObligationRepository != null) {
                            adminObligationRepository.findById(ref.id()).ifPresent(o -> {
                                if (!"PAID".equalsIgnoreCase(o.getStatus()) && !"CANCELLED".equalsIgnoreCase(o.getStatus())) {
                                    validRefs.add(new SourceReferenceDto("ADMIN_OBLIGATION", o.getId(), o.getName()));
                                }
                            });
                        } else {
                            validRefs.add(ref);
                        }
                    } else if ("ADMIN_DATE".equalsIgnoreCase(ref.type())) {
                        if (adminDateRepository != null) {
                            adminDateRepository.findById(ref.id()).ifPresent(d -> {
                                validRefs.add(new SourceReferenceDto("ADMIN_DATE", d.getId(), d.getTitle()));
                            });
                        } else {
                            validRefs.add(ref);
                        }
                    } else if ("ARTICLE".equalsIgnoreCase(ref.type())) {
                        if (articleRepository != null) {
                            articleRepository.findById(ref.id()).ifPresent(a -> {
                                validRefs.add(new SourceReferenceDto("ARTICLE", a.getId(), a.getTitle()));
                            });
                        } else {
                            validRefs.add(ref);
                        }
                    } else if ("COMMITMENT".equalsIgnoreCase(ref.type())) {
                        if (jdbc != null) {
                            try {
                                List<String> names = jdbc.query(
                                        "SELECT name FROM finance_commitments WHERE id = :id AND status != 'PAID'",
                                        new MapSqlParameterSource("id", ref.id()),
                                        (rs, rowNum) -> rs.getString("name"));
                                if (!names.isEmpty()) {
                                    validRefs.add(new SourceReferenceDto("COMMITMENT", ref.id(), names.get(0)));
                                }
                            } catch (Exception e) {
                                validRefs.add(ref);
                            }
                        } else {
                            validRefs.add(ref);
                        }
                    } else {
                        validRefs.add(ref);
                    }
                }
            }

            // Skip recommendation if it was specifically for a task that is already finished
            if (taskIsAlreadyDone && validRefs.isEmpty()) {
                continue;
            }

            // 2. Validate suggested start and end time windows against existing events and approved blocks
            Instant validatedStart = rec.suggestedStartAt();
            Instant validatedEnd = rec.suggestedEndAt();

            if (validatedStart != null && validatedEnd != null) {
                if (!validatedEnd.isAfter(validatedStart) ||
                        conflictsWithEvents(validatedStart, validatedEnd, existingEvents) ||
                        conflictsWithBlocks(validatedStart, validatedEnd, approvedBlocks)) {
                    log.info("Suggested time window [{}, {}] for '{}' conflicts or is invalid; clearing time suggestion.",
                            validatedStart, validatedEnd, rec.title());
                    validatedStart = null;
                    validatedEnd = null;
                } else {
                    UUID taskId = validRefs.stream()
                            .filter(r -> "TASK".equalsIgnoreCase(r.type()))
                            .map(SourceReferenceDto::id)
                            .findFirst().orElse(null);

                    approvedBlocks.add(new SuggestedTimeBlockDto(
                            validatedStart,
                            validatedEnd,
                            rec.title(),
                            rec.description(),
                            taskId
                    ));
                }
            }

            validated.add(new AgendaRecommendationDto(
                    rec.id() != null ? rec.id() : "rec-" + (validated.size() + 1),
                    rec.title() != null ? rec.title() : "Priority Outcome",
                    rec.description() != null ? rec.description() : "",
                    rec.priority() != null ? rec.priority().toUpperCase() : "MEDIUM",
                    rec.reason() != null ? rec.reason() : "High impact outcome for today.",
                    rec.nextAction() != null ? rec.nextAction() : "Take initial step.",
                    validRefs,
                    rec.suggestedDurationMinutes() != null ? rec.suggestedDurationMinutes() : 60,
                    validatedStart,
                    validatedEnd,
                    rec.confidence() != null ? rec.confidence() : 0.9,
                    rec.feedbackStatus()
            ));

            // Cap at 3 top priorities as specified in Section 7.1
            if (validated.size() >= 3) {
                break;
            }
        }

        return validated;
    }

    public List<SuggestedTimeBlockDto> validateSuggestedBlocks(List<SuggestedTimeBlockDto> blocks,
                                                               List<EventSummaryDto> existingEvents) {
        if (blocks == null) return Collections.emptyList();
        List<SuggestedTimeBlockDto> valid = new ArrayList<>();

        for (SuggestedTimeBlockDto b : blocks) {
            if (b.startAt() == null || b.endAt() == null || !b.endAt().isAfter(b.startAt())) {
                continue;
            }
            if (conflictsWithEvents(b.startAt(), b.endAt(), existingEvents) ||
                    conflictsWithBlocks(b.startAt(), b.endAt(), valid)) {
                continue;
            }
            valid.add(b);
        }
        return valid;
    }

    private boolean conflictsWithEvents(Instant start, Instant end, List<EventSummaryDto> events) {
        if (events == null) return false;
        for (EventSummaryDto e : events) {
            if (e.allDay() || e.startAt() == null || e.endAt() == null) continue;
            // Overlap check: start < e.endAt && e.startAt < end
            if (start.isBefore(e.endAt()) && e.startAt().isBefore(end)) {
                return true;
            }
        }
        return false;
    }

    private boolean conflictsWithBlocks(Instant start, Instant end, List<SuggestedTimeBlockDto> blocks) {
        if (blocks == null) return false;
        for (SuggestedTimeBlockDto b : blocks) {
            if (start.isBefore(b.endAt()) && b.startAt().isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}
