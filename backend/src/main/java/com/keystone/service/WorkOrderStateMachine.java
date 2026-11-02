package com.keystone.service;

import com.keystone.domain.Role;
import com.keystone.domain.WorkOrderStatus;
import com.keystone.exception.ApiException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.keystone.domain.Role.DISPATCHER;
import static com.keystone.domain.Role.MANAGER;
import static com.keystone.domain.Role.TECHNICIAN;
import static com.keystone.domain.WorkOrderStatus.*;

/**
 * The governed work-order lifecycle (brief section 07). This is the single place the rules live:
 * which jumps are legal, and which role may make each one. No framework dependencies on purpose,
 * so it is trivially unit-testable.
 */
public final class WorkOrderStateMachine {

    public record Rule(WorkOrderStatus from, WorkOrderStatus to, Set<Role> roles, boolean assigneeOnly, String action) {}

    private static final List<Rule> RULES = List.of(
            new Rule(NEW, ASSIGNED, EnumSet.of(DISPATCHER, MANAGER), false, "assign"),
            new Rule(NEW, CANCELLED, EnumSet.of(DISPATCHER, MANAGER), false, "cancel"),
            new Rule(ASSIGNED, IN_PROGRESS, EnumSet.of(TECHNICIAN), true, "start"),
            new Rule(ASSIGNED, CANCELLED, EnumSet.of(DISPATCHER, MANAGER), false, "cancel"),
            new Rule(IN_PROGRESS, ON_HOLD, EnumSet.of(TECHNICIAN), true, "hold"),
            new Rule(ON_HOLD, IN_PROGRESS, EnumSet.of(TECHNICIAN), true, "resume"),
            new Rule(IN_PROGRESS, COMPLETED, EnumSet.of(TECHNICIAN), true, "complete"),
            new Rule(COMPLETED, IN_PROGRESS, EnumSet.of(MANAGER), false, "reopen"),
            new Rule(COMPLETED, CLOSED, EnumSet.of(MANAGER), false, "close")
    );

    private WorkOrderStateMachine() {}

    public static Optional<Rule> find(WorkOrderStatus from, WorkOrderStatus to) {
        return RULES.stream().filter(r -> r.from() == from && r.to() == to).findFirst();
    }

    /**
     * @throws ApiException.Conflict  (409) the jump is not in the lifecycle at all
     * @throws ApiException.Forbidden (403) the jump exists but this actor may not make it
     */
    public static Rule check(WorkOrderStatus from, WorkOrderStatus to, Role actor, boolean isAssignee) {
        if (from.isTerminal()) {
            throw new ApiException.Conflict("Work order is " + from + " and can no longer change");
        }
        Rule rule = find(from, to).orElseThrow(() ->
                new ApiException.Conflict("Illegal transition: " + from + " -> " + to));
        if (!rule.roles().contains(actor)) {
            throw new ApiException.Forbidden("Role " + actor + " is not allowed to " + rule.action() + " a work order");
        }
        if (rule.assigneeOnly() && !isAssignee) {
            throw new ApiException.Forbidden("Only the assigned technician can " + rule.action() + " this work order");
        }
        return rule;
    }

    /** Targets this actor could move the order to right now (assignment is handled by its own endpoint). */
    public static List<WorkOrderStatus> allowedTargets(WorkOrderStatus from, Role actor, boolean isAssignee) {
        List<WorkOrderStatus> result = new ArrayList<>();
        for (Rule r : RULES) {
            if (r.from() == from && r.to() != ASSIGNED && r.roles().contains(actor) && (!r.assigneeOnly() || isAssignee)) {
                result.add(r.to());
            }
        }
        return result;
    }
}
