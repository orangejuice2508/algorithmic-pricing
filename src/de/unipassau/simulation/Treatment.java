package de.unipassau.simulation;

class Treatment {

    private final SimulationRun.Timing timing;
    private final Competition.Type competitionType;
    private final int marketSize;

    /**
     * Creates the Treatment object from the specified SimulationRun.Timing, Competition.Type and marketSize.
     *
     * @param timing the timing of interaction
     * @param competitionType the type of competition
     * @param marketSize the number of firms
     */
    public Treatment(SimulationRun.Timing timing, Competition.Type competitionType, int marketSize) {
        this.timing = timing;
        this.competitionType = competitionType;
        this.marketSize = marketSize;
    }

    /**
     * Calculates the hash code value of a Treatment instance.
     * The hash codes of two instances are the same if their timing, competitionType and marketSize are equal.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (timing != null ? timing.hashCode() : 0);
        result = 31 * result + (competitionType != null ? competitionType.hashCode() : 0);
        result = 31 * result + marketSize;
        return result;
    }

    /**
     * Compare this Treatment with another.
     * Two instances are equal if their timing, competitionType and marketSize are equal.
     *
     * @return true if the two Treatments match.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Treatment other = (Treatment) obj;
        return timing.equals(other.timing)
                && competitionType.equals(other.competitionType) && marketSize == other.marketSize;
    }
}
