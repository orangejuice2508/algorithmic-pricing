package de.unipassau.simulation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;


class SimulationManager {

    static final SimulationRun.Timing TIMING = SimulationRun.Timing.SIMULTANEOUS;       // Timing of interaction among firms
    static final Competition.Type COMPETITION_TYPE = Competition.Type.QUANTITY;         // Type of competition between firms
    static final int MARKET_SIZE = 2;                                                   // Number of firms in the market

    // Simulation settings
    static final int numberOfSimulationRuns = 10;                                      // Desired number of single simulation runs
    static final int sizeOfActionSet = 101;                                             // Total number of possible states and actions
    static final int maxNumberOfPeriods = sizeOfActionSet * sizeOfActionSet * 5000;     // Maximum number of periods per simulation run
    static final int minNumberOfConvergedPeriods = (int) (maxNumberOfPeriods * 0.0001); // Number of identical periods necessary to assume convergence of the algorithms
    static final int sizeOfExaminationInterval = minNumberOfConvergedPeriods;           // Size of interval to be examined; only relevant if the firms' states do not end up at a fix price or quantity level but oscillate in a certain interval.
    static QLearning.Parameter qLearningParameter;                                      // Parameter for Q-Learning

    // Optimal parameters for Q-Learning depending on Treatment.
    private final HashMap<Treatment, QLearning.Parameter> treatmentParameters = new HashMap<>() {{
        // For simultaneous interaction
        put(new Treatment(SimulationRun.Timing.SIMULTANEOUS, Competition.Type.PRICE, 2),
                new QLearning.Parameter(0.025, 0.96));
        put(new Treatment(SimulationRun.Timing.SIMULTANEOUS, Competition.Type.PRICE, 3),
                new QLearning.Parameter(0.055, 0.99, 1.0));
        put(new Treatment(SimulationRun.Timing.SIMULTANEOUS, Competition.Type.QUANTITY, 2),
                new QLearning.Parameter(0.055, 0.95));
        put(new Treatment(SimulationRun.Timing.SIMULTANEOUS, Competition.Type.QUANTITY, 3),
                new QLearning.Parameter(0.04, 0.98, 0.0));

        // For sequential interactions
        put(new Treatment(SimulationRun.Timing.SEQUENTIAL, Competition.Type.PRICE, 2),
                new QLearning.Parameter(0.055, 0.95));
        put(new Treatment(SimulationRun.Timing.SEQUENTIAL, Competition.Type.PRICE, 3),
                new QLearning.Parameter(0.14, 0.99, 1.0));
        put(new Treatment(SimulationRun.Timing.SEQUENTIAL, Competition.Type.QUANTITY, 2),
                new QLearning.Parameter(0.2, 0.97));
        put(new Treatment(SimulationRun.Timing.SEQUENTIAL, Competition.Type.QUANTITY, 3),
                new QLearning.Parameter(0.02, 0.98, 1.0));
    }};

    // Lists to store results.
    private final List<Double> prices = new ArrayList<>();
    private final List<Double> pricesSDs = new ArrayList<>();
    private final List<Double> quantities = new ArrayList<>();
    private final List<Double> profits = new ArrayList<>();
    private final List<Double> degrees = new ArrayList<>();
    private final List<Double> periods = new ArrayList<>();

    // Starting time of the simulation
    private LocalDateTime startingTime;
    private long startingTimeInMillis;
    private DateTimeFormatter dtf;


    /**
     * Main-method of the whole Simulation. Creates a new SimulationManager to run the simulation
     * or to examine alpha and delta or gamma. The desired examination method has to be called,
     * while all others have to be commented out.
     */
    public static void main(String[] args) {
        SimulationManager simulationManager = new SimulationManager();

        simulationManager.singleSimulation();
        // simulationManager.heatmapForAlphaAndDelta();
        // simulationManager.analyzeGamma();
    }

