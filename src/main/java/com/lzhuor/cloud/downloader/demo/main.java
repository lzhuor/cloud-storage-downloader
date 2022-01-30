package com.lzhuor.cloud.downloader.demo;

/**
 * This is the main class to run the demo of downloading the same content of files from GCP Cloud Storage & AWS S3 Bucket
 */
public class main {
    public static void main(String[] args) {
        // Download from Google Bucket
        try {
            CloudDownloader cloudDownloader = new CloudDownloader(CloudProvider.GOOGLE_CLOUD);
            Object object = cloudDownloader.download("demo_10k", "account.csv");

            System.out.println("GCP Download success: " + object);
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        // Download from AWS S3
        try {
            CloudDownloader cloudDownloader = new CloudDownloader(CloudProvider.AWS_S3);
            Object object = cloudDownloader.download("demotenk", "account.csv");

            System.out.println("GCP Download success: " + object);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
