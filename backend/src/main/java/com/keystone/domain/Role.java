package com.keystone.domain;

public enum Role {
    DISPATCHER, TECHNICIAN, MANAGER, CUSTOMER;

    public boolean isDispatchOrManager() {
        return this == DISPATCHER || this == MANAGER;
    }
}