    /**
     * Calls a single simulation run and exports it.
     */
    private void singleSimulation() {
        setEnvironment();

        String output =
                "Simulating " + numberOfSimulationRuns + " runs "
                + "interacting " + TIMING + "LY "
                + "in a " + COMPETITION_TYPE + " competition "
                + "with " + MARKET_SIZE + " firms "
                + "(alpha = " + qLearningParameter.getAlpha() + "; "
                + "delta = " + qLearningParameter.getDelta();

        if (MARKET_SIZE > 2) {
            output += "; gamma = " + qLearningParameter.getGamma();
        }
        output += ")";

        System.out.println(output);

        simulate();

        export();
    }


    /**
     * Manages several simulation runs to examine alpha and delta, stores their results and exports a .csv file
     * to be further processed to generate a heat map.
     */
    private void heatmapForAlphaAndDelta() {

        // Desired scope of alpha and delta to be analyzed.
        double alphaStart = 0.005;
        double alphaEnd = 0.3;
        double alphaStep = 0.005;

        double deltaStart = 0.80;
        double deltaEnd = 0.99;
        double deltaStep = 0.01;

        double fixedGamma = 0.5;

        // Total number of steps per parameter
        int numberOfAlpha = (int) Math.round((alphaEnd - alphaStart + alphaStep) / alphaStep);
        int numberOfDelta = (int) Math.round((deltaEnd - deltaStart + deltaStep) / deltaStep);
        int numberOfSteps = Math.round(numberOfAlpha * numberOfDelta);

        int stepcounter = 1;

        // Indices for columns and rows
        int c = 0, r = 0;

        // Arrays to store results
        double[][] meanPrices = new double[numberOfAlpha][numberOfDelta];
        double[][] meanPriceSDs = new double[numberOfAlpha][numberOfDelta];
        double[][] meanQuantities = new double[numberOfAlpha][numberOfDelta];
        double[][] meanProfits = new double[numberOfAlpha][numberOfDelta];
        double[][] meanPeriods = new double[numberOfAlpha][numberOfDelta];
        double[][] meanDegrees = new double[numberOfAlpha][numberOfDelta];
        double[][] meanPercentages = new double[numberOfAlpha][numberOfDelta];


        System.out.println("Generating heatmap data for alpha and delta " + TIMING + " interaction "
                + "in a " + COMPETITION_TYPE + " competition "
                + "with " + MARKET_SIZE + " firms");


        /*
        For-loops to sequentially conduct all simulations necessary with all desired parameter combinations
        Rounding parameters is necessary to fix precision problem of double values,
        as the number is stored as a binary representation of a fraction and a exponent.
        */
        for (double i = alphaStart; Calculation.round(i, 3) <= alphaEnd; i = i + alphaStep) {

            System.out.println("#");

            for (double j = deltaStart; Calculation.round(j, 2) <= deltaEnd; j = j + deltaStep) {

                /*
                The arrays' results of the previous simulation runs are deleted from the Lists
                as they already were saved, and the optimal gamma is set.
                 */
                setEnvironment();

                // Desired values for alpha and delta are set.
                qLearningParameter = new
                        QLearning.Parameter(Calculation.round(i, 4),Calculation.round(j, 4), fixedGamma);

                System.out.print(stepcounter + "/" + numberOfSteps
                        + "; alpha: " + qLearningParameter.getAlpha()
                        + "; delta: " + qLearningParameter.getDelta());

                if (MARKET_SIZE > 2) {
                    System.out.println("; gamma: " + qLearningParameter.getGamma());
                }

                // Simulation run is conducted.
                simulate();

                // Results of the run are stored.
                meanPrices[r][c] = Calculation.getMean(prices);
                meanPriceSDs[r][c] = Calculation.getMean(pricesSDs);
                meanQuantities[r][c] = Calculation.getMean(quantities);
                meanProfits[r][c] = Calculation.getMean(profits);
                meanPeriods[r][c] = Calculation.round(Calculation.getMean(periods) / 1000000, 3);
                meanDegrees[r][c] = Calculation.getMean(degrees);
                meanPercentages[r][c] = (pricesSDs.stream()
                        .filter(p -> p < MARKET_SIZE)
                        .count() * 100.0 / numberOfSimulationRuns);

                c++;
                stepcounter++;
            }
            c = 0;
            r++;
        }

        exportHeatmapData(alphaStart, alphaStep, numberOfAlpha, deltaStart, deltaStep, numberOfDelta,
                meanPrices,meanPriceSDs,meanQuantities,meanProfits,meanPeriods,meanDegrees,meanPercentages);
    }


