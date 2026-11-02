package com.keystone.domain;

import java.util.EnumSet;
import java.util.Set;

public enum WorkOrderStatus {
    NEW, ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED, CLOSED, CANCELLED;

    /** Work that still needs doing; these are the states SLA timers run against. */
    public static final Set<WorkOrderStatus> ACTIVE = EnumSet.of(NEW, ASSIGNED, IN_PROGRESS, ON_HOLD);
    /** Everything shown on the dispatch board. */
    public static final Set<WorkOrderStatus> BOARD = EnumSet.of(NEW, ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED);
    /** States in which parts and time may still be logged. */
    public static final Set<WorkOrderStatus> LOGGABLE = EnumSet.of(IN_PROGRESS, ON_HOLD, COMPLETED);

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
