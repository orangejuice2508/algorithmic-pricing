package de.unipassau.simulation;

import de.unipassau.simulation.NFor.NFor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;

class QLearning {

    private final double[][] q;         // Two-dimensional Q-matrix
    private final Parameter parameter;  // Parameters of the Q-learning algorithm
    private final Random rnd;           // Random-number generator for exploration
    private final Competition competition;  // Firm's competion object for initializing Q-matrix
    private int numberOfEpisodes;       // Number of episodes that have been conducted.
    private int state;                  // Holds the "old" state.

    /**
     * Constructor of firm's QLearning.
     *
     * @param seed seed for Random-class to reproduce randomness of Q-learning exploration.
     * @param competition Competition object to initialize Q matrix
     */
    QLearning(int seed, Competition competition) {
        q = new double[SimulationManager.sizeOfActionSet][SimulationManager.sizeOfActionSet];
        parameter = new QLearning.Parameter(SimulationManager.qLearningParameter.alpha,
                        SimulationManager.qLearningParameter.delta, SimulationManager.qLearningParameter.gamma);
        rnd = new Random(seed);
        this.competition = competition;

        initQMatrix();
    }

    /**
     * Initializes the Q matrix with the expected value for each state and action given one's opponents
     * randomize uniformly. It uses the NFor-library (https://github.com/BeUndead/NFor) to create an
     * arbitrary number of nested for-loops based on the number of other firms and the size of the action set.
     */
    private void initQMatrix() {
        // Calculate number of competitors
        int numberOfOtherFirms = SimulationManager.MARKET_SIZE - 1;

        /*
        NFor (https://github.com/BeUndead/NFor) is used to create an arbitrary number of nested for-loops. For example:
        To initialize the q matrix in a triopoly (n = 3), for each firm (n-1 = 2) two nested for-loops ranging
        from 0 (from[i] = 0) to sizeOfActionSet (to[i] = 101) are necessary, incrementing each by 1 (by[i]).
         */
        Integer[] from = new Integer[numberOfOtherFirms];
        Integer[] by = new Integer[numberOfOtherFirms];
        Integer[] to = new Integer[numberOfOtherFirms];
        for (int i = 0; i < numberOfOtherFirms; i++) {
            from[i] = 0;
            by[i] = 1;
            to[i] = SimulationManager.sizeOfActionSet;
        }

        NFor<Integer> nFor;
        double[] rowOfQ = new double[SimulationManager.sizeOfActionSet];

        /*
        Initializes each cell of the q matrix with the expected values if each opponent randomizes uniformly.
        Since all rows are identical, only one row has to be calculated and then the values of this row
        can be assigned to each row of the Q matrix
         */
        for (int j = 0; j < SimulationManager.sizeOfActionSet; j++) {
            int finalJ = j;
            nFor = NFor.of(Integer.class).from(from).by(by).to(to);

            double sum = StreamSupport.stream(nFor.spliterator(), true)
                    .mapToDouble(integers -> competition
                            .calculateProfit(finalJ, competition
                                    .calculateDependentVariable(finalJ, integers), numberOfOtherFirms + 1))
                    .sum();

            rowOfQ[j] = sum / ((1 - parameter.delta) * Math.pow(SimulationManager.sizeOfActionSet, numberOfOtherFirms));
        }

        for (int i = 0; i < SimulationManager.sizeOfActionSet; i++) {
            q[i] = rowOfQ.clone();
        }
    }

    /**
     * Carries out one episode of the Q-Learning algorithm. To do so, one first has to determine the current state
     * (= (weighted) price/quantity of the competing firm(s)) and select an action by using a policy
     * (either by exploration or exploitation). Finally, the counter for the number of episodes is increased,
     * beta (necessary for the action-selection policy) is decreased, and the chosen action is returned.
     *
     * @param actionsOfOtherFirms prices/quantities of all other other firms to determine the state.
     * @return result (= price/quantity) of this Q-Learning episode.
     */
    int runEpisode(Integer[] actionsOfOtherFirms) {
        state = getState(actionsOfOtherFirms);

        // Choose A from S using policy.
        int action = selectAction(state);

        numberOfEpisodes++;

        // Decrease epsilon.
        parameter.decreaseEpsilon(numberOfEpisodes);

        return action;
    }

    /**
     * Calculates the state of this Q-Learning algorithm object by weighting the minimum and maximum
     * of all other firms' actions (prices/quantities) with the parameter gamma.
     *
     * @param actionsOfOtherFirms all other firms' actions.
     * @return state of this Q-Learning algorithm object as a weighted number.
     */
    private int getState(Integer[] actionsOfOtherFirms) {
        List<Integer> list = Arrays.asList(actionsOfOtherFirms);
        Collections.sort(list);

        // Get minimum and maximum of actions.
        int minAction = list.get(0);
        int maxAction = list.get(list.size() - 1);

        int result;
        if (minAction == maxAction) {
            // If minimum equals maximum, then return simply the mean.
            result = Math.round((float) ((minAction + maxAction) / 2));
        } else {
            // Else weight the minimum with gamma and the maximum with 1 - gamma.
            result = (int) Math.round(parameter.gamma * minAction + (1 - parameter.gamma) * maxAction);
        }

        return result;
    }

