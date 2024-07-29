/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */
package net.clementlevallois.importers.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author LEVALLOIS
 */
public class ImagesPerFile implements Serializable {

    String fileName;
    byte[][] images;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[][] getImages() {
        return images;
    }

    public byte[][] getANumberOfImages(boolean shrink, int nbOfImages) {
        if (!shrink) {
            return images;
        } else {
            int shrunkSize = Math.min(images.length, nbOfImages);
            return Arrays.copyOf(images, shrunkSize);
        }
    }

    public void setImages(byte[][] images) {
        this.images = images;
    }

    public byte[] getImage(int nb) {
        return images[nb];
    }

}
