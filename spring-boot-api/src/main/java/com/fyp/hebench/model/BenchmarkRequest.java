package com.fyp.hebench.model;

public class BenchmarkRequest {
    private String library;
    private int numOperations;

    public BenchmarkRequest() {}

    public BenchmarkRequest(String library, int numOperations) {
        this.library = library;
        this.numOperations = numOperations;
    }

    public String getLibrary() { return library; }
    public void setLibrary(String library) { this.library = library; }
    public int getNumOperations() { return numOperations; }
    public void setNumOperations(int numOperations) { this.numOperations = numOperations; }
}
