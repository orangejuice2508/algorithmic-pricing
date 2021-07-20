package de.unipassau.simulation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class SequentialSimulationRun extends SimulationRun {

    /**
     * Carries out simulation periods by running an episode for only one firm in each period and then setting its action
     * (price or quantity). Then all remaining data is calculated for all firms. Finally, it is checked whether the
     * firm's actions have changed compared to the previous period. If no termination condition is fulfilled, the next
     * period will be simulated. The firm's Q-Learning algorithm is updated before running its episode, because then,
     * the previous actions of all other firms can be observed.
     */
    @Override
    public void simulate() {
        List<Firm> firms = super.getFirms();

        // An iterator is necessary to ensure that the firms choose their actions one after another.
        Iterator<Firm> iterator = firms.iterator();

        /*
        Run simulation periods as long as no termination condition is fulfilled:
            - the actual number of periods of this simulation run (numberOfPeriods)
                has to be below its upper limit (maxNumberOfPeriods); and
            - the actual number of converged periods (numberOfConvergedPeriods)
                has to be below its upper limit (minNumberOfConvergedPeriods)
        */
        while (super.getNumberOfPeriods() < SimulationManager.maxNumberOfPeriods
                && super.getNumberOfConvergedPeriods() < SimulationManager.minNumberOfConvergedPeriods) {

            // If iterator.hasNext() is true, then there is still a firm in the "queue" to chose an action.
            if (iterator.hasNext()){
                Firm currentFirm = iterator.next();

                // Do not update the Q-Learning algorithms until every firm has chosen an action at least twice.
                if (! (super.getNumberOfPeriods() < SimulationManager.MARKET_SIZE * 2)) {
                    currentFirm.updateQlearning();
                }

                // Run episode.
                int actionOfCurrentFirm = currentFirm.chooseAction();

                // Set the current firm's updated action.
                currentFirm.setAction(actionOfCurrentFirm);

                /*
                 Set the actions of all other firms according to their last action, because they are not allowed
                 to change their actions in this period
                 */
                List<Firm> otherFirms = new ArrayList<>(firms);
                otherFirms.remove(currentFirm);
                for (Firm otherFirm : otherFirms) {
                    otherFirm.setAction(otherFirm.getAction());
                }

                // Calculate all remaining data.
                for (Firm firm : firms){
                    firm.calculateData();
                }

                // Check if all firms' current actions are equal to their previous ones.
                if (allActionsOfLastTwoPeriodsAreIdentical(firms)){
                    super.setNumberOfConvergedPeriods(super.getNumberOfConvergedPeriods() + 1);
                } else {
                    super.setNumberOfConvergedPeriods(0);
                }

                super.setNumberOfPeriods(super.getNumberOfPeriods() + 1);
            }
            // If iterator.hasNext() is false, then start over with the first firm.
            else {
                iterator = firms.iterator();
            }
        }
    }

    /**
     * @return new SequentialSimulationRun object
     */
    @Override
    public SimulationRun clone() {
        return new SequentialSimulationRun();
    }
}
