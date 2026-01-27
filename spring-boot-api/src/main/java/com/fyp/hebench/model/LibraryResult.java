package com.fyp.hebench.model;

public class LibraryResult {
    private String library;
    private double keyGenTimeMs;
    private double encryptionTimeMs;
    private double additionTimeMs;
    private double multiplicationTimeMs;
    private double decryptionTimeMs;
    private double totalTimeMs;
    private boolean success;
    private String errorMessage;

    public LibraryResult() {}

    public LibraryResult(String library, double keyGenTimeMs, double encryptionTimeMs,
            double additionTimeMs, double multiplicationTimeMs, double decryptionTimeMs,
            double totalTimeMs, boolean success, String errorMessage) {
        this.library = library;
        this.keyGenTimeMs = keyGenTimeMs;
        this.encryptionTimeMs = encryptionTimeMs;
        this.additionTimeMs = additionTimeMs;
        this.multiplicationTimeMs = multiplicationTimeMs;
        this.decryptionTimeMs = decryptionTimeMs;
        this.totalTimeMs = totalTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public String getLibrary() { return library; }
    public void setLibrary(String library) { this.library = library; }
    public double getKeyGenTimeMs() { return keyGenTimeMs; }
    public void setKeyGenTimeMs(double keyGenTimeMs) { this.keyGenTimeMs = keyGenTimeMs; }
    public double getEncryptionTimeMs() { return encryptionTimeMs; }
    public void setEncryptionTimeMs(double encryptionTimeMs) { this.encryptionTimeMs = encryptionTimeMs; }
    public double getAdditionTimeMs() { return additionTimeMs; }
    public void setAdditionTimeMs(double additionTimeMs) { this.additionTimeMs = additionTimeMs; }
    public double getMultiplicationTimeMs() { return multiplicationTimeMs; }
    public void setMultiplicationTimeMs(double multiplicationTimeMs) { this.multiplicationTimeMs = multiplicationTimeMs; }
    public double getDecryptionTimeMs() { return decryptionTimeMs; }
    public void setDecryptionTimeMs(double decryptionTimeMs) { this.decryptionTimeMs = decryptionTimeMs; }
    public double getTotalTimeMs() { return totalTimeMs; }
    public void setTotalTimeMs(double totalTimeMs) { this.totalTimeMs = totalTimeMs; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
