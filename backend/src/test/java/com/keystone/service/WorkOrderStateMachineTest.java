package com.keystone.service;

import com.keystone.domain.Role;
import com.keystone.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.keystone.domain.WorkOrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests of the lifecycle rules: no Spring, no database. */
class WorkOrderStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "NEW,CANCELLED,DISPATCHER,false", "NEW,CANCELLED,MANAGER,false",
            "ASSIGNED,IN_PROGRESS,TECHNICIAN,true", "ASSIGNED,CANCELLED,DISPATCHER,false",
            "IN_PROGRESS,ON_HOLD,TECHNICIAN,true", "ON_HOLD,IN_PROGRESS,TECHNICIAN,true",
            "IN_PROGRESS,COMPLETED,TECHNICIAN,true", "COMPLETED,CLOSED,MANAGER,false",
            "COMPLETED,IN_PROGRESS,MANAGER,false"})
    void legalTransitionsAreAccepted(String from, String to, String role, boolean assignee) {
        assertDoesNotThrow(() -> WorkOrderStateMachine.check(
                com.keystone.domain.WorkOrderStatus.valueOf(from), com.keystone.domain.WorkOrderStatus.valueOf(to),
                Role.valueOf(role), assignee));
    }

    @ParameterizedTest
    @CsvSource({"NEW,COMPLETED", "NEW,IN_PROGRESS", "NEW,CLOSED", "ASSIGNED,COMPLETED", "IN_PROGRESS,CANCELLED",
            "IN_PROGRESS,CLOSED", "ON_HOLD,COMPLETED", "COMPLETED,CANCELLED", "CLOSED,NEW", "CANCELLED,ASSIGNED"})
    void illegalJumpsAreRejectedWithConflict(String from, String to) {
        ApiException e = assertThrows(ApiException.class, () -> WorkOrderStateMachine.check(
                com.keystone.domain.WorkOrderStatus.valueOf(from), com.keystone.domain.WorkOrderStatus.valueOf(to),
                Role.MANAGER, true));
        assertEquals(409, e.status());
    }

    @Test
    void terminalStatesCannotChangeAtAll() {
        for (var to : com.keystone.domain.WorkOrderStatus.values()) {
            assertEquals(409, assertThrows(ApiException.class,
                    () -> WorkOrderStateMachine.check(CLOSED, to, Role.MANAGER, true)).status());
            assertEquals(409, assertThrows(ApiException.class,
                    () -> WorkOrderStateMachine.check(CANCELLED, to, Role.MANAGER, true)).status());
        }
    }

    @Test
    void technicianCannotCloseAJob() {
        assertEquals(403, assertThrows(ApiException.class,
                () -> WorkOrderStateMachine.check(COMPLETED, CLOSED, Role.TECHNICIAN, true)).status());
    }

    @Test
    void onlyTheAssignedTechnicianCanStart() {
        assertEquals(403, assertThrows(ApiException.class,
                () -> WorkOrderStateMachine.check(ASSIGNED, IN_PROGRESS, Role.TECHNICIAN, false)).status());
        assertEquals(403, assertThrows(ApiException.class,
                () -> WorkOrderStateMachine.check(ASSIGNED, IN_PROGRESS, Role.MANAGER, false)).status());
    }

    @Test
    void customersCanDoNothing() {
        assertEquals(403, assertThrows(ApiException.class,
                () -> WorkOrderStateMachine.check(NEW, CANCELLED, Role.CUSTOMER, false)).status());
    }

    @Test
    void allowedTargetsMatchWhatCheckAccepts() {
        assertEquals(java.util.List.of(IN_PROGRESS, CLOSED), WorkOrderStateMachine.allowedTargets(COMPLETED, Role.MANAGER, false));
        assertTrue(WorkOrderStateMachine.allowedTargets(COMPLETED, Role.TECHNICIAN, true).isEmpty());
        assertTrue(WorkOrderStateMachine.allowedTargets(CLOSED, Role.MANAGER, false).isEmpty());
    }
}