    /**
     * Manages several simulation runs to examine gamma, stores their results and exports a .csv file.
     */
    private void analyzeGamma() {

        if (MARKET_SIZE < 3) {
            throw new IllegalArgumentException("Analyzing the weighting factor gamma makes " +
                    "only sense for more than two firms.");
        }

        // Desired scope of the weight to be analyzed.
        double weightStart = 0.;
        double weightEnd = 0.;
        double weightSteps = 0.02;

        // Fixed alpha and delta values as a framework to analyze gamma.
        double alphaStart = 0.05;
        double alphaEnd = 0.05;
        double alphaStep = 0.05;

        double deltaStart = 0.8;
        double deltaEnd = 0.85;
        double deltaStep = 0.05;

        // Total number of steps per parameter
        int numberOfWeightSteps = (int) Math.round((weightEnd - weightStart + weightSteps) / weightSteps);
        int numberOfAlpha = (int) Math.round((alphaEnd - alphaStart + alphaStep) / alphaStep);
        int numberOfDelta = (int) Math.round((deltaEnd - deltaStart + deltaStep) / deltaStep);

        int stepcounter = 1;

        // Indices for column and row
        int c = 0, r = 0;

        // Arrays to store results
        double[][] meanPrice = new double[numberOfWeightSteps][numberOfAlpha * numberOfDelta];
        double[][] meanPriceSD = new double[numberOfWeightSteps][numberOfAlpha * numberOfDelta];
        double[][] meanQuantity = new double[numberOfWeightSteps][numberOfAlpha * numberOfDelta];
        double[][] meanProfit = new double[numberOfWeightSteps][numberOfAlpha * numberOfDelta];
        double[][] meanPeriod = new double[numberOfWeightSteps][numberOfAlpha * numberOfDelta];
        double[][] meanDegree = new double[numberOfWeightSteps][numberOfAlpha * numberOfDelta];
        double[][] meanPercentage = new double[numberOfWeightSteps][numberOfAlpha * numberOfDelta];

        System.out.println("Analyzing gamma at " + TIMING + " interaction "
                + "in a " + COMPETITION_TYPE + " competition "
                + "with " + MARKET_SIZE + " firms");

        /*
        For-loops to sequentially conduct all simulations necessary with all desired parameter combinations
        Rounding parameters is necessary to fix precision problem of double values,
        as the number is stored as a binary representation of a fraction and a exponent.
        */
        for (double i = alphaStart; Calculation.round(i, 3) <= alphaEnd; i = i + alphaStep) {
            for (double j = deltaStart; Calculation.round(j, 2) <= deltaEnd; j = j + deltaStep) {
                for (double k = weightStart; Calculation.round(k, 3) <= weightEnd; k = k + weightSteps) {

                    /*
                    The arrays' results of the previous simulation runs are deleted from the Lists
                    as they already were saved, and the optimal gamma is set.
                     */
                    setEnvironment();

                    // Desired values for alpha, delta, and gamma are set.
                    qLearningParameter = new QLearning.Parameter(Calculation.round(i, 4),
                                    Calculation.round(j, 4),
                                    Calculation.round(k,4));

                    System.out.println(stepcounter + "/" + (numberOfWeightSteps * numberOfAlpha * numberOfDelta)
                            + " with alpha: " + qLearningParameter.getAlpha()
                            + ", and delta: " + qLearningParameter.getDelta()
                            + " at gamma: " + qLearningParameter.getGamma());

                    // Simulation run is conducted.
                    simulate();

                    // Results of the run are stored.
                    meanPrice[c][r] = Calculation.getMean(prices);
                    meanPriceSD[c][r] = Calculation.getMean(pricesSDs);
                    meanQuantity[c][r] = Calculation.getMean(quantities);
                    meanProfit[c][r] = Calculation.getMean(profits);
                    meanPeriod[c][r] = Calculation.round(Calculation.getMean(periods) / 1000000, 3);
                    meanDegree[c][r] = Calculation.getMean(degrees);
                    meanPercentage[c][r] = (pricesSDs.stream()
                            .filter(p -> p < MARKET_SIZE)
                            .count() * 100.0 / numberOfSimulationRuns);
                    c++;
                    stepcounter++;
                }
                c = 0;
                r++;
            }
        }

        // Stored results are written to a StringBuilder used to export data as .csv-file.
        StringBuilder content = new StringBuilder();
        StringJoiner joiner = new StringJoiner(",", ",", "");

        // Header: alpha
        content.append("alpha");
        for (int j = 0; j < numberOfAlpha; j++) {
            for (int k = 0; k < numberOfDelta; k++) {
                joiner.add(String.valueOf(Calculation.round(alphaStart + j * alphaStep, 4)));
            }
        }
        content.append(joiner);

        // Header: delta
        content.append("\ndelta");
        joiner = new StringJoiner(",", ",", "");
        for (int j = 0; j < numberOfAlpha; j++) {
            for (int k = 0; k < numberOfDelta; k++) {
                joiner.add(String.valueOf(Calculation.round(deltaStart + k * deltaStep, 4)));
            }
        }
        content.append(joiner);
        content.append("\n \n");

        // Actual results
        // Mean of prices of one combination of alpha and delta
        content.append("MEAN PRICE:\n");
        content.append(analyzeGammaDataToString(weightStart, weightSteps, numberOfWeightSteps, numberOfAlpha, numberOfDelta, meanPrice));

        // Mean standard deviation of the markets prices of one combination of alpha and delta
        content.append("MEAN STANDARD DEVIATION OF THE MARKET PRICE:\n");
        content.append(analyzeGammaDataToString(weightStart, weightSteps, numberOfWeightSteps, numberOfAlpha, numberOfDelta,
                meanPriceSD));

        // Mean of quantities of one combination of alpha and delta
        content.append("MEAN QUANTITY:\n");
        content.append(analyzeGammaDataToString(weightStart, weightSteps, numberOfWeightSteps, numberOfAlpha, numberOfDelta,
                meanQuantity));

        // Mean of profits of one combination of alpha and delta
        content.append("MEAN PROFIT:\n");
        content.append(analyzeGammaDataToString(weightStart, weightSteps, numberOfWeightSteps, numberOfAlpha, numberOfDelta,
                meanProfit));

        // Mean of simulated periods of one combination of alpha and delta
        content.append("PERIODS (in mio.):\n");
        content.append(analyzeGammaDataToString(weightStart, weightSteps, numberOfWeightSteps, numberOfAlpha, numberOfDelta,
                meanPeriod));

        // Mean of simulated periods of one combination of alpha and delta
        content.append("DEGREE OF TACIT COLLUSION:\n");
        content.append(analyzeGammaDataToString(weightStart, weightSteps, numberOfWeightSteps, numberOfAlpha, numberOfDelta,
                meanDegree));

        // Percentage of coordination
        content.append("PERCENTAGE OF COORDINATION:\n");
        content.append(analyzeGammaDataToString(weightStart, weightSteps, numberOfWeightSteps, numberOfAlpha, numberOfDelta,
                meanPercentage));

        // Export procedure
        Export export = Export.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = dateFormat.format(new Date()) + "_weightAnalysis_"
                + (qLearningParameter.getAlpha() * 100)
                + "_" + (qLearningParameter.getDelta() * 100);
        export.exportCsv(filename, content.toString());
        System.out.println("\nFile " + filename + ".csv exported");
    }


