package de.unipassau.simulation;

import java.util.ArrayList;
import java.util.List;

class PriceCompetition extends Competition {

    private final double profitScalingCoefficient; // Scaling coefficient

    public PriceCompetition() {
        profitScalingCoefficient = maxProfitInNashEquilibrium / getProfitInNashEquilibrium(SimulationManager.MARKET_SIZE);
    }

    /**
     * @return price of the firm.
     */
    @Override
    public int getIndependentVariable() {
        return (int) super.getPrice();
    }

    /**
     * @param price price to be set and added to the price list.
     */
    @Override
    public void setIndependentVariable(int price) {
        super.setPrice(price);
    }

    /**
     * @return quantity of the firm.
     */
    @Override
    public double getDependentVariable() {
        return super.getQuantity();
    }

    /**
     * @param quantity quantity to be set and added to the quantity list.
     */
    @Override
    public void setDependentVariable(double quantity) {
        super.setQuantity(quantity);
    }

    /**
     * Calculates the quantity.
     *
     * @param ownPrice price of the firm
     * @param pricesOfOtherFirms prices of all other firms.
     */
    @Override
    public double calculateDependentVariable(Integer ownPrice, Integer[] pricesOfOtherFirms) {
        List<Integer> pricesOfOtherFirmsWithNonNegativeDemand = new ArrayList<>();

        for (int i = 0; i < pricesOfOtherFirms.length; i++) {
            Integer[] otherPrices = pricesOfOtherFirms.clone();
            otherPrices[i] = ownPrice;

            double quantityOfOtherFirm = calculateDemand(pricesOfOtherFirms[i].doubleValue(), otherPrices);

            // Only non-negative quantities are possible.
            if (!(quantityOfOtherFirm < 0)) {
                pricesOfOtherFirmsWithNonNegativeDemand.add(pricesOfOtherFirms[i]);
            }
        }

        double ownQuantity = calculateDemand(ownPrice.doubleValue(), pricesOfOtherFirmsWithNonNegativeDemand);

        // Only non-negative quantities are possible.
        return ownQuantity < 0 ? 0.0 : ownQuantity;
    }

    /**
     * Calculates the profit depending on price, quantity and number of all firms.
     *
     * @param price price of the firm
     * @param quantity quantity of the firm
     * @param numberOfFirms number of all competing firms
     * @return profit of the firm
     */
    @Override
    public double calculateProfit(double price, double quantity, int numberOfFirms) {
        return price * quantity * profitScalingCoefficient;
    }

    /**
     * Reviews whether the prices of the last two periods lead to the identical result.
     *
     * @return true, if the firm's price in the current period is identical to the previous period.
     */
    @Override
    public boolean actionsOfLastTwoPeriodsAreIdentical() {
        return super.actionsOfLastTwoPeriodsAreIdentical(super.getPrices());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPriceInNashEquilibrium(int numberOfFirms) {
        return (omega * (1 - theta))/(2 + theta * (numberOfFirms - 3));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getProfitInNashEquilibrium(int numberOfFirms) {
        return (Math.pow(omega, 2) * (1 - theta) * (1 + theta * (numberOfFirms - 2))) /
                (lambda * Math.pow(2 + theta * (numberOfFirms - 3), 2) * (1 + theta * (numberOfFirms - 1)));
    }

    /**
     * @return new PriceCompetition object
     */
    @Override
    public Competition clone() {
        return new PriceCompetition();
    }

    /**
     * @param numberOfFirms number of firms
     * @return Omega (see economic model)
     */
    private double calcOmega(double numberOfFirms) {
        return (omega / (lambda * (1 + theta * (numberOfFirms - 1))));
    }

    /**
     * @param numberOfFirms number of firms
     * @return Lambda (see economic model)
     */
    private double calcLambda(double numberOfFirms) {
        return ((1 + theta * (numberOfFirms - 2)) / (lambda * (1 - theta) * (1 + theta * (numberOfFirms - 1))));
    }

    /**
     * @param numberOfFirms number of firms
     * @return Theta (see economic model)
     */
    private double calcTheta(double numberOfFirms) {
        return ((theta * (numberOfFirms - 1)) / (lambda * (1 - theta) * (1 + theta * (numberOfFirms - 1))));
    }

    /**
     * See {@link #calculateDemand(double, Integer[])}
     */
    private double calculateDemand(double ownPrice, List<? extends Integer> pricesOfOtherFirms) {
        return calculateDemand(ownPrice, pricesOfOtherFirms.toArray(Integer[]::new));
    }

    /**
     * Calculates the demand for a firm depending on its own price and the price of all other firms.
     *
     * @param ownPrice price of the firm
     * @param pricesOfOtherFirms price of all other firms
     * @return demand for the firm
     */
    private double calculateDemand(double ownPrice, Integer[] pricesOfOtherFirms) {
        int numberOfFirms = pricesOfOtherFirms.length + 1;
        double demand;

        if (numberOfFirms == 1) {
            demand = (calcOmega(numberOfFirms) - calcLambda(numberOfFirms) * ownPrice);
        } else {
            double sumOfOtherPrices = 0;

            for (Integer pricesOfOtherFirm : pricesOfOtherFirms) {
                sumOfOtherPrices += pricesOfOtherFirm.doubleValue();
            }

            demand = calcOmega(numberOfFirms) - calcLambda(numberOfFirms) * ownPrice +
                    calcTheta(numberOfFirms) * ((sumOfOtherPrices) / (numberOfFirms - 1));
        }

        return demand;
    }
}