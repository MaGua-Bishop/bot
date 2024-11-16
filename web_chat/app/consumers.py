import json
from urllib.parse import parse_qs

from asgiref.sync import sync_to_async
from channels.generic.websocket import AsyncWebsocketConsumer
from app import models
from django.core.cache import cache


def get_user(uid):
    return models.User.objects.filter(uid=uid).first()


class ChatConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        query_string = self.scope['query_string'].decode()
        query_params = parse_qs(query_string)
        self.admin_username = query_params.get('admin', [None])[0]
        print(f"WebSocket连接 - admin_username: {self.admin_username}")

        if self.admin_username:
            self.room_group_name = f"chat_room_{self.admin_username}"
            await self.channel_layer.group_add(
                self.room_group_name,
                self.channel_name
            )
            await self.accept()
        else:
            await self.close()

    async def receive(self, text_data):
        text_data_json = json.loads(text_data)
        print(f"收到消息: {text_data_json}")

        # 广播消息到房间组
        await self.channel_layer.group_send(
            self.room_group_name,
            {
                'type': 'chat_message',
                'message': text_data_json
            }
        )

    async def chat_message(self, event):
        message = event['message']
        print(f"发送消息到客户端: {message}")
        
        # 发送消息到WebSocket
        await self.send(text_data=json.dumps(message))

    async def disconnect(self, close_code):
        if hasattr(self, 'room_group_name'):
            await self.channel_layer.group_discard(
                self.room_group_name,
                self.channel_name
            )
