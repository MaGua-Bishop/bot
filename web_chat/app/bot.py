import asyncio
import logging
import datetime
from channels.db import database_sync_to_async
from app.models import Message, Admin
from channels.layers import get_channel_layer


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class ChatBot:
    _instance = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if not hasattr(self, 'initialized'):
            self.chat_rooms = {}
            self.keywords = {
                "您好": "您也好",
            }
            self.initialized = True

    @database_sync_to_async
    def save_message(self, admin_username, message):
        """
        保存消息到数据库
        """
        try:
            message_obj = Message(
                admin_username=admin_username,
                message=message,
                is_bot=True
            )
            message_obj.save()
            logger.info(f"成功保存机器人消息: admin={admin_username}, message={message}")
        except Exception as e:
            logger.error(f"保存消息时出错: {str(e)}")
            import traceback
            logger.error(traceback.format_exc())

    async def handle_message(self, room_id: str, user_id: str, message: str, consumer=None):
        """
        处理接收到的消息
        """
        for keyword, response in self.keywords.items():
            if keyword in message:
                logger.info(f"聊天室 {room_id} 中的用户 {user_id} 说: {message}")
                if consumer:
                    try:
                        await self.send_response(room_id, response, consumer)
                    except Exception as e:
                        logger.error(f"发送响应时出错: {str(e)}")
                break

    async def send_response(self, room_id: str, message: str, consumer):
        """
        发送机器人回复
        """
        if consumer:
            try:
                # 确保时间戳比用户消息晚
                current_time = datetime.datetime.now()

                # 构造回复消息数据
                response_data = {
                    'type': 'chat_message',
                    'message': {
                        'admin_username': room_id,
                        'message': message,
                        'user_id': 'Anonymous ',
                        'user_name': 'Anonymous',  # 添加用户名
                        'timestamp': current_time.strftime("%H:%M")
                    }
                }

                # 先发送消息
                await consumer.channel_layer.group_send(
                    f"chat_{room_id}",
                    response_data
                )

                # 保存到数据库
                await self.save_message(room_id, message)
                logger.info(f"机器人在聊天室 {room_id} 回复: {message}")
            except Exception as e:
                logger.error(f"在send_response中出错: {str(e)}")
                import traceback
                logger.error(traceback.format_exc())

    @database_sync_to_async
    def get_all_admins(self):
        """获取所有管理员"""
        try:
            return list(Admin.objects.values_list('username', flat=True))
        except Exception as e:
            logger.error(f"获取管理员列表时出错: {str(e)}")
            return []

    async def broadcast_message(self, message: str):
        """
        向所有聊天室广播消息
        """
        try:
            admin_usernames = await self.get_all_admins()
            if not admin_usernames:
                logger.warning("没有找到任何管理员聊天室")
                return

            channel_layer = get_channel_layer()
            current_time = datetime.datetime.now()
            broadcast_data = {
                'type': 'chat_message',
                'message': {
                    'message': message,
                    'user_id': 'Anonymous',
                    'user_name': 'Anonymous',
                    'timestamp': current_time.strftime("%H:%M")
                }
            }

            for admin_username in admin_usernames:
                try:
                    ws_group = f'chat_{admin_username}'
                    broadcast_data['message']['admin_username'] = admin_username

                    # 发送消息
                    await channel_layer.group_send(
                        ws_group,
                        broadcast_data.copy()
                    )

                    logger.info(f"消息已发送到聊天室 {admin_username}")
                    await self.save_message(admin_username, message)

                except Exception as e:
                    logger.error(f"向聊天室 {admin_username} 发送消息失败: {str(e)}")

                    continue

            logger.info("广播消息处理完成")

        except Exception as e:
            logger.error(f"广播消息时出错: {str(e)}")


