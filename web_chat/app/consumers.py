import json

from channels.generic.websocket import AsyncWebsocketConsumer

from app import models
from app.bot import ChatBot


def get_user(uid):
    return models.User.objects.filter(uid=uid).first()


class ChatConsumer(AsyncWebsocketConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.bot = ChatBot()
        self.room_group_name = None

    async def connect(self):
        await self.accept()

    async def disconnect(self, close_code):
        if self.room_group_name:
            await self.channel_layer.group_discard(
                self.room_group_name,
                self.channel_name
            )

    async def receive(self, text_data):
        text_data_json = json.loads(text_data)
        print(f"收到消息: {text_data_json}")

        # 从消息中获取admin_username作为房间ID
        room_id = text_data_json.get('admin_username')
        if room_id:
            self.room_group_name = f'chat_{room_id}'

            # 确保channel加入到正确的群组
            await self.channel_layer.group_add(
                self.room_group_name,
                self.channel_name
            )

            # 先广播用户消息到房间组
            await self.channel_layer.group_send(
                self.room_group_name,
                {
                    'type': 'chat_message',
                    'message': text_data_json
                }
            )

            # 等待用户消息处理完成后，再让机器人处理消息
            if 'message' in text_data_json:
                user_id = text_data_json.get('user_id', 'anonymous')
                # 使用 asyncio.create_task 创建一个新的任务
                await self.bot.handle_message(
                    room_id=room_id,
                    user_id=user_id,
                    message=text_data_json['message'],
                    consumer=self
                )

    async def chat_message(self, event):
        # 发送消息到WebSocket
        await self.send(text_data=json.dumps(event['message']))

    async def broadcast_message(self, message):
        bot = ChatBot()
        await bot.broadcast_message(message, self)
