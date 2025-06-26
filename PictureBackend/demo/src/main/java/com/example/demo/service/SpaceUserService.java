package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.model.dto.space.SpaceAddRequest;
import com.example.demo.model.dto.space.SpaceQueryRequest;
import com.example.demo.model.dto.spaceuser.SpaceUserAddRequest;
import com.example.demo.model.dto.spaceuser.SpaceUserQueryRequest;
import com.example.demo.model.entity.Space;
import com.example.demo.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.model.entity.User;
import com.example.demo.model.vo.SpaceUserVO;
import com.example.demo.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 16130
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-06-26 12:45:10
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest 添加空间请求
     * @return 返回空间id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 查询空间
     *
     * @param spaceUserQueryRequest 查询条件
     * @return 空间列表
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间空间成员包装类
     *
     * @param spaceUser 空间用户
     * @param request 请求
     * @return 空间视图
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类（列表）
     *
     * @param spaceUserList 空间分页
     * @return 空间视图列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 校验空间成员
     *
     * @param spaceUser 空间用户
     * @param add 是否为添加
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

}
