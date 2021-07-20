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
    static int MARKET_SIZE;                                                             // Number of firms in the market

    // Simulation settings
    static final int numberOfSimulationRuns = 10;                                      // Desired number of single simulation runs
    static final int sizeOfActionSet = 101;                                             // Total number of possible states and actions
    static final int maxNumberOfPeriods = sizeOfActionSet * sizeOfActionSet * 5000;     // Maximum number of periods per simulation run
    static final int minNumberOfConvergedPeriods = (int) (maxNumberOfPeriods * 0.0001); // Number of identical periods necessary to assume convergence of the algorithms
    static final int sizeOfExaminationInterval = minNumberOfConvergedPeriods;           // Size of interval to be examined; only relevant if the firms' states do not end up at a fix price or quantity level but oscillate in a certain interval.
    static QLearning.Parameter qLearningParameter;                                      // Parameter for Q-Learning

    // Lists to store results.
    private final List<Double> prices = new ArrayList<>();
    private final List<Double> pricesSDs = new ArrayList<>();
    private final List<Double> quantities = new ArrayList<>();
    private final List<Double> profits = new ArrayList<>();
    private final List<Double> degrees = new ArrayList<>();
    private final List<Double> periods = new ArrayList<>();


    /**
     * Main-method of the whole Simulation. Creates a new SimulationManager to examine the market size.
     */
    public static void main(String[] args) {
        SimulationManager simulationManager = new SimulationManager();

        simulationManager.analyzeMarketSize();
    }


    private void analyzeMarketSize() {
        // Desired scope of market size to be analyzed.
        int marketSizeStart = 2;
        int marketSizeEnd = 10;

        int numberOfMarketSizes = marketSizeEnd - marketSizeStart + 1;

        int stepcounter = 1;

        // Index for rows.
        int r = 0;

        // Arrays to store results.
        double[] meanPrices = new double[numberOfMarketSizes];
        double[] meanPriceSDs = new double[numberOfMarketSizes];
        double[] meanQuantities = new double[numberOfMarketSizes];
        double[] meanProfits = new double[numberOfMarketSizes];
        double[] meanPeriods = new double[numberOfMarketSizes];
        double[] meanDegrees = new double[numberOfMarketSizes];
        double[] meanPercentages = new double[numberOfMarketSizes];


        System.out.println("Analyzing the market size at " + TIMING + " interaction "
                + "in a " + COMPETITION_TYPE + " competition");

        /*
        For-loops to sequentially conduct all simulations necessary with the desired market sizes.
        */
        for (int i = marketSizeStart; i <= marketSizeEnd; i++) {

            System.out.println("#");

            // Desired market size value is set.
            SimulationManager.MARKET_SIZE = i;

            /*
            The arrays' results of the previous simulation runs are deleted from the Lists
            as they already were saved, and the optimal parameters are set.
             */
            setEnvironment();

            System.out.print(stepcounter + "/" + numberOfMarketSizes
                    + "; Market Size: " + SimulationManager.MARKET_SIZE
                    + " with alpha: " + qLearningParameter.getAlpha()
                    + ", and delta: " + qLearningParameter.getDelta());

            if (MARKET_SIZE > 2) {
                System.out.println("; gamma: " + qLearningParameter.getGamma());
            } else {
                System.out.println();
            }

            // Simulation run is conducted.
            simulate();

            // Results of the run are stored.
            meanPrices[r] = Calculation.getMean(prices);
            meanPriceSDs[r] = Calculation.getMean(pricesSDs);
            meanQuantities[r] = Calculation.getMean(quantities);
            meanProfits[r] = Calculation.getMean(profits);
            meanPeriods[r] = Calculation.round(Calculation.getMean(periods) / 1000000, 3);
            meanDegrees[r] = Calculation.getMean(degrees);
            meanPercentages[r] = (pricesSDs.stream()
                    .filter(p -> p < MARKET_SIZE)
                    .count() * 100.0 / numberOfSimulationRuns);

            stepcounter++;
            r++;
        }

        // Stored results are written to a StringBuilder used to export data as .csv-file.
        StringBuilder content = new StringBuilder();
        StringJoiner joiner = new StringJoiner(",");

        // Header: market size
        joiner.add("MARKET SIZE")
                .add("MEAN PRICE").add("MEAN STANDARD DEVIATION OF THE MARKET PRICE")
                .add("MEAN QUANTITY").add("MEAN PROFIT").add("PERIODS (in mio.)").add("DEGREE OF TACIT COLLUSION")
                .add("PERCENTAGE OF COORDINATION");
        content.append(joiner);
        content.append("\n");

        for (int i = 0; i < meanDegrees.length; i++) {
            joiner = new StringJoiner(",");

            joiner.add(String.valueOf(marketSizeStart + i));
            joiner.add(String.valueOf(meanPrices[i])).add(String.valueOf(meanPriceSDs[i]))
                    .add(String.valueOf(meanQuantities[i])).add(String.valueOf(meanProfits[i]))
                    .add(String.valueOf(meanPeriods[i])).add(String.valueOf(meanDegrees[i]))
                    .add(String.valueOf(meanPercentages[i]));

            content.append(joiner).append("\n");
        }
        // Export procedure
        Export export = Export.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = dateFormat.format(new Date()) + "_marketSizeAnalysis";
        export.exportCsv(filename, content.toString());
        System.out.println("\nFile " + filename + ".csv exported");
    }

    /**
     * Resets the environment by emptying the Lists and setting identical parameters of alpha, delta, and gamma
     * for each treatment.
     */
    private void setEnvironment() {
        // Empty Lists.
        prices.clear();
        pricesSDs.clear();
        quantities.clear();
        profits.clear();
        degrees.clear();
        periods.clear();

        // Set identical parameters.
            qLearningParameter = new QLearning.Parameter(0.3,0.95, 1);
    }


    /**
     * Starts simulation, conducts the desired number of simulation runs (numberOfSimulationRuns), and calls
     * required methods to store and export the simulation data.
     */
    private void simulate() {
        if (MARKET_SIZE < 2) {
            throw new IllegalArgumentException("Number of firms must greater than or equal two.");
        }

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
}
