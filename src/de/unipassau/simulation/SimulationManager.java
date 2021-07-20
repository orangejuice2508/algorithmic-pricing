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
     * Main-method of the whole Simulation. Creates a new SimulationManager to examine theta.
     */
    public static void main(String[] args) {
        SimulationManager simulationManager = new SimulationManager();
        simulationManager.analyzeTheta();
    }

    /**
     * Manages several simulation runs to examine theta (the degree of substitutability in the economic model),
     * stores their results and exports a .csv file.
     */
    private void analyzeTheta() {
        // Desired scope of theta to be analyzed
        double thetaStart = 0.01;
        double thetaEnd = 0.99;
        double thetaStep = 0.01;

        int numberOfTheta = (int) Math.round((thetaEnd - thetaStart + thetaStep) / thetaStep);
        int numberOfSteps = Math.round(numberOfTheta);

        int stepcounter = 1;

        // Index for rows
        int r = 0;

        // Arrays to store results
        double[] meanPrices = new double[numberOfTheta];
        double[] meanPriceSDs = new double[numberOfTheta];
        double[] meanQuantities = new double[numberOfTheta];
        double[] meanProfits = new double[numberOfTheta];
        double[] meanPeriods = new double[numberOfTheta];
        double[] meanDegrees = new double[numberOfTheta];
        double[] meanPercentages = new double[numberOfTheta];


        System.out.println("Analyzing degree of substitutability theta at " + TIMING + " interaction "
                + "in a " + COMPETITION_TYPE + " competition "
                + "with " + MARKET_SIZE + " firms");

        /*
        For-loops to sequentially conduct all simulations necessary with the desired parameters
        Rounding parameters is necessary to fix precision problem of double values,
        as the number is stored as a binary representation of a fraction and a exponent
        */
        for (double i = thetaStart; Calculation.round(i, 3) <= thetaEnd; i = i + thetaStep) {

            System.out.println("#");

            /*
            The arrays' results of the previous simulation runs are deleted from the Lists
            as they already were saved, and the optimal parameters are set.
             */
            setEnvironment();

            // Desired theta value is set.
            Competition.theta = Calculation.round(i, 2);

            System.out.print(stepcounter + "/" + numberOfSteps
                    + "; theta: " + Competition.theta
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

        // Header: theta
        joiner.add("THETA")
                .add("MEAN PRICE").add("MEAN STANDARD DEVIATION OF THE MARKET PRICE")
                .add("MEAN QUANTITY").add("MEAN PROFIT").add("PERIODS (in mio.)").add("DEGREE OF TACIT COLLUSION")
                .add("PERCENTAGE OF COORDINATION");
        content.append(joiner);
        content.append("\n");

        for (int i = 0; i < meanDegrees.length; i++) {
            joiner = new StringJoiner(",");

            joiner.add(String.valueOf(Calculation.round(thetaStart + i * thetaStep, 4)));
            joiner.add(String.valueOf(meanPrices[i])).add(String.valueOf(meanPriceSDs[i]))
                    .add(String.valueOf(meanQuantities[i])).add(String.valueOf(meanProfits[i]))
                    .add(String.valueOf(meanPeriods[i])).add(String.valueOf(meanDegrees[i]))
                    .add(String.valueOf(meanPercentages[i]));

            content.append(joiner).append("\n");
        }
        // Export procedure
        Export export = Export.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = dateFormat.format(new Date()) + "_thetaAnalysis";
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
}