    /**
     * Resets the environment by emptying the Lists and setting optimal parameters of alpha, delta, and gamma
     * according to the treatment.
     */
    private void setEnvironment() {
        // Empty Lists.
        prices.clear();
        pricesSDs.clear();
        quantities.clear();
        profits.clear();
        degrees.clear();
        periods.clear();

        // Set optimal parameters.
        Treatment treatment = new Treatment(TIMING, COMPETITION_TYPE, MARKET_SIZE);
        qLearningParameter = treatmentParameters.get(treatment);

        if (qLearningParameter == null) {
            qLearningParameter = new QLearning.Parameter(0.3,0.95, 1.0);
        }
    }


    /**
     * Starts simulation, conducts the desired number of simulation runs (numberOfSimulationRuns), and calls
     * required methods to store and export the simulation data.
     */
    private void simulate() {
        if (MARKET_SIZE < 2) {
            throw new IllegalArgumentException("Number of firms must greater than or equal two.");
        }

        // Store starting time.
        dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        startingTimeInMillis = System.currentTimeMillis();
        startingTime = LocalDateTime.now();

        // Create the desired number of simulation runs.
        List<SimulationRun> simulationRuns = new ArrayList<>();
        for (int i = 0; i < numberOfSimulationRuns; i++) {
            simulationRuns.add(TIMING.getSimulationRun().clone());
        }

        // Run the simulations parallelly.
        simulationRuns.parallelStream().forEach(SimulationRun::simulate);

        // Store results of each simulation run.
        for (SimulationRun simulationRun : simulationRuns) {
            storeData(simulationRun.getMeanPrice(),
                    simulationRun.getSDPrice(),
                    simulationRun.getMeanQuantity(),
                    simulationRun.getMeanProfit(),
                    simulationRun.getDegreeOfTacitCollusion(),
                    simulationRun.getNumberOfPeriods());
        }

        System.out.println("  ->  Degree of Tacit Collusion: " + Calculation.getMean(degrees) + "; "
                + "colluding in " + (pricesSDs.stream()
                .filter(p -> p < MARKET_SIZE)
                .count() * 100.0 / numberOfSimulationRuns) + "% ");

    }


