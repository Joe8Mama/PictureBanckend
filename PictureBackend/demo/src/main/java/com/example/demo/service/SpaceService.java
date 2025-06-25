package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.model.dto.space.SpaceAddRequest;
import com.example.demo.model.dto.space.SpaceQueryRequest;
import com.example.demo.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.model.entity.User;
import com.example.demo.model.vo.PictureVO;
import com.example.demo.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-06-20 17:03:42
*/
public interface SpaceService extends IService<Space> {

    /**
     * 添加空间
     *
     * @param spaceAddRequest 添加空间请求
     * @param loginUser 登录用户
     * @return 返回空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 查询空间
     *
     * @param spaceQueryRequest 查询条件
     * @return 空间列表
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间视图
     *
     * @param space 空间
     * @param request 请求
     * @return 空间视图
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间视图列表
     *
     * @param spacePage 空间分页
     * @param request     请求
     * @return 空间视图列表
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 校验空间
     *
     * @param space 空间
     * @param add 是否为添加
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别填充空间对象
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     *
     * @param loginUser 登录用户
     * @param space     空间
     */
    void checkSpaceAuth(User loginUser, Space space);


}
