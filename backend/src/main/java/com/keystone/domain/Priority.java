package com.keystone.domain;

public enum Priority {
    LOW(72), MEDIUM(24), HIGH(8), URGENT(4);

    private final int slaHours;

    Priority(int slaHours) {
        this.slaHours = slaHours;
    }

    /** Hours allowed from creation until the work order must be resolved. */
    public int slaHours() {
        return slaHours;
    }
}
