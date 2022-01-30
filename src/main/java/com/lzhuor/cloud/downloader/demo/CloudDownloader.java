package com.lzhuor.cloud.downloader.demo;

import com.google.common.base.Supplier;
import com.google.common.io.Files;
import com.lzhuor.cloud.downloader.demo.exceptions.CloudDownloaderException;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.io.FileUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.domain.Credentials;
import org.jclouds.googlecloud.GoogleCredentialsFromJson;
import org.jclouds.googlecloudstorage.GoogleCloudStorageApi;
import org.jclouds.googlecloudstorage.domain.GoogleCloudStorageObject;
import org.jclouds.http.HttpRequest;
import org.jclouds.s3.S3Client;
import org.jclouds.s3.domain.ObjectMetadata;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.google.common.base.Charsets.UTF_8;
import static org.jclouds.b2.reference.B2Headers.FILE_NAME;

public class CloudDownloader {
    private static final int MAX_DOWNLOAD_BYTES = 1024 * 1024 * 1024; // 100mb

    private static final int SIGNED_URL_VALIDITY_SECONDS = 60; // 60secs

    /**
     * The identity of the Cloud Provider. Example: Service Account Name for GCP; Access ID for AWS.
     */
    String identity;

    /**
     * The credential of the Cloud Provider. Example: The private key for GCP derived from the access the JSON file; The Secret Key for AWS.
     */
    String credential;

    /**
     * The Cloud-agnostic Blob Storage Context from Apache JCloud
     */
    BlobStoreContext blobStoreContext;

    /**
     *  The enum of the Cloud Provider.
     */
    CloudProvider cloudProvider;

    /**
     * This is the constructor of the cloud-agnostic file downloader utilising JClouds from Apache Foundation
     *
     * @param cloudProvider - The CloudProvider enum that describes the source of the files to download
     * @throws Exception
     */
    public CloudDownloader(CloudProvider cloudProvider) throws Exception {
        this.cloudProvider = cloudProvider;

        boolean isGcpDownloader = cloudProvider.equals(CloudProvider.GOOGLE_CLOUD);

        // Initialise Environment Values from .env file in the resources folder
        Dotenv dotenv = Dotenv.load();

        if (isGcpDownloader) {
            String googleCloudIdentity = dotenv.get("GOOGLE_CLOUD_CREDENTIAL_PATH");
            this.identity = dotenv.get("GOOGLE_CLOUD_SERVICE_ACCOUNT");
            this.credential = getCredentialFromJsonKeyFile(googleCloudIdentity);
        } else {
            String awsAccessId = dotenv.get("AWS_ACCESS_ID");
            String awsSecretKey = dotenv.get("AWS_ACCESS_SECRET");

            this.identity = awsAccessId;
            this.credential = awsSecretKey;
        }

        String provider = getCloudProviderString(cloudProvider);

        BlobStoreContext context = this.initialiseBlobStoreContext(provider, identity, credential);

        this.blobStoreContext = context;
    }


    /**
     * Initialise the BlobStoreContext - abstracted it out for the ease of writing unit tests if needed
     *
     * @param provider - the plain text of the Cloud Provider from JClouds.
     * @param identity - the identity of the Cloud Provider. Example: Service Account Name for GCP; Access ID for AWS.
     * @param credential - the credential of the Cloud Provider. Example: The private key for GCP derived from the access the JSON file; The Secret Key for AWS.
     * @return
     */
    public BlobStoreContext initialiseBlobStoreContext(String provider, String identity, String credential) {
        return ContextBuilder.newBuilder(provider)
                .credentials(identity, credential)
                .buildView(BlobStoreContext.class);
    }

    /**
     * @param bucket - The name of the container / bucket
     * @param file - The path of the object which is also known as file name
     * @return
     * @throws CloudDownloaderException
     */
    public Object download(String bucket, String file) throws CloudDownloaderException {
        try {
            return cloudProvider.equals(CloudProvider.GOOGLE_CLOUD) ? this.downloadFromGcpBucket(bucket, file) : this.downloadFromAwsS3(bucket, file);
        } catch (Exception e) {
            throw new CloudDownloaderException(String.format("Failed to download file from the cloud storage provider %s, Bucket Name: %s, File: %s, original message: %s", this.cloudProvider, bucket, file, e.getMessage()));
        } finally {
            this.blobStoreContext.close();
        }

    }


