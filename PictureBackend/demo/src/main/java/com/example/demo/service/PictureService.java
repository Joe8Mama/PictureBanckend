package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.model.dto.picture.PictureQueryRequest;
import com.example.demo.model.dto.picture.PictureUploadRequest;
import com.example.demo.model.dto.user.UserQueryRequest;
import com.example.demo.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.model.entity.User;
import com.example.demo.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 16130
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-06-15 23:04:39
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile        文件
     * @param pictureUploadRequest 上传结果
     * @param loginUser            登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
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
}


