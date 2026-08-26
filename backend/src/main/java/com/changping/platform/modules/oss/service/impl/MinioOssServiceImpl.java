package com.changping.platform.modules.oss.service.impl;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.modules.oss.config.OssProperties;
import com.changping.platform.modules.oss.service.OssService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author lxy
 * @Description //基于 MinIO 的 OSS 服务实现，提供文件上传、删除和访问 URL 生成功能
 * @Date 2026/04/18 10:22
 */
@Service
@ConditionalOnProperty(prefix = "oss", name = "enable", havingValue = "true")
public class MinioOssServiceImpl implements OssService {

    private final MinioClient minioClient;
    private final OssProperties ossProperties;

    /**
     * @Author lxy
     * @Description //构造函数，注入 MinIO 客户端和 OSS 配置属性
     * @Date 2026/04/18 10:22
     * @Param [minioClient MinIO 客户端, ossProperties OSS 配置属性]
     * @return
     */
    public MinioOssServiceImpl(MinioClient minioClient, OssProperties ossProperties) {
        this.minioClient = minioClient;
        this.ossProperties = ossProperties;
    }

    /**
     * @Author lxy
     * @Description //上传 MultipartFile 文件到 MinIO，返回 OSS 对象名称
     * @Date 2026/04/18 10:22
     * @Param [file 上传的文件对象, bizType 业务类型，用于构建对象路径前缀]
     * @return String OSS 对象名称
     */
    @Override
    public String uploadFile(MultipartFile file, String bizType) {
        try {
            return uploadFile(file.getInputStream(), file.getOriginalFilename(), file.getContentType(), bizType);
        } catch (Exception exception) {
            throw new BusinessException("OSS_UPLOAD_FAILED", "文件上传失败: " + exception.getMessage());
        }
    }

    /**
     * @Author lxy
     * @Description //上传输入流到 MinIO，自动确保存储桶存在后执行上传，返回对象名称
     * @Date 2026/04/18 10:22
     * @Param [inputStream 文件输入流, fileName 原始文件名, contentType 文件 MIME 类型, bizType 业务类型]
     * @return String OSS 对象名称
     */
    @Override
    public String uploadFile(InputStream inputStream, String fileName, String contentType, String bizType) {
        try {
            ensureBucketExists();
            String objectName = generateObjectName(bizType, fileName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(ossProperties.getBucket())
                    .object(objectName)
                    .stream(inputStream, -1, 10 * 1024 * 1024)
                    .contentType(StringUtils.hasText(contentType) ? contentType : "application/octet-stream")
                    .build());
            return objectName;
        } catch (Exception exception) {
            throw new BusinessException("OSS_UPLOAD_FAILED", "文件上传失败: " + exception.getMessage());
        }
    }

    /**
     * @Author lxy
     * @Description //从 MinIO 删除文件，支持传入完整 URL 或对象名称，失败时返回 false
     * @Date 2026/04/18 10:22
     * @Param [fileUrlOrObjectName 文件访问 URL 或对象名称]
     * @return boolean 删除是否成功
     */
    @Override
    public boolean deleteFile(String fileUrlOrObjectName) {
        try {
            String objectName = fileUrlOrObjectName;
            // If it looks like a full URL, extract the object name
            if (fileUrlOrObjectName.startsWith("http://") || fileUrlOrObjectName.startsWith("https://")) {
                objectName = extractObjectName(fileUrlOrObjectName);
            }
            if (!StringUtils.hasText(objectName)) {
                return false;
            }
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(ossProperties.getBucket())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * @Author lxy
     * @Description //根据对象名称拼接完整的文件访问 URL
     * @Date 2026/04/18 10:22
     * @Param [objectName OSS 对象名称]
     * @return String 文件访问 URL
     */
    @Override
    public String getFileUrl(String objectName) {
        String accessUrl = ossProperties.getAccess();
        if (!StringUtils.hasText(accessUrl)) {
            throw new BusinessException("OSS_ACCESS_URL_MISSING", "OSS 访问地址未配置");
        }
        if (!accessUrl.endsWith("/")) {
            accessUrl += "/";
        }
        return accessUrl + objectName;
    }

    /**
     * @Author lxy
     * @Description //确保存储桶存在，若不存在则自动创建
     * @Date 2026/04/18 10:22
     * @Param []
     * @return void
     */
    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(ossProperties.getBucket())
                .build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(ossProperties.getBucket())
                    .build());
        }
    }

    /**
     * @Author lxy
     * @Description //生成 OSS 对象名称，格式为 bizType/yyyy/MM/dd/随机8位UUID+扩展名
     * @Date 2026/04/18 10:22
     * @Param [bizType 业务类型, fileName 原始文件名]
     * @return String 生成的对象名称
     */
    private String generateObjectName(String bizType, String fileName) {
        String normalizedBizType = StringUtils.hasText(bizType) ? bizType.trim() : "common";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String extension = "";
        if (StringUtils.hasText(fileName) && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf('.'));
        }
        return String.format("%s/%s/%s%s", normalizedBizType, date, uuid, extension);
    }

    /**
     * @Author lxy
     * @Description //从完整 URL 中提取 OSS 对象名称，优先剥离访问前缀，次用存储桶路径解析
     * @Date 2026/04/18 10:22
     * @Param [fileUrl 文件访问 URL]
     * @return String 对象名称，解析失败返回 null
     */
    private String extractObjectName(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        // Try stripping the access prefix first
        String accessUrl = ossProperties.getAccess();
        if (StringUtils.hasText(accessUrl)) {
            if (!accessUrl.endsWith("/")) accessUrl += "/";
            if (fileUrl.startsWith(accessUrl)) {
                return fileUrl.substring(accessUrl.length());
            }
        }
        // Fallback: look for bucket segment (for legacy URLs)
        String bucketSegment = ossProperties.getBucket() + "/";
        int index = fileUrl.indexOf(bucketSegment);
        if (index < 0) {
            return null;
        }
        return fileUrl.substring(index + bucketSegment.length());
    }
}