    /**
     * Stores results of each simulation run.
     *
     * @param price           mean price (= market price) of all firms in the market
     * @param priceSD         standard deviation of the mean price
     * @param quantity        mean quantity of all firms in the market
     * @param profit          mean profit of all firms in the market
     * @param degree          degree of tacit collusion of the market price
     * @param numberOfPeriods number of periods necessary to collude (or max. number of periods if firms did
     *                        not collude)
     */
    private void storeData(double price, double priceSD, double quantity,
                           double profit, double degree, int numberOfPeriods) {
        prices.add(price);
        pricesSDs.add(priceSD);
        quantities.add(quantity);
        profits.add(profit);
        degrees.add(degree);
        periods.add(numberOfPeriods + 0.0);
    }

    /**
     * Helper method to export all relevant and averaged data of each combination of the analyzed parameters in the
     * heatmap method.
     *
     * @param firstParameterStart start value of the first parameter
     * @param firstParameterStep step size to increase the first parameter
     * @param numberOfFirstParameter number of different values of the first parameter
     * @param secondParameterStart start value of the second parameter
     * @param secondParameterStep step size to increase the second parameter
     * @param numberOfSecondParameter number of different values of the second parameter
     * @param meanPrices array with the mean prices of all simulations runs of each parameter combination
     * @param meanPriceSDs array with the mean standard deviation of prices of all simulations runs of each parameter combination
     * @param meanQuantities array with the mean quantities of all simulations runs of each parameter combination
     * @param meanProfits array with the mean profits of all simulations runs of each parameter combination
     * @param meanPeriods array with the mean periods of all simulations runs of each parameter combination
     * @param meanDegrees array with the mean degree of tacit collusion of all simulations runs of each parameter combination
     * @param meanPercentages array with the mean percentage of coordination of all simulations runs of each parameter combination
     */
    private void exportHeatmapData(double firstParameterStart, double firstParameterStep, int numberOfFirstParameter,
                                   double secondParameterStart, double secondParameterStep, int numberOfSecondParameter,
                                   double[][] meanPrices, double[][] meanPriceSDs, double[][] meanQuantities,
                                   double[][] meanProfits, double[][] meanPeriods,
                                   double[][] meanDegrees, double[][] meanPercentages) {
        // Stored results are written to a StringBuilder used to export data as .csv-file.
        StringBuilder content = new StringBuilder();

        // Actual results
        // Mean of prices of one combination of alpha and delta
        content.append("MEAN PRICE:\n");
        content.append(heatmapDataToString(firstParameterStart, firstParameterStep, numberOfFirstParameter,
                secondParameterStart, secondParameterStep, numberOfSecondParameter,
                meanPrices));

        // Mean standard deviation of the markets prices of one combination of alpha and delta
        content.append("MEAN STANDARD DEVIATION OF THE MARKET PRICE:\n");
        content.append(heatmapDataToString(firstParameterStart, firstParameterStep, numberOfFirstParameter,
                secondParameterStart, secondParameterStep, numberOfSecondParameter,
                meanPriceSDs));

        // Mean of quantities of one combination of alpha and delta
        content.append("MEAN QUANTITY:\n");
        content.append(heatmapDataToString(firstParameterStart, firstParameterStep, numberOfFirstParameter,
                secondParameterStart, secondParameterStep, numberOfSecondParameter,
                meanQuantities));

        // Mean of profits of one combination of alpha and delta
        content.append("MEAN PROFIT:\n");
        content.append(heatmapDataToString(firstParameterStart, firstParameterStep, numberOfFirstParameter,
                secondParameterStart, secondParameterStep, numberOfSecondParameter,
                meanProfits));

        // Mean of simulated periods of one combination of alpha and delta
        content.append("PERIODS (in mio.):\n");
        content.append(heatmapDataToString(firstParameterStart, firstParameterStep, numberOfFirstParameter,
                secondParameterStart, secondParameterStep, numberOfSecondParameter,
                meanPeriods));

        // Mean of simulated periods of one combination of alpha and delta
        content.append("DEGREE OF TACIT COLLUSION:\n");
        content.append(heatmapDataToString(firstParameterStart, firstParameterStep, numberOfFirstParameter,
                secondParameterStart, secondParameterStep, numberOfSecondParameter,
                meanDegrees));

        // Percentage of coordination
        content.append("PERCENTAGE OF COORDINATION:\n");
        content.append(heatmapDataToString(firstParameterStart, firstParameterStep, numberOfFirstParameter,
                secondParameterStart, secondParameterStep, numberOfSecondParameter,
                meanPercentages));

        // Export procedure
        Export export = Export.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = dateFormat.format(new Date()) + "_heatmap";
        export.exportCsv(filename, content.toString());
        System.out.println("\nFile " + filename + ".csv exported");
    }

