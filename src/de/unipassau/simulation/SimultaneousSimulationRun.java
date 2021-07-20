package de.unipassau.simulation;

import java.util.List;

class SimultaneousSimulationRun extends SimulationRun {

    /**
     * Carries out simulation periods by running an episode for all firms and then setting these actions
     * (prices or quantities). All remaining firm data is calculated and the Q-Learning algorithms are updated once
     * all actions of a period were observed. Finally, it is checked whether the firm's actions have changed compared
     * to the previous period. If no termination condition is fulfilled, the next period will be simulated.
     */
    @Override
    public void simulate() {
        List<Firm> firms = super.getFirms();

        // Array to store periodical actions of all firms.
        int[] actions = new int[SimulationManager.MARKET_SIZE];

        /*
        Run simulation periods as long as no termination condition is fulfilled:
            - the actual number of periods of this simulation run (numberOfPeriods)
                has to be below its upper limit (maxNumberOfPeriods); and
            - the actual number of converged periods (numberOfConvergedPeriods)
                has to be below its upper limit (minNumberOfConvergedPeriods)
        */
        while (super.getNumberOfPeriods() < SimulationManager.maxNumberOfPeriods
                && super.getNumberOfConvergedPeriods() < SimulationManager.minNumberOfConvergedPeriods) {

            // Run episode for each firm.
            for (int i = 0; i < SimulationManager.MARKET_SIZE; i++) {
                actions[i] = firms.get(i).chooseAction();
            }

            // Set all buffered and possibly updated actions.
            for (int i = 0; i < SimulationManager.MARKET_SIZE; i++) {
                firms.get(i).setAction(actions[i]);
            }

            // Calculate all remaining data and update the Q-Learning once the new actions were observed.
            for (Firm firm : firms){
                firm.calculateData();

                // Do not update the Q-Learning algorithms until every firm has chosen an action at least twice.
                if (super.getNumberOfPeriods() > 0) {
                    firm.updateQlearning();
                }
            }

            // Check if all firms' current actions are equal to their previous ones.
            if (allActionsOfLastTwoPeriodsAreIdentical(firms)) {
                super.setNumberOfConvergedPeriods(super.getNumberOfConvergedPeriods() + 1);
            } else {
                super.setNumberOfConvergedPeriods(0);
            }

            super.setNumberOfPeriods(super.getNumberOfPeriods() + 1);
        }
    }

    /**
     * @return new SimultaneousSimulationRun object
     */
    @Override
    public SimulationRun clone() {
        return new SimultaneousSimulationRun();
    }
}
