# Nordea Homework - Text Parser

## Purpose

This program reads a text file, breaks it down into individual sentences, and then further breaks down each sentence into alphabetically sorted words. The results are saved in two formats: XML and CSV.

## Usage

To run the program, you need Java 21 or later installed. You can run it from the command line after building the project (e.g., using Maven).

Assuming you have built a JAR file named `nordeahomework.jar`:

bash java -jar nordeahomework.jar `[input_file_name]` `[output_directory]`

## Arguments

*   **`[input_file_name]`** (Optional): The name of the text file to process (e.g., `small.in`).
    *   It should be located in the `src/test/resources/in` directory.
    *   If not provided, it defaults to `small.in`.
*   **`[output_directory]`** (Optional): The directory where the output XML and CSV files will be saved.
    *   If not provided, it defaults to `src/test/resources/out`. The directory will be created if it doesn't exist.

## Dependencies

The project relies on the following main libraries:

*   **Apache OpenNLP:** For natural language processing tasks like sentence detection and tokenization (word splitting).
*   **SLF4J & Logback:** For logging application events and errors.
*   **JUnit 5 & XMLUnit:** For testing purposes.