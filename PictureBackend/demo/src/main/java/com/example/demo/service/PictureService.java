package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.example.demo.model.dto.picture.*;
import com.example.demo.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.model.entity.User;
import com.example.demo.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-06-15 23:04:39
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource           输入源
     * @param pictureUploadRequest 上传结果
     * @param loginUser            登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);



    /**
     * 查询图片
     *
     * @param pictureQueryRequest 查询条件
     * @return 图片列表
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片视图
     *
     * @param picture 图片
     * @param request 请求
     * @return 图片视图
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片视图列表
     *
     * @param picturePage 图片分页
     * @param request     请求
     * @return 图片视图列表
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片
     *
     * @param picture 图片
     */
    void validPicture(Picture picture);

    /**
     * 审核图片
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     *
     * @param picture   图片
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 图片上传（批量）请求
     * @param loginUser                   登录用户
     * @return 成功的图片数量
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理图片文件
     *
     * @param picture  图片
     */
    void clearPictureFile(Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId 图片id
     * @param loginUser 登录用户
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片
     *
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser 登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 检查图片权限
     *
     * @param loginUser 登录用户
     * @param picture   图片
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 根据空间id和颜色查询图片
     *
     * @param spaceId   空间id
     * @param picColor  颜色
     * @param loginUser 登录用户
     * @return 图片列表
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 编辑图片（批量）
     *
     * @param pictureEditByBatchRequest 图片编辑（批量）请求
     * @param loginUser                 登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建AI扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建图片外绘任务请求
     * @param loginUser                           登录用户
     * @return 创建图片外绘任务响应
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}


