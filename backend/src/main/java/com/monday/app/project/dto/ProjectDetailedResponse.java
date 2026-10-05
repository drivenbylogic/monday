package com.monday.app.project.dto;

import com.monday.app.project.entity.Milestone;
import com.monday.app.project.entity.Task;
import java.util.List;

public class ProjectDetailedResponse {
    private Milestone nextMilestone;
    private Task nextImpendingTask;
    private List<Milestone> milestonesRoadmap;

    public Milestone getNextMilestone() { return nextMilestone; }
    public void setNextMilestone(Milestone nextMilestone) { this.nextMilestone = nextMilestone; }

    public Task getNextImpendingTask() { return nextImpendingTask; }
    public void setNextImpendingTask(Task nextImpendingTask) { this.nextImpendingTask = nextImpendingTask; }

    public List<Milestone> getMilestonesRoadmap() { return milestonesRoadmap; }
    public void setMilestonesRoadmap(List<Milestone> milestonesRoadmap) { this.milestonesRoadmap = milestonesRoadmap; }
}
