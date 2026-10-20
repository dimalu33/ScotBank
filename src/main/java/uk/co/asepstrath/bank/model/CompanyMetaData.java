package uk.co.asepstrath.bank.model;

public class CompanyMetaData {
    private final String country;
    private final String region;
    private final String sector;

    public CompanyMetaData(String country, String region, String sector) {
        this.country = country == null ? "Uncategorised" : country;
        this.region = region == null ? "Uncategorised" : region;
        this.sector = sector == null ? "Uncategorised" : sector;
    }

    public String getCountry() { return country; }
    public String getRegion() { return region; }
    public String getSector() { return sector; }
}