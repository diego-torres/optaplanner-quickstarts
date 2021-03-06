/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme.maintenancescheduling.rest;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.acme.maintenancescheduling.domain.MaintenanceJobAssignment;
import org.acme.maintenancescheduling.domain.MaintenanceSchedule;
import org.acme.maintenancescheduling.persistence.MaintainableUnitRepository;
import org.acme.maintenancescheduling.persistence.MaintenanceCrewRepository;
import org.acme.maintenancescheduling.persistence.MaintenanceJobAssignmentRepository;
import org.acme.maintenancescheduling.persistence.MutuallyExclusiveJobsRepository;
import org.acme.maintenancescheduling.persistence.TimeGrainRepository;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;

import io.quarkus.panache.common.Sort;

@Path("/schedule")
public class MaintenanceScheduleResource {

    public static final Long SINGLETON_SCHEDULE_ID = 1L;

    @Inject
    MaintainableUnitRepository maintainableUnitRepository;
    @Inject
    MaintenanceCrewRepository maintenanceCrewRepository;
    @Inject
    MaintenanceJobAssignmentRepository maintenanceJobAssignmentRepository;
    @Inject
    MutuallyExclusiveJobsRepository mutuallyExclusiveJobsRepository;
    @Inject
    TimeGrainRepository timeGrainRepository;

    @Inject
    SolverManager<MaintenanceSchedule, Long> solverManager;
    @Inject
    ScoreManager<MaintenanceSchedule, HardSoftScore> scoreManager;

    // To try, open http://localhost:8080/schedule
    @GET
    public MaintenanceSchedule getSchedule() {
        // Get the solver status before loading the solution
        // to avoid the race condition that the solver terminates between them
        SolverStatus solverStatus = getSolverStatus();
        MaintenanceSchedule solution = findById(SINGLETON_SCHEDULE_ID);
        scoreManager.updateScore(solution); // Sets the score
        solution.setSolverStatus(solverStatus);
        return solution;
    }

    public SolverStatus getSolverStatus() {
        return solverManager.getSolverStatus(SINGLETON_SCHEDULE_ID);
    }

    @POST
    @Path("solve")
    public void solve() {
        solverManager.solveAndListen(SINGLETON_SCHEDULE_ID,
                this::findById,
                this::save);
    }

    @POST
    @Path("stopSolving")
    public void stopSolving() {
        solverManager.terminateEarly(SINGLETON_SCHEDULE_ID);
    }

    @Transactional
    protected MaintenanceSchedule findById(Long id) {
        if (!SINGLETON_SCHEDULE_ID.equals(id)) {
            throw new IllegalStateException("There is no schedule with id (" + id + ").");
        }

        return new MaintenanceSchedule(
                maintainableUnitRepository.listAll(Sort.by("unitName").and("id")),
                mutuallyExclusiveJobsRepository.listAll(Sort.by("id")),
                maintenanceCrewRepository.listAll(Sort.by("crewName").and("id")),
                timeGrainRepository.listAll(Sort.by("grainIndex").and("id")),
                maintenanceJobAssignmentRepository.listAll(Sort.by("id")));
    }

    @Transactional
    protected void save(MaintenanceSchedule schedule) {
        for (MaintenanceJobAssignment job : schedule.getMaintenanceJobAssignmentList()) {
            // TODO this is awfully naive: optimistic locking causes issues if called by the SolverManager
            MaintenanceJobAssignment persistedJobAssignment = maintenanceJobAssignmentRepository.findById(job.getId());
            persistedJobAssignment.setStartingTimeGrain(job.getStartingTimeGrain());
            persistedJobAssignment.setAssignedCrew(job.getAssignedCrew());
        }
    }
}
