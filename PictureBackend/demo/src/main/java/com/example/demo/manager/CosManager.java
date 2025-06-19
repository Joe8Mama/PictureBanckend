package com.example.demo.manager;

import cn.hutool.core.io.FileUtil;
import com.example.demo.config.CosClientConfig;
import com.example.demo.constant.LoadConst;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.demo.PicOperationDemo;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

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
        // 图片压缩（转成 avif 格式）
        String avifKey = FileUtil.mainName(key) + ".avif";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(avifKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/avif");
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
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }


}