    /**
     * Helper method to convert an array with data of the heatmap method into a String ready for export.
     *
     * @param firstParameterStart start value of the first parameter
     * @param firstParameterStep step size to increase the first parameter
     * @param numberOfFirstParameter number of different values of the first parameter
     * @param secondParameterStart start value of the second parameter
     * @param secondParameterStep step size to increase the second parameter
     * @param numberOfSecondParameter number of different values of the second parameter
     * @param data array of data to be converted to a string
     * @return a string of the data separated by commas
     */
    private String heatmapDataToString(double firstParameterStart, double firstParameterStep, int numberOfFirstParameter,
                                       double secondParameterStart, double secondParameterStep, int numberOfSecondParameter,
                                       double[][] data) {

        StringBuilder content = new StringBuilder();
        StringJoiner joiner = new StringJoiner(",", ",", "");

        // Header: alpha
        content.append("alpha");

        // Header: delta
        for (int i = 0; i < numberOfSecondParameter; i++) {
            joiner.add(String.valueOf(Calculation.round(secondParameterStart + i * secondParameterStep, 4)));
        }
        content.append(joiner);

        for (int i = 0; i < numberOfFirstParameter; i++) {
            content.append("\n");

            // Corresponding alpha value of each row is printed in first column.
            content.append(Calculation.round(firstParameterStart + i * firstParameterStep, 4));

            joiner = new StringJoiner(",", ",", "");
            for (int j = 0; j < numberOfSecondParameter; j++) {
                joiner.add(String.valueOf(data[i][j]));
            }
            content.append(joiner);
        }

        content.append("\n \n");

        return content.toString();
    }


