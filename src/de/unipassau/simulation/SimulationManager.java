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
    static final int numberOfSimulationRuns = 1;                                      // Desired number of single simulation runs
    static final int sizeOfActionSet = 101;                                             // Total number of possible states and actions
    static final int maxNumberOfPeriods = sizeOfActionSet * sizeOfActionSet * 5000;     // Maximum number of periods per simulation run
    static final int minNumberOfConvergedPeriods = (int) (maxNumberOfPeriods * 0.0001); // Number of identical periods necessary to assume convergence of the algorithms
    static final int sizeOfExaminationInterval = minNumberOfConvergedPeriods;           // Size of interval to be examined; only relevant if the firms' states do not end up at a fix price or quantity level but oscillate in a certain interval.
    static QLearning.Parameter qLearningParameter;                                      // Parameter for Q-Learning

    // Export settings
    static final boolean detailedExport = true;
    static int firstPeriodForDetailedExport;
    static int lastPeriodForDetailedExport;

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

        if (detailedExport) {
            for (SimulationRun simulationRun : simulationRuns) {
                detailedExport(simulationRun);
            }
        }

    }

    /**
     * Exports detailed data of a simulation run including each firms prices, quantities and profits (as integers).
     * It exports all the data from the period specified by firstPeriodForDetailedExport
     * until the period specified by lastPeriodForDetailedExport.
     *
     * @param simulationRun the simulation run of which the data should be exported.
     */
    private void detailedExport(SimulationRun simulationRun) {
        // Get or create instance of the Export class
        Export export = Export.getInstance();

        // Create StringBuilder to add export data to
        StringBuilder content = new StringBuilder();
        StringJoiner joiner = new StringJoiner(",", ",", "");
        List<Firm> firms = simulationRun.getFirms();

        // Add headers: Period, Prices/Quantities/Profits of all firms
        content.append("Period");
        int numberOfFirms = firms.size();
        for (int i = 0; i < numberOfFirms; i++) {
            int currentFirm = i + 1;
            joiner.add("Price of firm " + currentFirm);
            joiner.add("Quantity of firm " + currentFirm);
            joiner.add("Profit of firm " + currentFirm);
        }
        content.append(joiner).append("\n");

        // Add data as rows
        for (int i = SimulationManager.firstPeriodForDetailedExport - 1; i < SimulationManager.lastPeriodForDetailedExport; i++) {
            content.append((i + 1));
            joiner = new StringJoiner(",", ",", "");
            for (Firm firm : firms) {
                joiner.add(firm.getPricesForDetailedExport().get(i).toString());
                joiner.add(firm.getQuantitiesForDetailedExport().get(i).toString());
                joiner.add(firm.getProfitsForDetailedExport().get(i).toString());
            }
            content.append(joiner).append("\n");
        }

        // Get date format to use as filename
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = dateFormat.format(new Date());
        filename += "_detailed_output";

        export.exportCsv(filename, content.toString());

        System.out.println("File " + filename + ".csv exported");
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
