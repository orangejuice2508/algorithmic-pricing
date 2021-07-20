package de.unipassau.simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


class Export {
    private static Export export;
    private final String directory;

    /**
     * Sets directory where file is supposed to be exported.
     */
    private Export() {
        String timing = SimulationManager.TIMING.toString();
        String competitionType = SimulationManager.COMPETITION_TYPE.toString();
        int marketSize = SimulationManager.MARKET_SIZE;

        directory = "out/" + timing.substring(0,3) + "-" + competitionType.charAt(0)  + "-" + marketSize;

        new File(directory).mkdirs();
    }


    /**
     * Implements Singleton Pattern to ensure that only one instance of this class gets created.
     *
     * @return instance of class Export.
     */
    static Export getInstance() {
        if (Export.export == null) {
            Export.export = new Export();
        }
        return Export.export;
    }


    /**
     * Exports a .csv-file with passed data using Java FileWriter
     * (https://bit.ly/3usY45c).
     *
     * @param filename name of the .csv-file.
     * @param data     data of the .csv-file.
     */
    void exportCsv(String filename, String data) {
        FileWriter fileWriter = null;
        String name = "/" + filename + ".csv";

        try {
            fileWriter = new FileWriter(directory + name);
            fileWriter.append(data);
        } catch (Exception e) {
            System.out.println("Error in fileWriter!");
            e.printStackTrace();
        } finally {

            try {
                assert fileWriter != null;
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing file!");
                e.printStackTrace();
            }

        }
    }

}
