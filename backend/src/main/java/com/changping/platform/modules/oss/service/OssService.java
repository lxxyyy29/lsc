package com.changping.platform.modules.oss.service;

import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author lxy
 * @Description //OSS 对象存储服务接口，定义文件上传、删除和访问 URL 获取操作
 * @Date 2026/04/18 10:20
 */
public interface OssService {

    /**
     * @Author lxy
     * @Description //上传 MultipartFile 文件到 OSS，返回对象名称
     * @Date 2026/04/18 10:20
     * @Param [file 上传的文件对象, bizType 业务类型，用于构建对象路径前缀]
     * @return String OSS 对象名称
     */
    String uploadFile(MultipartFile file, String bizType);

    /**
     * @Author lxy
     * @Description //上传输入流到 OSS，返回对象名称
     * @Date 2026/04/18 10:20
     * @Param [inputStream 文件输入流, fileName 原始文件名, contentType 文件 MIME 类型, bizType 业务类型]
     * @return String OSS 对象名称
     */
    String uploadFile(InputStream inputStream, String fileName, String contentType, String bizType);

    /**
     * @Author lxy
     * @Description //删除 OSS 中的文件，支持传入完整 URL 或对象名称
     * @Date 2026/04/18 10:20
     * @Param [fileUrl 文件访问 URL 或对象名称]
     * @return boolean 删除是否成功
     */
    boolean deleteFile(String fileUrl);

    /**
     * @Author lxy
     * @Description //根据对象名称拼接完整的文件访问 URL
     * @Date 2026/04/18 10:20
     * @Param [objectName OSS 对象名称]
     * @return String 文件访问 URL
     */
    String getFileUrl(String objectName);
}
