/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.importers.model;

/**
 *
 * @author LEVALLOIS
 */
public class UrlLink {

    private String linkText;
    private String link;

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public String getLink() {
        return link;
    }

    public String getLinkFirstChars(Integer nbChars) {
        int maxChar = Math.min(nbChars, link.length()-1);
        return link.substring(0, maxChar);
    }

    public void setLink(String link) {
        this.link = link;
    }
    
    

}
