package com.example.demo.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ThrowUtils;
import com.example.demo.mapper.SpaceMapper;
import com.example.demo.model.dto.space.analyze.*;
import com.example.demo.model.entity.Picture;
import com.example.demo.model.entity.Space;
import com.example.demo.model.entity.User;
import com.example.demo.model.vo.space.analyze.*;
import com.example.demo.service.PictureService;
import com.example.demo.service.SpaceAnalyzeService;
import com.example.demo.service.SpaceService;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureservice;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        // 校验参数
        // 全空间或公共图库，需要从 Picture 表查询
        if (spaceUsageAnalyzeRequest.isQueryPublic() || spaceUsageAnalyzeRequest.isQueryAll()) {
            // 校验权限，管理员可访问
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            // 统计图库的使用空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 补充查询范围
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            List<Object> pictureObjList = pictureservice.getBaseMapper().selectObjs(queryWrapper);
            long totalSize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();
            long useCount = pictureObjList.size();
            // 封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setUsedCount(useCount);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 特定空间，需要从 Space 表查询
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到该空间");
            spaceService.checkSpaceAuth(loginUser, space);
            // 统计空间使用空间
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 获取空间分类分析结果
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 检查权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);

        // 使用 Mybatis Plus 分组查询
        queryWrapper.select("category", "count(*) as count","sum(picSize) as totalSize")
                .groupBy("category");
        // 查询
        return pictureservice.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = (String) result.get("category");
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取空间图片标签情况
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        // 使用 Mybatis Plus 查询
        queryWrapper.select("tags");
        List<Object> tagsJsonList = pictureservice.getBaseMapper().selectObjs(queryWrapper);
        List<String> resultList = tagsJsonList.stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        // 解析标签并统计
        Map<String, Long> tagCountMap = resultList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 转为响应对象，按照使用次数进行排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间图片大小情况
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        // 查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizeList = pictureservice.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(size -> (Long) size)
                .collect(Collectors.toList());

        // 定义分段范围，使用有序的 Map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizeList.stream().filter(size -> size >= 1024 * 1024).count());

        // 转为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间用户上传情况
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);

        // 补充分析维度：每日， 每周，每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度错误");
        }

        // 分组排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询并封装结果
        List<Map<String, Object>> resultList = pictureservice.getBaseMapper().selectMaps(queryWrapper);
        return resultList.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取空间排行分析
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 仅管理员可查看
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无操作权限");
        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());
        return spaceService.list(queryWrapper);
    }


    /**
     * 校验用户权限
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 全空间分析或公共图库权限校验，仅管理员可访问
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无操作权限");
        } else {
            // 分析特定空间，仅本人或管理员可访问
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = this.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到该空间");
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 填充查询条件
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        // 全空间分析
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        // 公共图库分析
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        // 分析特定空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }


}
