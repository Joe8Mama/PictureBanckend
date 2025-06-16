package com.example.demo.mapper;

import com.example.demo.model.dto.picture.PictureUploadRequest;
import com.example.demo.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.model.entity.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 16130
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-06-15 23:04:39
 * @Entity com.example.demo.model.entity.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {

}




