package com.example.demo.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.demo.config.CosClientConfig;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.manager.CosManager;
import com.example.demo.model.dto.file.UploadPictureResult;
import com.example.demo.utils.ColorSimilarUtils;
import com.example.demo.utils.ColorTransformUtils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosClientConfig cosClientConfig;

    @Autowired
    private CosManager cosManager;

    /**
     * 上传文件
     * @param inputSource    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 文件路径
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 校验图片
        validPicture(inputSource);
        // 图片上传地址
        String uuid = RandomUtil.randomString(12);
        String originalFilename = getOriginFilename(inputSource);
        // 自拼接文件上传路径，保障安全性
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        // 解析结果并返回
        File file = null;
        try {
            // 获取临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSource, file);
            // 上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取到图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 封装压缩图的返回结果
                CIObject compressCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressCiObject;
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(0);
                    compressCiObject = objectList.get(1);
                }
                return buildResult(originalFilename, compressCiObject, thumbnailCiObject, imageInfo);
            }
            return buildResult(originalFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 临时文件清理
            this.deleteTempFile(file);
        }

    }

    /**
     * 封装返回对象
     * @param originalFilename 原始文件名
     * @param compressCiObject 压缩图对象
     * @param thumbnailCiObject  缩略图对象
     * @param imageInfo 图片信息对象
     * @return 返回对象
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        // 计算宽高
        int pictureWidth = compressCiObject.getWidth();
        int pictureHeight = compressCiObject.getHeight();
        double pictureScale = NumberUtil.round(pictureWidth * 1.0 / pictureHeight, 2).doubleValue();
        // 封装并返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // 设置压缩后的图片路径
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(pictureWidth);
        uploadPictureResult.setPicHeight(pictureHeight);
        uploadPictureResult.setPicScale(pictureScale);
        uploadPictureResult.setPicFormat(compressCiObject.getFormat());
        String aveColor = ColorTransformUtils.getStandardColor(imageInfo.getAve());
        uploadPictureResult.setPicColor(aveColor);
        // 设置缩略图路径
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());

        // 返回文件路径
        return uploadPictureResult;
    }

    /**
     * 封装返回对象
     * @param imageInfo 图片信息对象
     * @param uploadPath 上传路径
     * @param originalFilename 原始文件名
     * @param file 文件
     * @return 返回对象
     */
    private UploadPictureResult buildResult(String originalFilename, File file, String uploadPath, ImageInfo imageInfo) {
        // 计算宽高
        int pictureWidth = imageInfo.getWidth();
        int pictureHeight = imageInfo.getHeight();
        double pictureScale = NumberUtil.round(pictureWidth * 1.0 / pictureHeight, 2).doubleValue();
        // 封装并返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(pictureWidth);
        uploadPictureResult.setPicHeight(pictureHeight);
        uploadPictureResult.setPicScale(pictureScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());

        // 返回文件路径
        return uploadPictureResult;
    }


    /**
     * 删除临时文件
     *
     * @param file 临时文件
     */
    public void deleteTempFile(File file) {
        if (file != null) {
            // 删除临时文件
            boolean delete = file.delete();
            if (!delete) {
                log.error("file delete error, filepath = " + file.getAbsolutePath());
            }
        }
    }

    // todo 新增方法

    /**
     * 处理文件
     * @param inputSource 文件来源
     * @param file 文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 获取原始文件名
     * @param inputSource 文件来源
     * @return 文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 校验图片
     * @param inputSource  输入源
     */
    protected abstract void validPicture(Object inputSource);


}
