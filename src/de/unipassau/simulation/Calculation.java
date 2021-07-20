package de.unipassau.simulation;

import java.util.List;

class Calculation {

    /**
     * Calculates the mean of all values of a passed List.
     *
     * @param list List containing all values.
     * @return mean rounded to 3 decimals.
     */
    static double getMean(List<? extends Number> list) {
        double sum = 0.0;

        for (Number l : list) {
            sum += l.doubleValue();
        }

        double mean = sum / list.size();

        mean = round(mean, 3);

        return mean;
    }


    /**
     * Calculates the standard deviation of all values of a passed List.
     *
     * @param list List containing all values.
     * @return standard deviation rounded to 3 decimals.
     */
    static double getSD(List<? extends Number> list) {
        double mean = getMean(list);
        double standardDeviation = 0.0;


        for (Number number : list) {
            standardDeviation += Math.pow(number.doubleValue() - mean, 2);
        }
        double sd = Math.sqrt(standardDeviation / (list.size() - 1));


        sd = round(sd, 3);

        return sd;
    }


    /**
     * Method rounds a given number to a certain number of decimals.
     *
     * @param number   number to be rounded.
     * @param decimals desired number of decimals.
     * @return rounded number
     */
    static double round(double number, int decimals) {
        double factors = Math.pow(10, decimals);

        return (Math.round(factors * number) / factors);
    }

}
