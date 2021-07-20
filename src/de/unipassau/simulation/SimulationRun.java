package de.unipassau.simulation;

import java.util.ArrayList;
import java.util.List;

abstract class SimulationRun {

    private final List<Firm> firms;         // Array to store firms of the market
    private int numberOfPeriods;            // Number of elapsed periods
    private int numberOfConvergedPeriods;   // Number of periods with unchanged actions (prices/quantities)

    /**
     * Constructor of a SimulationRun object
     */
    SimulationRun() {
        firms = new ArrayList<>();
    }

    /**
     * Carries out simulation periods according to the timing of interaction across the firms.
     */
    public abstract void simulate();

    /**
     * @return clone of a SimulationRun object
     */
    public abstract SimulationRun clone();

    /**
     * @return all firms of this simulation run
     */
    public List<Firm> getFirms() {
        // Initialize firms first if list is still empty
        if (firms.isEmpty()) {
            // Create the number of firms according to the MARKET_SIZE and store them in the List
            for (int i = 0; i < SimulationManager.MARKET_SIZE; i++) {
                firms.add(new Firm(this,i));
            }
        }
        return firms;
    }

    /**
     * @return number of elapsed periods
     */
    public int getNumberOfPeriods() {
        return numberOfPeriods;
    }

    /**
     * @param numberOfPeriods number of elapsed periods to be set.
     */
    public void setNumberOfPeriods(int numberOfPeriods) {
        this.numberOfPeriods = numberOfPeriods;
    }

    /**
     * @return number of periods with unchanged actions of all firms
     */
    public int getNumberOfConvergedPeriods() {
        return numberOfConvergedPeriods;
    }

    /**
     *
     * @param numberOfConvergedPeriods  number of periods with unchanged actions of all firms to be set.
     */
    public void setNumberOfConvergedPeriods(int numberOfConvergedPeriods) {
        this.numberOfConvergedPeriods = numberOfConvergedPeriods;
    }

    /**
     * Calculates the mean price of all firms on the market (= market price).
     *
     * @return market price rounded to 2 decimals.
     */
    double getMeanPrice() {
        List<Double> allPrices = new ArrayList<>();

        for (Firm firm : firms) {
            List<Double> pricesOfFirm = firm.getPrices();
            allPrices.addAll(pricesOfFirm.subList(pricesOfFirm.size() - SimulationManager.sizeOfExaminationInterval, pricesOfFirm.size()));
        }

        return Calculation.round(Calculation.getMean(allPrices), 2);
    }

    /**
     * Calculates the standard deviation of the market price.
     *
     * @return standard deviation of market price rounded to 2 decimals.
     */
    double getSDPrice() {
        ArrayList<Double> allPrices = new ArrayList<>();

        for (Firm firm : firms) {
            List<Double> pricesOfFirm = firm.getPrices();
            allPrices.addAll(pricesOfFirm.subList(pricesOfFirm.size() - SimulationManager.sizeOfExaminationInterval, pricesOfFirm.size()));
        }

        return Calculation.round(Calculation.getSD(allPrices), 2);
    }

    /**
     * Calculates the mean quantity of all firms on the market (= market quantity).
     *
     * @return market quantity rounded to 2 decimals.
     */
    double getMeanQuantity() {
        ArrayList<Double> allQuantities = new ArrayList<>();

        for (Firm firm : firms) {
            List<Double> quantitiesOfFirm = firm.getQuantities();
            allQuantities.addAll(quantitiesOfFirm.subList(quantitiesOfFirm.size() - SimulationManager.sizeOfExaminationInterval, quantitiesOfFirm.size()));
        }

        return Calculation.round(Calculation.getMean(allQuantities), 2);
    }

    /**
     * Calculates the mean profit of all firms on the market (= market profit).
     *
     * @return market profit rounded to 2 decimals.
     */
    double getMeanProfit() {
        ArrayList<Double> allProfits = new ArrayList<>();

        for (Firm firm : firms) {
            List<Double> profitsOfFirm = firm.getProfits();
            allProfits.addAll(profitsOfFirm.subList(profitsOfFirm.size() - SimulationManager.sizeOfExaminationInterval, profitsOfFirm.size()));
        }

        return Calculation.round(Calculation.getMean(allProfits), 2);
    }

    /**
     * Calculates the degree of tacit collusion as the ratio of the market price to the price in Nash
     * equilibrium and the one in JPM equilibrium. Nash and JPM equilibria are retrieved for each setting.
     *
     * @return degree of tacit collusion rounded to 3 decimals.
     */
    double getDegreeOfTacitCollusion() {
        double price = getMeanPrice();
        Competition competition = SimulationManager.COMPETITION_TYPE.getCompetition();
        double priceInNashEquilibrium = competition.getPriceInNashEquilibrium(SimulationManager.MARKET_SIZE);
        double priceInJpmEquilibrium = competition.getPriceInJpmEquilibrium();

        return Calculation.round((price - priceInNashEquilibrium) /
                (priceInJpmEquilibrium - priceInNashEquilibrium), 3);
    }

    /**
     * Compares actions of all firms in the current and the previous period. If a action is different for at
     * least one firm, false is returned and the method terminates. Otherwise (meaning
     * current variables are identical to the ones of the previous period for all firms), true is returned.
     *
     * @return true if current actions are identical to the ones of the previous period for all firms.
     */
    boolean allActionsOfLastTwoPeriodsAreIdentical(List<Firm> firms) {
        for (Firm firm : firms) {
            // One false is enough to interrupt, since periods have to be identical for all firms
            if (!firm.actionsOfLastTwoPeriodsAreIdentical()) {
                return false;
            }
        }

        return true;
    }


    enum Timing {
        /*
        Create a new SimulationRun object, when retrieving the enum.
        */
        SIMULTANEOUS(new SimultaneousSimulationRun()), SEQUENTIAL(new SequentialSimulationRun());

        private final SimulationRun simulationRun;

        /**
         * Creates a MarketTiming.Type object from the SimulationRun.
         *
         * @param simulationRun underlying SimulationRun object
         */
        Timing(SimulationRun simulationRun) {
            this.simulationRun = simulationRun;
        }

        /**
         * @return underlying SimulationRun object.
         */
        public SimulationRun getSimulationRun() {
            return simulationRun;
        }
    }
}
