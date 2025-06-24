package com.example.demo.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import com.example.demo.config.CosClientConfig;
import com.example.demo.constant.LoadConst;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.demo.PicOperationDemo;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

@Slf4j
@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象(附带图片信息)
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片处理(处理动作为获取图片信息)
        PicOperations picOperations = new PicOperations();
        // 设置是否返回图片信息
        picOperations.setIsPicInfo(1);

        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);
        // 仅对>80kB的图片进行压缩
        if (file.length() > 80 * 1024) {
            // 缩略图处理：
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径
            String thumbnailKey = FileUtil.mainName(key) + ".thumbnail" + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            // 缩放规则
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", LoadConst.THUMBNAIL_SIZE, LoadConst.THUMBNAIL_SIZE));
            rules.add(thumbnailRule);
        }
        // 构造处理函数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    public String getPresignedUrl(String imageUrl) {
        try {
            // 从完整URL中解析对象键
            //String objectKey = extractObjectKey(imageUrl);
            log.info("原始URL: {}", imageUrl);
            String objectKey = imageUrl;

            // 设置签名过期时间(30分钟)
            Date expirationDate = new Date(System.currentTimeMillis() + 30 * 60 * 1000);

            // 生成预签名URL
            URL url = cosClient.generatePresignedUrl(
                    cosClientConfig.getBucket(),  // 存储桶名称
                    objectKey,                    // 对象键
                    expirationDate,               // 过期时间
                    HttpMethodName.GET
            );

            log.info("预签名URL: {}", url);
            return url.toString();
        } catch (Exception e) {
            log.error("生成预签名URL失败，原始URL: {}", imageUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成预签名URL失败");
        }
    }

    /**
     * 从完整COS URL中提取对象键
     */
    private String extractObjectKey(String imageUrl) {
        // 示例URL: https://picbackend01-1331768829.cos.ap-shanghai.myqcloud.com/public/1933555623843966978/2025-06-19_izcfy0ox12xa.avif

        // 移除协议头和域名部分
        int domainEndIndex = imageUrl.indexOf(".myqcloud.com/");
        if (domainEndIndex == -1) {
            throw new IllegalArgumentException("无效的COS URL格式");
        }

        // 提取对象键部分
        return imageUrl.substring(domainEndIndex + ".myqcloud.com/".length());
    }

    public String getObjectUrl(String key) {
        int doaminIndex = key.indexOf('?');
        String url = key.substring(0, doaminIndex);
        return url;
    }

}
