package com.fyp.hebench.model;

import java.util.List;

public class ComparisonResponse {
    private List<LibraryResult> results;

    public ComparisonResponse() {}

    public ComparisonResponse(List<LibraryResult> results) {
        this.results = results;
    }

    public List<LibraryResult> getResults() { return results; }
    public void setResults(List<LibraryResult> results) { this.results = results; }
}
