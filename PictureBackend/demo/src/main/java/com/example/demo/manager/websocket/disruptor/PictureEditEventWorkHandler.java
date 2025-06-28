package com.example.demo.manager.websocket.disruptor;

import cn.hutool.json.JSONUtil;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ThrowUtils;
import com.example.demo.manager.websocket.PictureEditHandler;
import com.example.demo.manager.websocket.model.PictureEditMessageTypeEnum;
import com.example.demo.manager.websocket.model.PictureEditRequestMessage;
import com.example.demo.manager.websocket.model.PictureEditResponseMessage;
import com.example.demo.model.entity.User;
import com.example.demo.service.UserService;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 图片编辑事件处理器（消费者）
 */

@Slf4j
@Component
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        ;
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();

        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);

        // 根据消息类型处理消息
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                log.info("进入编辑状态");
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                log.info("退出编辑状态");
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                log.info("执行编辑操作");
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                // 其他消息，返回错误提示
                PictureEditResponseMessage errorResponse = new PictureEditResponseMessage();
                errorResponse.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                errorResponse.setMessage("未知的消息类型");
                errorResponse.setUser(userService.getUserVO(user));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(errorResponse)));
                break;
        }
    }
}
