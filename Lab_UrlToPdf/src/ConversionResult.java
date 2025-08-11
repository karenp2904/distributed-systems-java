
public class ConversionResult {
    private final String url;
    private final String outputPath;
    private final String error;
    
    public ConversionResult(String url, String outputPath, String error) {
        this.url = url;
        this.outputPath = outputPath;
        this.error = error;
    }
    
    public boolean isSuccess() { 
        return error == null; 
    }
    
    public String getUrl() { 
        return url; 
    }
    
    public String getOutputPath() { 
        return outputPath; 
    }
    
    public String getError() { 
        return error; 
    }
    
    @Override
    public String toString() {
        if (isSuccess()) {
            return "SUCCESS: " + url + " -> " + outputPath;
        } else {
            return "ERROR: " + url + " -> " + error;
        }
    }
}