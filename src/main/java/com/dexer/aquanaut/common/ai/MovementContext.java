package com.dexer.aquanaut.common.ai;

import com.dexer.aquanaut.common.entity.BaseFishEntity;

public class MovementContext {
    private final BaseFishEntity fish;
    private final MovementState state;

    private double acceleration;
    private double maxSpeed;

    public MovementContext(BaseFishEntity fish, MovementState state) {
        this.fish = fish;
        this.state = state;
    }

    public BaseFishEntity fish() {
        return this.fish;
    }

    public MovementState state() {
        return this.state;
    }

    public void setMotion(double acceleration, double maxSpeed) {
        this.acceleration = acceleration;
        this.maxSpeed = maxSpeed;
    }

    public double acceleration() {
        return this.acceleration;
    }

    public double maxSpeed() {
        return this.maxSpeed;
    }
}
