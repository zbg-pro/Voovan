package org.voovan.http.websocket;

import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebSocketSession {
    private IoSession socketSession;
    private WebSocketRouter webSocketRouter;
    private String remoteAddres;
    private int remotePort;

    private Map<String,Object> attributes;

    /**
     * 构造函数
     * @param socketSession Socket 会话
     * @param webSocketRouter WebSocket 路由处理对象
     */
    public WebSocketSession(IoSession socketSession, WebSocketRouter webSocketRouter, String remoteAddres, int remotePort){
        this.socketSession = socketSession;
        this.remoteAddres = remoteAddres;
        this.remotePort = remotePort;
        this.webSocketRouter = webSocketRouter;
        attributes = new ConcurrentHashMap<String, Object>();
    }

    /**
     * 获取对端连接的 IP
     *
     * @return 对端连接的 IP
     */
    public String getRemoteAddres() {
       return this.remoteAddres;
    }

    /**
     * 获取对端连接的端口
     *
     * @return 对端连接的端口
     */
    public int getRemotePort() {
        return remotePort;
    }

    /**
     * 获取当前 Session 属性
     * @param name 属性名
     * @return 属性值
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * 判断当前 Session 属性是否存在
     * @param name 属性名
     * @return true: 存在, false: 不存在
     */
    public boolean containAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * 设置当前 Session 属性
     * @param name	属性名
     * @param value	属性值
     */
    public void setAttribute(String name,Object value) {
        attributes.put(name, value);
    }

    /**
     *  删除当前 Session 属性
     * @param name	属性名
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * 获取 WebSocket 路由处理对象
     * @return WebSocket 路由处理对象
     */
    public WebSocketRouter getWebSocketRouter() {
        return webSocketRouter;
    }

    /**
     * 设置获取WebSocket 路由处理对象
     * @param webSocketRouter WebSocket 路由处理对象
     */
    public void setWebSocketRouter(WebSocketRouter webSocketRouter) {
        this.webSocketRouter = webSocketRouter;
    }

    /**
     * 发送 websocket 消息
     * @param obj 消息对象
     */
    public synchronized void send(Object obj) {

        ByteBuffer byteBuffer = (ByteBuffer)webSocketRouter.filterEncoder(this, obj);
        WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, false, byteBuffer);
        try {
            this.socketSession.syncSend(webSocketFrame);
        } catch (SendMessageException e) {
            Logger.error("WebSocket send frame error", e);
        }
    }

    /**
     * 发送 websocket 帧
     * @param webSocketFrame 帧
     */
    protected synchronized void send(WebSocketFrame webSocketFrame) throws SendMessageException {
        this.socketSession.syncSend(webSocketFrame);

        if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.TEXT ||
                webSocketFrame.getOpcode() == WebSocketFrame.Opcode.BINARY) {
            //触发发送事件
            webSocketRouter.onSent(this, webSocketFrame.getFrameData());
        }
    }

    /**
     * 判断连接状态
     * @return true: 连接状态, false: 断开状态
     */
    public boolean isConnected(){
        return socketSession.isConnected();
    }

    /**
     * 直接关闭 Socket 连接
     *      不会发送 CLOSING 给客户端
     */
    /**
     * 关闭 WebSocket
     */
    public void close() {
        WebSocketFrame closeWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING,
                false, ByteBuffer.wrap(WebSocketTools.intToByteArray(1000, 2)));
        try {
            send(closeWebSocketFrame);
        } catch (SendMessageException e) {
            Logger.error("Close WebSocket error, Socket will be close " ,e);
            socketSession.close();
        }
    }

    protected IoSession getSocketSession() {
        return socketSession;
    }

     public void setSocketSession(IoSession socketSession) {
        this.socketSession = socketSession;
    }

}