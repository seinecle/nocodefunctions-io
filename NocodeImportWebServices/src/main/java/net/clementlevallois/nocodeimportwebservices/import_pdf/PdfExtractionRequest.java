package net.clementlevallois.nocodeimportwebservices.import_pdf;


public class PdfExtractionRequest {
    private String jobId;
    private String callbackURL;
    private String fileName;
    private boolean allPages;
    private Integer selectedPage;
    private Float leftCornerX;
    private Float leftCornerY;
    private Float width;
    private Float height;
    private String owner;

    // Default constructor
    public PdfExtractionRequest() {
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getCallbackURL() {
        return callbackURL;
    }

    public void setCallbackURL(String callbackURL) {
        this.callbackURL = callbackURL;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileNamePrefix(String fileName) {
        this.fileName = fileName;
    }

    public boolean isAllPages() {
        return allPages;
    }

    public void setAllPages(boolean allPages) {
        this.allPages = allPages;
    }

    public Integer getSelectedPage() {
        return selectedPage;
    }

    public void setSelectedPage(Integer selectedPage) {
        this.selectedPage = selectedPage;
    }

    public Float getLeftCornerX() {
        return leftCornerX;
    }

    public void setLeftCornerX(Float leftCornerX) {
        this.leftCornerX = leftCornerX;
    }

    public Float getLeftCornerY() {
        return leftCornerY;
    }

    public void setLeftCornerY(Float leftCornerY) {
        this.leftCornerY = leftCornerY;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}