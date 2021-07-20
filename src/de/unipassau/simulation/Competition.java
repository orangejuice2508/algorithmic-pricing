package de.unipassau.simulation;

import java.util.LinkedList;
import java.util.List;

abstract class Competition {

    // Model parameter
    static final double omega = 100;
    static final double lambda = 1.0;
    static final double theta = (2.0 / 3.0);

    // Variables
    private double price;
    private double quantity;

    // Lists to store the variables over the time.
    private final List<Double> prices = new LinkedList<>();
    private final List<Double> quantities = new LinkedList<>();
    private final List<Double> profits = new LinkedList<>();

    // Relevant constants for scaling coefficients
    static final double maxProfitInNashEquilibrium = (Math.pow(omega, 2)) / (lambda * Math.pow(2 + theta, 2));
    static final double fixedJpmEquilibrium = 50;

    /**
     * @return price or quantity, depending on the type of competition.
     */
    public abstract int getIndependentVariable();

    /**
     * @param independentVariable price or quantity to be set and added to the corresponding list,
     *                            depending on the type of competition.
     */
    public abstract void setIndependentVariable(int independentVariable);

    /**
     * @return price or quantity depending on the type of competition.
     */
    public abstract double getDependentVariable();

    /**
     * @param dependentVariable price or quantity to be set and added to the corresponding list,
     *                          depending on the type of competition.
     */
    public abstract void setDependentVariable(double dependentVariable);

    /**
     * Calculates either the price or quantity, depending on the type of competition.
     *
     * @param ownIndependentVariable price or quantity, depending on the type of competition.
     * @param independentVariablesOfOtherFirms  prices or quantities of the other firms, depending on the type of
     *                                          competition.
     */
    public abstract double calculateDependentVariable(Integer ownIndependentVariable,
                                                      Integer[] independentVariablesOfOtherFirms);

    /**
     * Calculates the profit depending on price, quantity and number of all firms.
     *
     * @param independentVariable price or quantity, depending on the type of competition.
     * @param dependentVariable quantity or price, depending on the type of competition.
     * @param numberOfFirms number of all competing firms
     * @return profit of the firm
     */
    public abstract double calculateProfit(double independentVariable, double dependentVariable, int numberOfFirms);

    /**
     * See {@link #actionsOfLastTwoPeriodsAreIdentical(List)}.
     *
     * @return true, if the firm's price/quantity in the current period is identical to the previous period.
     */
    public abstract boolean actionsOfLastTwoPeriodsAreIdentical();

    /**
     * @param numberOfFirms number of all competing firms
     * @return the price in the Nash equilibrium for the corresponding number of all competing firms
     */
    public abstract double getPriceInNashEquilibrium(int numberOfFirms);

    /**
     * @param numberOfFirms number of all competing firms
     * @return the profit in the Nash equilibrium for the corresponding number of all competing firms
     */
    public abstract double getProfitInNashEquilibrium(int numberOfFirms);

    /**
     * @return clone of an Competition object
     */
    public abstract Competition clone();

    /**
     * @return price of the firm.
     */
    double getPrice() {
        return price;
    }

    /**
     * @return prices of the firm.
     */
    public List<Double> getPrices() {
        return prices;
    }

    /**
     * @param price price to be set and added to the price list.
     */
    void setPrice(double price) {
        this.price = price;
        prices.add(price);

        // Remove first entry of the list if its size exceeds the examination interval.
        if (prices.size() > SimulationManager.sizeOfExaminationInterval) {
            prices.remove(0);
        }
    }

    /**
     * @return quantity of the firm.
     */
    double getQuantity() {
        return quantity;
    }

    /**
     * @param quantity quantity to be set and added to the quantity list.
     */
    void setQuantity(double quantity) {
        this.quantity = quantity;
        quantities.add(quantity);

        // Remove first entry of the list if its size exceeds the examination interval.
        if (quantities.size() > SimulationManager.sizeOfExaminationInterval) {
            quantities.remove(0);
        }
    }

    /**
     * @return quantities of the firm.
     */
    public List<Double> getQuantities() {
        return quantities;
    }

    /**
     * @param profit profit to be added to the profit list.
     */
    public void addProfit(double profit) {
        profits.add(profit);

        // Remove first entry of the list if its size exceeds the examination interval.
        if (profits.size() > SimulationManager.sizeOfExaminationInterval) {
            profits.remove(0);
        }
    }

    /**
     * @return profits of the firm.
     */
    public List<Double> getProfits() {
        return profits;
    }

    /**
     * Reviews whether the last two periods lead to the identical result.
     * Depending on the competition type, either the prices or the quantities are checked.
     *
     * @return true, if the firm's price/quantity in the current period is identical to the previous period.
     */
    boolean actionsOfLastTwoPeriodsAreIdentical(List<Double> values) {
        int sizeOfValues = values.size();
        boolean result = false;

        if (sizeOfValues > 1) {
            if (Double.compare(values.get(sizeOfValues - 2), values.get(sizeOfValues - 1)) == 0) {
                result = true;
            }
        }

        return result;
    }

    /**
     * @return the price in the joint profit maximization (JPM) equilibrium
     * for the corresponding number of all competing firms
     */
    public double getPriceInJpmEquilibrium() {
        return omega / 2;
    }

    /**
     * Calculates all relevant data and adds it to the lists. First, the dependent variable (price/quantity)
     * is calculated and added to the corresponding list (prices/quantities).
     * Based on that, the profit is calculated and added to the profit list.
     *
     * @param independentVariable price or quantity, depending on the type of competition.
     * @param independentVariablesOfOtherFirms prices or quantities of the other firms, depending on the type of
     *                                         competition.
     */
    void calculateData(int independentVariable, Integer[] independentVariablesOfOtherFirms) {

        // Calculate and set dependent variable (price/quantity) based on the actions of all firms.
        double dependentVariable = this.calculateDependentVariable(independentVariable, independentVariablesOfOtherFirms);
        this.setDependentVariable(dependentVariable);

        // Calculate and add profit (price/quantity) based on own price and quantity.
        double profit = this.calculateProfit(independentVariable, this.getDependentVariable(),
                independentVariablesOfOtherFirms.length + 1);
        this.addProfit(profit);
    }


    enum Type {
        /*
        Create a new Competition object, when retrieving the enum.
         */
        PRICE(new PriceCompetition()), QUANTITY(new QuantityCompetition());

        private final Competition competition;

        /**
         * Creates a Competition.Type object from the Competition.
         *
         * @param competition underlying Competition object
         */
        Type(Competition competition) {
            this.competition = competition;
        }

        /**
         * @return underlying Competition object.
         */
        public Competition getCompetition() {
            return competition;
        }
    }
}
