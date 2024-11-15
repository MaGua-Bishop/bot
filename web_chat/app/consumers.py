import json

from asgiref.sync import sync_to_async
from channels.generic.websocket import AsyncWebsocketConsumer
from app import models
from django.core.cache import cache

def get_user(uid):
    return models.User.objects.filter(uid=uid).first()

class ChatConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        self.room_group_name = 'game'  # 固定为单一房间
        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name
        )
        await self.accept()

    async def disconnect(self, close_code):
        await self.channel_layer.group_discard(
            self.room_group_name,
            self.channel_name
        )

    async def receive(self, text_data):
        text_data_json = json.loads(text_data)
        user = text_data_json['user']
        key = f"cache/user/{user}"
        cached_qs = cache.get(key)
        if cached_qs is None:
            print(user)
            cached_qs = await sync_to_async(get_user)(uid=user)
            if cached_qs:
                cache.set(key, cached_qs, timeout=60)  # 缓存 15 分钟
            else:
                print("关闭连接")
                await self.close()
                return

        message = text_data_json.get('message')
        file_url = text_data_json.get('file_url')
        file_type = text_data_json.get('file_type')

        # 将消息发送到房间
        await self.channel_layer.group_send(
            self.room_group_name,
            {
                'type': 'chat_message',
                'user': user,
                'username': cached_qs.user,
                'message': message,
                'file_url': file_url,
                'file_type': file_type
            }
        )

    async def chat_message(self, event):
        message = event.get('message')
        user = event['user']
        username = event['username']
        file_url = event.get('file_url')
        file_type = event.get('file_type')
        key = f"cache/user/{user}"
        cached_qs = cache.get(key)

        # 将消息发送到 WebSocket，包括用户名和文件信息
        await self.send(text_data=json.dumps({
            'user': user,
            'username': cached_qs.user,
            'message': message,
            'file_url': file_url,
            'file_type': file_type
        }))