    /**
     * Helper method to convert an array with data of the analyzeGamma method into a String ready for export.
     *
     * @param weightStart first gamma value
     * @param weightSteps step size to increase gamma
     * @param numberOfWeightSteps number of different gamma values
     * @param numberOfAlpha number of different alpha values
     * @param numberOfDelta number of different delta values
     * @param data array of data to be converted to a string
     * @return a string of the data separated by commas
     */
    private String analyzeGammaDataToString(double weightStart, double weightSteps, int numberOfWeightSteps,
                                            int numberOfAlpha, int numberOfDelta, double[][] data) {

        StringBuilder content = new StringBuilder();
        StringJoiner joiner;

        content.append("gamma");

        for (int i = 0; i < numberOfWeightSteps; i++) {
            content.append("\n");
            // Corresponding gamma value of each row is printed in first column.
            content.append(Calculation.round(weightStart + i * weightSteps, 4));

            joiner = new StringJoiner(",", ",", "");
            for (int j = 0; j < numberOfAlpha * numberOfDelta; j++) {
                joiner.add(String.valueOf(data[i][j]));
            }
            content.append(joiner);
        }

        content.append("\n \n");
        return content.toString();
    }

    /**
     * Prepares the simulation data for its export by collecting all export data in a StringBuilder in
     * csv format. Export information includes all input commands, all necessary parameters of the Q-Learning
     * algorithm and the simulation framework, as well as general information about the simulation (such as
     * duration, starting and end time). The most interesting output data are the simulation's results:
     * the mean degree of tacit collusion over all simulation runs, the mean prices, quantities and profits,
     * and the mean number of periods until collusion (all means come with their corresponding standard
     * deviations). Furthermore, each simulation run is listed separately along with its results: the degree of
     * tacit collusion, the mean of price, quantity, profit, and number of periods, as well as the standard
     * deviation of all market prices combined. The latter also gets returned as mean over all simulation runs
     * and serves as an evaluation criteria to further evaluate the degree of tacit collusion.
     */
    private void export() {
        // Get or create instance of the Export class.
        Export export = Export.getInstance();

        // Get ending and running time.
        long runningTimeInMillis = (System.currentTimeMillis() - startingTimeInMillis);
        String runningTime = String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(runningTimeInMillis),
                TimeUnit.MILLISECONDS.toMinutes(runningTimeInMillis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(runningTimeInMillis)),
                TimeUnit.MILLISECONDS.toSeconds(runningTimeInMillis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runningTimeInMillis)));

        LocalDateTime endingTime = LocalDateTime.now();

        // Create StringBuilder to add export data to.
        StringBuilder content = new StringBuilder();

        // Treatment data
        content.append("TREATMENT");
        content.append("\ntiming of interaction,");
        content.append(TIMING);
        content.append("\ntype of competition,");
        content.append(COMPETITION_TYPE);
        content.append("\nnumber of firms,");
        content.append(MARKET_SIZE);

        // Parameters of the economic model
        content.append("\n \nECONOMIC MODEL");
        content.append("\nω,");
        content.append(Competition.omega);
        content.append("\nλ,");
        content.append(Competition.lambda);
        content.append("\nθ,");
        content.append(Competition.theta);

        // Simulation data and conditions
        content.append("\n \nSIMULATION");
        content.append("\nstart,");
        content.append(dtf.format(startingTime));
        content.append("\nduration,");
        content.append(runningTime);
        content.append("\nend,");
        content.append(dtf.format(endingTime));
        content.append("\nmax number of periods,");
        content.append(maxNumberOfPeriods);
        content.append("\nnumber of runs,");
        content.append(numberOfSimulationRuns);
        content.append("\nnumber of coordinative runs,");
        content.append(pricesSDs.stream().filter(p -> p < MARKET_SIZE).count());
        content.append(",");
        content.append(pricesSDs.stream()
                .filter(p -> p < MARKET_SIZE)
                .count() * 100 / numberOfSimulationRuns);
        content.append("%");


        // Parameters of the Q-Learning algorithms
        content.append("\n \nQ-LEARNING");
        content.append("\nactionset,[0; ");
        content.append(sizeOfActionSet - 1);
        content.append("]");
        content.append("\nalpha,");
        content.append(qLearningParameter.getAlpha());
        content.append("\nbeta,");
        content.append(qLearningParameter.getBeta());
        content.append("\ndelta,");
        content.append(qLearningParameter.getDelta());
        if (MARKET_SIZE > 2) {
            content.append("\ngamma,");
            content.append(qLearningParameter.getGamma());
        }
        content.append("\nØ epsilon at the end,");
        content.append(Calculation.round(
                Math.exp(-qLearningParameter.getBeta() * Calculation.getMean(periods)), 8));


        // Bundled mean data over all simulation runs
        content.append("\n \nSUMMARY OF ALL RUNS");
        content.append("\nfigure,mean,σ");

        content.append("\nprice,");
        content.append(Calculation.getMean(prices));
        content.append(",");
        content.append(Calculation.getSD(prices));

        content.append("\nσ of prices,");
        content.append(Calculation.getMean(pricesSDs));

        content.append("\nquantity,");
        content.append(Calculation.getMean(quantities));
        content.append(",");
        content.append(Calculation.getSD(quantities));

        content.append("\nprofit,");
        content.append(Calculation.getMean(profits));
        content.append(",");
        content.append(Calculation.getSD(profits));

        content.append("\nperiods (in mio),");
        content.append(Calculation.round(Calculation.getMean(periods) / 1000000, 3));
        content.append(",");
        content.append(Calculation.round(Calculation.getSD(periods) / 1000000, 3));

        content.append("\ndegree,");
        content.append(Calculation.getMean(degrees));
        content.append(",");
        content.append(Calculation.getSD(degrees));


        // Individual data of each simulation run
        content.append("\n \nDATA OF INDIVIDUAL RUNS");

        content.append("\ni,");
        content.append("price,");
        content.append("σ (of all prices),");
        content.append("quantity,");
        content.append("profit,");
        content.append("periods (in mio),");
        content.append("degree,");
        content.append("coordinative?");

        content.append("\n");
        StringJoiner joiner;

        for (int i = 0; i < numberOfSimulationRuns; i++) {
            joiner = new StringJoiner(",");

            joiner.add(String.valueOf(i + 1))
                    .add(prices.get(i).toString())
                    .add(pricesSDs.get(i).toString())
                    .add(quantities.get(i).toString())
                    .add(profits.get(i).toString())
                    .add(String.valueOf(Calculation.round(periods.get(i) / 1000000, 3)))
                    .add((degrees.get(i)).toString());
            if (pricesSDs.get(i) < MARKET_SIZE) {
                joiner.add("yes");
            } else {
                joiner.add("no");
            }
            content.append(joiner);
            content.append("\n");
        }

        // Get date format to use as filename.
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = dateFormat.format(new Date());

        export.exportCsv(filename, content.toString());

        System.out.println("File " + filename + ".csv exported");
    }
}
