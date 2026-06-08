package com.dexer.aquanaut.client.model;

import net.minecraft.network.chat.Component;

public final class GasFlowMeterReadoutHelper {
    private GasFlowMeterReadoutHelper() {
    }

    public static Readout airPump(boolean active, int flowStrength) {
        if (!active) {
            return new Readout(
                    Component.translatable("hud.aquanaut.gas_flow_meter.air_pump"),
                    Component.translatable("hud.aquanaut.gas_flow_meter.inactive"));
        }
        return new Readout(
                Component.translatable("hud.aquanaut.gas_flow_meter.air_pump"),
                Component.translatable("hud.aquanaut.gas_flow_meter.active", signed(Math.max(0, flowStrength))));
    }

    public static Readout airPipe(int flowStrength) {
        if (flowStrength == 0) {
            return new Readout(
                    Component.translatable("hud.aquanaut.gas_flow_meter.air_pipe"),
                    Component.translatable("hud.aquanaut.gas_flow_meter.no_flow"));
        }
        return new Readout(
                Component.translatable("hud.aquanaut.gas_flow_meter.air_pipe"),
                Component.translatable("hud.aquanaut.gas_flow_meter.flow", signed(flowStrength)));
    }

    public static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    public record Readout(Component title, Component value) {
        public String titleText() {
            return title.getString();
        }

        public String valueText() {
            return value.getString();
        }
    }
}
