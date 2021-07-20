package de.unipassau.simulation;


class QuantityCompetition extends Competition {

    // Scaling coefficients
    private final double profitScalingCoefficient;
    private final double quantityScalingCoefficient;

    public QuantityCompetition() {
        profitScalingCoefficient = maxProfitInNashEquilibrium / getProfitInNashEquilibrium(SimulationManager.MARKET_SIZE);
        quantityScalingCoefficient = getQuantityInJpmEquilibrium() / fixedJpmEquilibrium;
    }

    /**
     * @return quantity of the firm.
     */
    @Override
    public int getIndependentVariable() {
        return (int) super.getQuantity();
    }

    /**
     * @param quantity quantity to be set and added to the quantity list.
     */
    @Override
    public void setIndependentVariable(int quantity) {
        super.setQuantity(quantity);
    }

    /**
     * @return price of the firm.
     */
    @Override
    public double getDependentVariable() {
        return super.getPrice();
    }

    /**
     * @param price price to be set and added to the price list.
     */
    @Override
    public void setDependentVariable(double price) {
        super.setPrice(price);
    }

    /**
     * Calculates the quantity.
     *
     * @param ownQuantity quantity of the firm
     * @param quantitiesOfOtherFirms quantities of all other firms.
     */
    @Override
    public double calculateDependentVariable(Integer ownQuantity, Integer[] quantitiesOfOtherFirms) {

        double scaledOwnQuantity = ownQuantity.doubleValue() * quantityScalingCoefficient;
        double sumOfOtherQuantities = 0;
        for (Integer quantitiesOfOtherFirm : quantitiesOfOtherFirms) {
            sumOfOtherQuantities += quantitiesOfOtherFirm.doubleValue() * quantityScalingCoefficient;
        }

        return omega - lambda * (scaledOwnQuantity + theta * (sumOfOtherQuantities));
    }

    /**
     * Calculates the profit depending on quantity, price and number of all firms.
     *
     * @param quantity quantity of the firm
     * @param price price of the firm
     * @param numberOfFirms number of all competing firms
     * @return profit of the firm
     */
    @Override
    public double calculateProfit(double quantity, double price, int numberOfFirms) {
        return quantity * price * profitScalingCoefficient;
    }

    /**
     * Reviews whether the quantities of the last two periods lead to the identical result.
     *
     * @return true, if the firm's quantity in the current period is identical to the previous period.
     */
    @Override
    public boolean actionsOfLastTwoPeriodsAreIdentical() {
        return actionsOfLastTwoPeriodsAreIdentical(super.getQuantities());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPriceInNashEquilibrium(int numberOfFirms) {
        return omega / (2 + theta * (numberOfFirms - 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getProfitInNashEquilibrium(int numberOfFirms) {
        return (Math.pow(omega, 2)) / (lambda * Math.pow(2 + theta * (numberOfFirms - 1), 2));
    }

    /**
     * @return the quantity in the joint profit maximization (JPM) equilibrium
     * for the corresponding number of all competing firms
     */
    private double getQuantityInJpmEquilibrium() {
        return (omega) / (2 * lambda * (1 + theta * (SimulationManager.MARKET_SIZE - 1)));
    }

    /**
     * @return new PriceCompetition object
     */
    @Override
    public Competition clone() {
        return new QuantityCompetition();
    }
}
