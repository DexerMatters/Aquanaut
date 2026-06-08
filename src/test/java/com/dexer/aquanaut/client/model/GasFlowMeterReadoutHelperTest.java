package com.dexer.aquanaut.client.model;

public final class GasFlowMeterReadoutHelperTest {
    public static void main(String[] args) {
        GasFlowMeterReadoutHelperTest test = new GasFlowMeterReadoutHelperTest();
        test.activePumpShowsAirFlowTitleAndStrength();
        test.inactivePumpShowsInactiveState();
        test.pipeShowsSignedFlowAndNoFlowState();
    }

    private void activePumpShowsAirFlowTitleAndStrength() {
        GasFlowMeterReadoutHelper.Readout readout = GasFlowMeterReadoutHelper.airPump(true, 16);

        assertEquals("AIR PUMP", readout.titleText(), "active title");
        assertEquals("ACTIVE +16", readout.valueText(), "active value");
    }

    private void inactivePumpShowsInactiveState() {
        GasFlowMeterReadoutHelper.Readout readout = GasFlowMeterReadoutHelper.airPump(false, 16);

        assertEquals("AIR PUMP", readout.titleText(), "inactive title");
        assertEquals("INACTIVE", readout.valueText(), "inactive value");
    }

    private void pipeShowsSignedFlowAndNoFlowState() {
        GasFlowMeterReadoutHelper.Readout positive = GasFlowMeterReadoutHelper.airPipe(8);
        assertEquals("AIR PIPE", positive.titleText(), "positive pipe title");
        assertEquals("FLOW +8", positive.valueText(), "positive pipe value");

        GasFlowMeterReadoutHelper.Readout negative = GasFlowMeterReadoutHelper.airPipe(-6);
        assertEquals("AIR PIPE", negative.titleText(), "negative pipe title");
        assertEquals("FLOW -6", negative.valueText(), "negative pipe value");

        GasFlowMeterReadoutHelper.Readout none = GasFlowMeterReadoutHelper.airPipe(0);
        assertEquals("AIR PIPE", none.titleText(), "zero pipe title");
        assertEquals("NO FLOW", none.valueText(), "zero pipe value");
    }

    private void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
