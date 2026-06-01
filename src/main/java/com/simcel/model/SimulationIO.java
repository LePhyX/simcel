package com.simcel.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility class for saving and loading simulation state to/from binary files.
 *
 * <p>Uses Java object serialization. The recommended file extension is
 * {@code .simcel}.</p>
 */
public final class SimulationIO {

    private SimulationIO() {}

    /**
     * Serializes a {@link SimulationSnapshot} to a binary file.
     *
     * @param file     destination file
     * @param snapshot snapshot to save
     * @throws IOException if writing fails
     */
    public static void save(File file, SimulationSnapshot snapshot) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(snapshot);
        }
    }

    /**
     * Deserializes a {@link SimulationSnapshot} from a binary file.
     *
     * @param file source file
     * @return the loaded snapshot
     * @throws IOException            if reading fails
     * @throws ClassNotFoundException if the file was created with an incompatible version
     */
    public static SimulationSnapshot load(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (SimulationSnapshot) ois.readObject();
        }
    }
}
