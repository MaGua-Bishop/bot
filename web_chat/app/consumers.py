import json

from channels.generic.websocket import AsyncWebsocketConsumer

from app import models
from app.bot import ChatBot

import logging

logger = logging.getLogger(__name__)


def get_user(uid):
    return models.User.objects.filter(uid=uid).first()


class ChatConsumer(AsyncWebsocketConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.bot = ChatBot()
        self.room_group_name = None

    async def connect(self):
        # 从 URL 查询参数中获取 admin
        query_string = self.scope['query_string'].decode()
        params = dict(param.split('=') for param in query_string.split('&') if param)
        admin = params.get('admin')

        if admin:
            # 立即设置房间组名并加入
            self.room_group_name = f'chat_{admin}'
            await self.channel_layer.group_add(
                self.room_group_name,
                self.channel_name
            )
            logger.info(f"WebSocket连接成功: admin={admin}, group={self.room_group_name}")

        await self.accept()

    async def disconnect(self, close_code):
        if self.room_group_name:
            await self.channel_layer.group_discard(
                self.room_group_name,
                self.channel_name
            )
            logger.info(f"WebSocket断开连接: group={self.room_group_name}")

    async def receive(self, text_data):
        text_data_json = json.loads(text_data)
        logger.info(f"收到消息: {text_data_json}")

        # 从消息中获取admin_username
        room_id = text_data_json.get('admin_username')
        if room_id:
            # 如果房间组名与连接时不同，更新它
            new_room_group_name = f'chat_{room_id}'
            if self.room_group_name != new_room_group_name:
                # 如果已在其他组中，先退出
                if self.room_group_name:
                    await self.channel_layer.group_discard(
                        self.room_group_name,
                        self.channel_name
                    )

                self.room_group_name = new_room_group_name
                await self.channel_layer.group_add(
                    self.room_group_name,
                    self.channel_name
                )
                logger.info(f"切换到新的聊天室: {self.room_group_name}")

            # 广播消息
            await self.channel_layer.group_send(
                self.room_group_name,
                {
                    'type': 'chat_message',
                    'message': text_data_json
                }
            )

            # 处理机器人消息
            if 'message' in text_data_json:
                user = text_data_json.get('user', 'anonymous')
                await self.bot.handle_message(
                    room_id=room_id,
                    user_id=user,
                    message=text_data_json['message'],
                    consumer=self
                )

    async def chat_message(self, event):
        await self.send(text_data=json.dumps(event['message']))
