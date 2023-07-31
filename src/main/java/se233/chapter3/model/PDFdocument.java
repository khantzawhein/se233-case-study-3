package se233.chapter3.model;

import org.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class PDFdocument {
    private String name;
    private String filePath;
    private PDDocument document;
    private LinkedHashMap<String, ArrayList<FileFreq>> uniqueSets;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public PDDocument getDocument() {
        return document;
    }

    public void setDocument(PDDocument document) {
        this.document = document;
    }

    public LinkedHashMap<String, ArrayList<FileFreq>> getUniqueSets() {
        return uniqueSets;
    }

    public void setUniqueSets(LinkedHashMap<String, ArrayList<FileFreq>> uniqueSets) {
        this.uniqueSets = uniqueSets;
    }

    public PDFdocument(String filePath) throws IOException {
        this.name = Paths.get(filePath).toString();
        this.filePath = filePath;
        File pdfFile = new File(filePath);
        this.document = PDDocument.load(pdfFile);
    }
}
