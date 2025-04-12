package com.tarasiuk.nordeahomework;

import com.tarasiuk.nordeahomework.domain.Sentence;
import com.tarasiuk.nordeahomework.output.CsvWriter;
import com.tarasiuk.nordeahomework.output.XmlWriter;
import com.tarasiuk.nordeahomework.processing.TextProcessor;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    private static final String DEFAULT_INPUT_DIR = "src/main/resources/in";
    private static final String DEFAULT_OUTPUT_DIR = "src/main/resources/out";

    public static void main(String[] args) {
        System.out.println("Starting text processing...");

        // --- Configuration ---
        // Use command line arguments or defaults
        String inputFileName = args.length > 0 ? args[0] : "/small.in"; // Default to small.in
        String baseOutputName = args.length > 1 ? args[1] : inputFileName.replaceFirst("[.][^.]+$", ""); // Base name for output

        Path inputFile;
        Path xmlOutputFile;
        Path csvOutputFile;

        try {
            // Resolve input path (look in resources first, then current dir)
            Path resourcePath = Paths.get(DEFAULT_INPUT_DIR, inputFileName);
            if (Files.exists(resourcePath)) {
                inputFile = resourcePath;
            } else {
                inputFile = Paths.get(inputFileName); // Try current directory if not in resources
            }


            // Ensure output directory exists
            Path outputDir = Paths.get(DEFAULT_OUTPUT_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                System.out.println("Created output directory: " + outputDir.toAbsolutePath());
            }

            xmlOutputFile = outputDir.resolve(baseOutputName + ".xml");
            csvOutputFile = outputDir.resolve(baseOutputName + ".csv");

        } catch (InvalidPathException | IOException e) {
            System.err.println("Error setting up file paths: " + e.getMessage());
            return;
        }

        System.out.println("Input file: " + inputFile.toAbsolutePath());
        System.out.println("XML output file: " + xmlOutputFile.toAbsolutePath());
        System.out.println("CSV output file: " + csvOutputFile.toAbsolutePath());

        if (!Files.exists(inputFile)) {
            System.err.println("Error: Input file not found at " + inputFile.toAbsolutePath());
            return;
        }


        // --- Processing ---
        TextProcessor processor = new TextProcessor();
        XmlWriter xmlWriter = new XmlWriter();
        CsvWriter csvWriter = new CsvWriter();

        try {
            List<Sentence> sentences = processor.parseText(inputFile);
            System.out.println("Successfully parsed " + sentences.size() + " sentences.");

            // --- Output Generation ---
            xmlWriter.write(xmlOutputFile, sentences);
            System.out.println("Successfully wrote XML output.");

            csvWriter.write(csvOutputFile, sentences);
            System.out.println("Successfully wrote CSV output.");

            System.out.println("Processing finished successfully.");

        } catch (IOException e) {
            System.err.println("Error during file reading/writing: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
        } catch (XMLStreamException e) {
            System.err.println("Error writing XML file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) { // Catch unexpected errors
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}