    // TODO: @lzhuor Any subsequent handling after downloading the file could be achieved with post-calling this method. For example, reading the downloaded file to retrieve certain information from the file
    /**
     * This is a method to generate download link for SIGNED_URL_VALIDITY_SECONDS period of time
     *
     * @param bucket - The name of the container / bucket
     * @param file - The path of the object which is also known as file name
     * @return A URL with SIGNED_URL_VALIDITY_SECONDS to download the file
     * @throws IOException
     */
    public URI getAssetDirectLink(String bucket, String file) throws IOException {
        HttpRequest httpRequest = this.blobStoreContext.getSigner().signGetBlob(bucket, file, SIGNED_URL_VALIDITY_SECONDS);
        if (httpRequest == null) {
            throw new IOException(String.format("No object found to creat signed url, bucket=%s, file=%s", bucket, file));
        }

        return httpRequest.getEndpoint();
    }

    private Object downloadFromGcpBucket(String bucket, String file) throws CloudDownloaderException, IOException {
        GoogleCloudStorageApi api = this.blobStoreContext.unwrapApi(GoogleCloudStorageApi.class);
        GoogleCloudStorageObject object = api.getObjectApi().getObject(bucket, file);

        if (object.metadata().size() > MAX_DOWNLOAD_BYTES) {
            throw new CloudDownloaderException(String.format("File size over maxBytes=%s", MAX_DOWNLOAD_BYTES));
        } else {
            URI uri = this.getAssetDirectLink(bucket, file);
            this.savefile(uri);
        }

        return object;
    }

    private Object downloadFromAwsS3(String bucket, String file) throws CloudDownloaderException, IOException {
        S3Client api = this.blobStoreContext.unwrapApi(S3Client.class);
        ObjectMetadata object = api.headObject(bucket, file);

        if (object.getContentMetadata().getContentLength() > MAX_DOWNLOAD_BYTES) {
            throw new CloudDownloaderException(String.format("File size over maxBytes=%s", MAX_DOWNLOAD_BYTES));
        } else {
            URI uri = this.getAssetDirectLink(bucket, file);
            this.savefile(uri);
        }

        return object;
    }

    private String getCloudProviderString(CloudProvider cloudProvider) throws Exception {
        if (cloudProvider.equals(CloudProvider.GOOGLE_CLOUD)) {
            // hardcoded value from jclouds docs for GCP Bucket
            return "google-cloud-storage";
        } else if (cloudProvider.equals(CloudProvider.AWS_S3)) {
            // hardcoded value from jclouds docs for AWS S3
            return "aws-s3";
        } else {
            throw new Exception("Unknown cloud provider: " + cloudProvider.toString());
        }
    }

    // TODO: @lzhuor We can utilise or modify this method to control the path of the saved files.
    /**
     * This is the method that saves the file from the URI inputStream
     *
     * @param uri The URI of java.net to download the file
     * @throws IOException
     */
    private void savefile(URI uri) throws IOException {
        try (InputStream inputStream = uri.toURL().openStream()) {

            String filePath = String.format("./download/account_from_%s.csv", this.cloudProvider == CloudProvider.GOOGLE_CLOUD ? "_gcp" : "_aws");

            File file = new File(filePath);

            FileUtils.copyInputStreamToFile(inputStream, file);
        }
    }

    private static String getCredentialFromJsonKeyFile(String filename) {
        try {
            String fileContents = Files.toString(new File(filename), UTF_8);
            Supplier<Credentials> credentialSupplier = new GoogleCredentialsFromJson(fileContents);
            String credential = credentialSupplier.get().credential;
            return credential;
        } catch (IOException e) {
            System.err.println(String.format("Exception reading private key from '%s'", filename));
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }
}
