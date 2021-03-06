package com.skyword.api.feed;

/**
 * A class to hold the datatype for a file attachment which includes the bytes of the file and the mime type.
 * @author john
 *
 */
public class FileAttachment {
    String mimeType;
    byte[] fileData;

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the fileData
     */
    public byte[] getFileData() {
        return fileData;
    }

    /**
     * @param fileData the fileData to set
     */
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

}