    /**
     * Selects an action according to epsilon-greedy policy. Here, the perceived optimal action is chosen
     * if a random value is greater than the current epsilon (exploit), while a random action gets chosen otherwise
     * (explore).
     *
     * @param state current state of the firm deploying this Q-Learning algorithm.
     * @return action determined by the epsilon-greedy policy.
     */
    private int selectAction(int state) {
        int index;

        if (rnd.nextDouble() > parameter.epsilon) {
            // Exploit
            index = getMaxActionIndex(state);
        } else {
            // Explore
            index = rnd.nextInt(SimulationManager.sizeOfActionSet);
        }

        return index;
    }


    /**
     * Get the action's index of the Q matrix with the highest (optimal) value given a certain state.
     *
     * @param state state to which the optimal action is requested.
     * @return the index of the optimal action given a certain state.
     */
    private int getMaxActionIndex(int state) {
        double value;
        double maxValue = 0;
        int maxIndex = 0;

        for (int i = 0; i < SimulationManager.sizeOfActionSet; i++) {
            value = q[state][i];

            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * For simultaneous interaction:
     * Updates a cell of the Q-matrix based on the previous action (price/quantity) and reward (profit)
     * of the corresponding firm and by determining the new state based on the latest actions (prices/quantities)
     * of all other firms.
     *
     * @param actionsOfOtherFirms latest prices/quantities of all other other firms to determine the new state.
     * @param action previous price/quantity chosen by the corresponding firm.
     * @param reward previous profit generated by the corresponding firm after choosing the action (price/quantity).
     */
    void updateMatrix(int action, Double reward, Integer[] actionsOfOtherFirms) {
        // Get the new state based on all other firms' actions.
        int newState = getState(actionsOfOtherFirms);

        // Observe maxQ for the new state.
        double nextMaxQ = q[newState][getMaxActionIndex(newState)];

        // Update corresponding Q-matrix cell.
        q[state][action] = (1 - parameter.alpha) * q[state][action] + parameter.alpha * (reward + parameter.delta * nextMaxQ);
    }

    /**
     * For sequential interaction:
     * Updates a cell of the Q-matrix based on the previous action (price/quantity) and rewards (profits)
     * of the corresponding firm and by determining the new state based on the latest actions (prices/quantities)
     * of all other firms.
     *
     * @param actionsOfOtherFirms latest prices/quantities of all other other firms to determine the new state.
     * @param action previous price/quantity chosen by the corresponding firm.
     * @param rewards previous profits generated by the corresponding firm after choosing the action (price/quantity).
     */
    public void updateMatrix(int action, List<Double> rewards, Integer[] actionsOfOtherFirms) {
        // Get the new state based on all other firms' actions.
        int newState = getState(actionsOfOtherFirms);

        // Observe maxQ for new state S'.
        double nextMaxQ = q[newState][getMaxActionIndex(newState)];

        // Calculate the total discounted reward (profit) by discounting each accrued reward (profits).
        double discountedTotalReward = 0;
        int index = 0;
        for (; index < rewards.size(); index++) {
            discountedTotalReward = discountedTotalReward + Math.pow(parameter.delta, index) * rewards.get(index);
        }

        // Discount nextMaxQ with the highest exponent, as this reward (profit) accrues at the latest.
        discountedTotalReward = discountedTotalReward + Math.pow(parameter.delta, index) * nextMaxQ;

        // Update corresponding Q-matrix cell.
        q[state][action] = (1 - parameter.alpha) * q[state][action] + parameter.alpha * discountedTotalReward;
    }

    /**
     * Q-Learning parameters:
     *      alpha: learning rate
     *      beta: for decreasing epsilon
     *      delta: discount factor
     *      epsilon: probability for exploring
     *      gamma: weight for calculating the state
     */
    public static class Parameter {
        private double alpha;                   // Learning factor
        private double delta;                   // Discount factor

        private double gamma = 1.0;                   // Weighting factor

        private double epsilon = 1;             // Probability of exploration
        private double beta = 1 - Math.pow(0.000001, epsilon / SimulationManager.maxNumberOfPeriods); // Factor for decreasing epsilon

        public Parameter(double alpha, double delta, double gamma) {
            this.alpha = alpha;
            this.delta = delta;
            this.gamma = gamma;
        }

        public Parameter(double alpha, double delta) {
            this.alpha = alpha;
            this.delta = delta;
        }

        public double getAlpha() {
            return alpha;
        }

        public double getDelta() {
            return delta;
        }

        public double getGamma() {
            return gamma;
        }

        public double getBeta() {
            return beta;
        }

        public void decreaseEpsilon(int numberOfEpisodes) {
            epsilon = Math.pow(1 - beta, numberOfEpisodes);
        }
    }
}