package com.careeriq.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ))
            .build();
    }

    /**
     * Upload a resume PDF to S3.
     * Returns the S3 key (path) for storage in the candidates table.
     */
    public String uploadResume(MultipartFile file, String userEmail) throws IOException {
        String key = "resumes/%s/%s.pdf".formatted(
            userEmail.replace("@", "_").replace(".", "_"),
            UUID.randomUUID()
        );

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType("application/pdf")
            .contentLength(file.getSize())
            .build();

        s3Client.putObject(request, RequestBody.fromInputStream(
            file.getInputStream(), file.getSize()));

        log.info("[S3] Uploaded resume: key={}, size={}KB", key, file.getSize() / 1024);
        return key;
    }

    /**
     * Upload a generated ATS resume PDF.
     */
    public String uploadAtsResume(byte[] pdfBytes, String candidateId, String jobId) {
        String key = "ats-resumes/%s/%s.pdf".formatted(candidateId, jobId);

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType("application/pdf")
            .contentLength((long) pdfBytes.length)
            .build();

        s3Client.putObject(request, RequestBody.fromBytes(pdfBytes));

        log.info("[S3] Uploaded ATS resume: key={}", key);
        return key;
    }

    /**
     * Delete a file from S3.
     */
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build());
        log.info("[S3] Deleted: key={}", key);
    }
}
