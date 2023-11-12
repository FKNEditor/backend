package au.com.expressionless.nish.service;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import au.com.expressionless.nish.utils.ANSIColour;
import au.com.expressionless.nish.utils.GeneralUtils;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Item;

import static au.com.expressionless.nish.constants.Config.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MinIO {
    public static final String PDF_BUCKET = "pdf";
    public static final String THUMBNAIL_BUCKET = "thumbnail";
    public static final String QUILL_DELTA_BUCKET = "quill-delta";
    public static final String HTML_BUCKET = "html";
    Logger log  = Logger.getLogger(MinIO.class);

    MinioClient minioClient;


    public MinIO() {
        String minioUrl = GeneralUtils.getSystemEnv(ENV_MINIO_URL, "http://localhost:9000");
        String minioPass = GeneralUtils.getSystemEnv(ENV_MINIO_PASS, "password");
        String minioUser = GeneralUtils.getSystemEnv(ENV_MINIO_USER, "admin");
        log.info("User: " + minioUser);
        log.info("Pass: " + minioPass);
        minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioUser, minioPass)
                .build();
        makeBucket(PDF_BUCKET);
        makeBucket(THUMBNAIL_BUCKET);
        makeBucket(QUILL_DELTA_BUCKET);
        makeBucket(HTML_BUCKET);
        log.info(ANSIColour.doPurple("Created connection to minio client at: " + minioUrl + " successful!"));
    }

    public boolean bucketExists(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            log.info(bucketName + " exists: " + exists);
            return exists;
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e) {
                    return false;
                }
    }

    public Map<String, Object> listObjectsInBucket(String bucketName, long limit) {
                // Lists objects information.
        Iterable<Result<Item>> results = minioClient.listObjects(
            ListObjectsArgs.builder().bucket(bucketName).build());
        List<Item> res = new ArrayList<>();
        long total = 0;
        for(Result<Item> result : results) {
            if(++total > limit && limit > 0) {
                log.info("Skipping. Limit reached");
                continue;
            }
            try {
                res.add(result.get());
            } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException
                    | InternalException | InvalidResponseException | NoSuchAlgorithmException | ServerException
                    | XmlParserException | IOException e) {
                log.error("Error during fetch of results: " + e.getMessage());
            }
        }
        String line = "Found " + total + " items. Sending " + (limit > 0 ? limit : "all") + " items from bucket: " + bucketName;
        log.info(ANSIColour.doGreen(line));

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("total", total);
        resMap.put("limit", limit);
        resMap.put("items", res);

        return resMap;
    }

    public void makeBucket(String bucketName) {
        boolean exists = bucketExists(bucketName);
        if(exists) {
            log.info(ANSIColour.doCyan("Bucket: " + bucketName + " already exists!"));
            return;
        }
        try {
            minioClient.makeBucket(
                MakeBucketArgs
                    .builder()
                    .bucket(bucketName)
                    .build());
            log.info(ANSIColour.doCyan("Bucket creation: " + bucketName + " successful!"));
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e) {
            // God so many exception types!!!
            log.error(ANSIColour.doColour("What happened here?? " + e.getMessage(), ANSIColour.RED));
            e.printStackTrace();
        }
    }

    private ObjectWriteResponse uploadToBucket(
        String bucketName, 
        String contentType, 
        File f, 
        String fileName) {
            log.info("Path: " + f.getAbsolutePath());
            UploadObjectArgs args;
            try {
                args = UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .filename(f.getAbsolutePath())
                    .contentType(contentType)
                    .build();
            } catch (IllegalArgumentException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }

            try {
                return minioClient.uploadObject(args);
            } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | 
                    InternalException | InvalidResponseException | NoSuchAlgorithmException | 
                    ServerException | XmlParserException| IOException e) {
                log.error("Error occurred during upload of: " + fileName);
                log.error("Upload details: ");
                log.error("File: " + f.getAbsolutePath());
                e.printStackTrace();
                return null;
            }
    }

    public ObjectWriteResponse uploadPdf(File f, String fileName) {
        return uploadToBucket(PDF_BUCKET, "application/pdf", f, fileName);
    }

    public ObjectWriteResponse uploadThumbnail(File f, String fileName) {
        return uploadToBucket(THUMBNAIL_BUCKET, "application/jpeg", f, fileName);
    }

    public ObjectWriteResponse uploadDelta(File f, String fileName) {
        return uploadToBucket(QUILL_DELTA_BUCKET, "application/json", f, fileName);
    }

    public ObjectWriteResponse uploadHtml(File f, String fileName) {
        return uploadToBucket(HTML_BUCKET, "application/json", f, fileName);
    }

    public GetObjectResponse downloadThumbnail(String name) throws ErrorResponseException {
        return downloadObject(THUMBNAIL_BUCKET, name);
    }

    public GetObjectResponse downloadPdf(String name) throws ErrorResponseException {
        return downloadObject(PDF_BUCKET, name);
    }

    public GetObjectResponse downloadDelta(String name) throws ErrorResponseException {
        return downloadObject(QUILL_DELTA_BUCKET, name);
    }

    public GetObjectResponse downloadHtml(String name) throws ErrorResponseException {
        return downloadObject(HTML_BUCKET, name);
    }

    public GetObjectResponse downloadObject(String bucket, String name) throws ErrorResponseException {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
            .bucket(bucket)
            .object(name)
            .build());
        } catch (InvalidKeyException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public void deletePdf(String name) {
        deleteObject(PDF_BUCKET, name);
    }

    public void deleteDelta(String name) {
        deleteObject(QUILL_DELTA_BUCKET, name);
    }

    public void deleteThumbnail(String name) {
        deleteObject(THUMBNAIL_BUCKET, name);
    }

    public void deleteHtml(String name) {
        deleteObject(HTML_BUCKET, name);
    }

    private void deleteObject(String bucket, String name) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket(bucket)
            .object(name)
            .build()
            );

        } catch (InvalidKeyException | InsufficientDataException | InternalException
                | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                | IllegalArgumentException | IOException | ErrorResponseException e) {
            e.printStackTrace();
        }
    }

}
