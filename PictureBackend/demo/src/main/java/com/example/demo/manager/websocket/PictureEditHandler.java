package com.example.demo.manager.websocket;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.ThrowUtils;
import com.example.demo.manager.websocket.disruptor.PictureEditEventProducer;
import com.example.demo.manager.websocket.model.PictureEditActionEnum;
import com.example.demo.manager.websocket.model.PictureEditMessageTypeEnum;
import com.example.demo.manager.websocket.model.PictureEditRequestMessage;
import com.example.demo.manager.websocket.model.PictureEditResponseMessage;
import com.example.demo.model.entity.User;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑 WebSocket 处理器
 */
@Slf4j
@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 收到前端发送的消息，根据消息类别处理消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 获取消息内容，将JSON转为PictureEditMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        ThrowUtils.throwIf(pictureEditMessageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型不存在");

        // 从 Session 中获取公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");

        // 根据消息类型处理消息（生产消息到 Disruptor 环形队列中）
        pictureEditEventProducer.publish(pictureEditRequestMessage, session, user, pictureId);
    }


    /**
     * 处理编辑操作
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        log.info("用户 " + user.getUserName() + " 进入编辑图片");
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (pictureEditActionEnum == null) {
            log.error("无效的图片编辑动作");
            return;
        }
        // 确认当前用户是否为编辑者
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 构造响应，发送具体操作通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setMessage("用户 " + user.getUserName() + " 执行了 " + pictureEditActionEnum.getText() + " 操作");
            pictureEditResponseMessage.setEditAction(editAction);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给当前用户之外的用户，否则导致重复编辑
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }
    }

    /**
     * 处理退出编辑操作
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        // 确认当前用户是否为编辑者

        if (editingUserId != null && editingUserId.equals(user.getId())) {
            // 移出用户正在编辑该图片
            pictureEditingUsers.remove(pictureId);
            // 构造响应，发送推出编辑的消息通知
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setMessage("用户 " + user.getUserName() + " 退出编辑图片");
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给所有用户
            broadcastToPicture(pictureId, pictureEditResponseMessage);
            log.info("用户 " + user.getUserName() + " 退出编辑图片");
        }
    }

    /**
     * 进入编辑状态
     *
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     **/
    public void handleEnterEditMessage(
            PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws Exception {
        // 没有用户正在编辑该图片 才能进入编辑
        log.info("进入handleEnterEditMessage");
        if (!pictureEditingUsers.containsKey(pictureId)) {
            // 记录编辑状态
            pictureEditingUsers.put(pictureId, user.getId());
            // 通知其它用户
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 加入编辑", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVO(user));
            // 广播给所有用户
            log.info("广播给所有用户");
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }

    }



    /**
     * 连接建立成功
     *
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("连接建立成功，afterConnectionEstablished");
        super.afterConnectionEstablished(session);
        // 保存到会话集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        // 构造响应，发送加入到编辑的消息通知
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage("用户 " + user.getUserName() + " 加入到编辑");
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 关闭连接
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 移出当前用户编辑状态
        handleExitEditMessage(null, session, user, pictureId);
        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
        // 通知其他用户，该用户已经离开编辑
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage("用户 " + user.getUserName() + " 已离开编辑");
        pictureEditResponseMessage.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 广播图片给该图片的所有用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            // 创建 ObjectMapper
            ObjectMapper objectMapper = new ObjectMapper();
            // 配置序列化：将 Long 类型转为 String，解决丢失精度问题
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance); // 支持 long 基本类型
            objectMapper.registerModule(module);
            // 序列化为 JSON 字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                // 排除掉的 session 不发送
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播图片给该图片的所有用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }


}
