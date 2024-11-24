import asyncio
import logging
import datetime
from channels.db import database_sync_to_async
from app.models import Message, Admin,User, ChangeMoney
from channels.layers import get_channel_layer
from decimal import Decimal


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
            # 正则表达式
            self.bet_patterns = {
                '总和大': r'大\s*(\d+\.?\d*)',
                '总和小': r'小\s*(\d+\.?\d*)',
                '总和单': r'单\s*(\d+\.?\d*)',
                '总和双': r'双\s*(\d+\.?\d*)',
                '尾大': r'尾大\s*(\d+\.?\d*)',
                '尾小': r'尾小\s*(\d+\.?\d*)',
                # '龙': r'龙\s*(\d+\.?\d*)',
                # '虎': r'虎\s*(\d+\.?\d*)',
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
        """处理接收到的消息"""
        try:
            # 获取当前可下注期号
            current_betting_issue = await self.get_current_betting_issue()
            if not current_betting_issue:
                logger.warning("未找到当前可下注期号")
                return

            # 检查管理员是否开盘
            admin_is_open = await self.check_admin_open_status(room_id)
            if not admin_is_open:
                await self.send_response(room_id, "❌ 当前未开盘，无法下注", consumer)
                return

            # 获取管理员的最大下注金额
            max_bet_amount = await self.get_admin_max_bet_amount(room_id)

            # 检查是否是下注指令
            bet_info = self.parse_bet_command(message)
            if bet_info:
                bet_type, amount = bet_info

                if amount <= 0:
                    logger.warning(f"下注金额必须大于0: {amount}")
                    return

                # 检查下注金额是否超过最大下注金额
                if amount > max_bet_amount:
                    await self.send_response(room_id, f"❌ 下注金额超过最大限制\n最大下注金额: {max_bet_amount:.2f}", consumer)
                    return

                # 检查用户余额
                user_balance = await self.get_user_balance(user_id)
                if user_balance < amount:
                    await self.send_response(room_id, f"❌ 余额不足\n当前余额: {user_balance:.2f}", consumer)
                    return

                # 检查期号状态是否可下注
                if await self.check_issue_status(current_betting_issue):
                    # 扣除用户余额
                    if await self.deduct_user_balance(user_id, amount):
                        # 创建下注记录
                        await self.create_bet_record(
                            user_id=user_id,
                            admin_username=room_id,
                            issue=current_betting_issue,
                            bet_type=bet_type,
                            amount=amount
                        )
                        # 发送下注确认消息
                        new_balance = await self.get_user_balance(user_id)
                        confirm_message = (
                            f"✅ 下注成功\n"
                            f"期号: {current_betting_issue}\n"
                            f"玩法: {bet_type}\n"
                            f"金额: {amount:.2f}\n"
                            f"余额: {new_balance:.2f}"
                        )
                        await self.send_response(room_id, confirm_message, consumer)
                    else:
                        await self.send_response(room_id, "❌ 扣款失败，请重试", consumer)
                else:
                    await self.send_response(room_id, "❌ 当前期号已停止下注", consumer)
            else:
                # 处理其他关键词回复
                for keyword, response in self.keywords.items():
                    if keyword in message:
                        await self.send_response(room_id, response, consumer)
                        break

        except Exception as e:
            logger.error(f"处理消息时出错: {str(e)}")

    def parse_bet_command(self, message: str):
        """解析下注指令"""
        import re
        for bet_type, pattern in self.bet_patterns.items():
            match = re.match(pattern, message.strip())
            if match:
                # 将金额转换为 Decimal
                amount = Decimal(match.group(1))
                return bet_type, amount
        return None

    @database_sync_to_async
    def get_current_betting_issue(self):
        """获取当前可下注期号"""
        from app.models import LotteryRecord
        from django.utils import timezone
        from datetime import timedelta

        try:
            # 获取当前时间
            now = timezone.now()
            # 计算5分钟前的时间
            five_minutes_ago = now - timedelta(minutes=5)

            # 查找最新的可下注期号（状态为0且创建时间在5分钟内）
            record = (LotteryRecord.objects
                     .filter(status=0)
                     .filter(created_at__gte=five_minutes_ago)  # 创建时间不早于5分钟前
                     .order_by('-issue')
                     .first())

            if record:
                logger.info(f"获取到当前可下注期号: {record.issue}, 创建时间: {record.created_at}")
                return record.issue
            else:
                logger.warning("未找到符合条件的可下注期号")
                return None

        except Exception as e:
            logger.error(f"获取当前可下注期号时出错: {str(e)}")
            return None

    @database_sync_to_async
    def check_issue_status(self, issue):
        """检查期号是否可下注"""
        from app.models import LotteryRecord
        try:
            record = LotteryRecord.objects.get(issue=issue)
            return record.status == 0
        except Exception as e:
            logger.error(f"检查期号状态时出错: {str(e)}")
            return False

    @database_sync_to_async
    def create_bet_record(self, user_id, admin_username, issue, bet_type, amount):
        """创建下注记录"""
        from app.models import BetRecord, User
        try:
            # 获取用户名
            user = User.objects.get(id=user_id)
            user_name = user.user  # 获取用户名

            bet_record = BetRecord(
                user_id=user_id,
                user_name=user_name,  # 添加用户名
                admin_username=admin_username,
                issue=issue,
                bet_type=bet_type,
                amount=Decimal(str(amount)),  # 确保金额是 Decimal 类型
                status=0
            )
            bet_record.save()
            logger.info(f"创建下注记录成功: {user_name}({user_id}) - {admin_username} - {issue} - {bet_type} - {amount}")
            return True
        except User.DoesNotExist:
            logger.error(f"用户 {user_id} 不存在")
            return False
        except Exception as e:
            logger.error(f"创建下注记录时出错: {str(e)}")
            return False

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

    async def broadcast_message(self, message: str, room_id: str = None):
        """
        向聊天室广播消息
        :param message: 消息内容
        :param room_id: 可选的聊天室ID，如果指定则只发送到特定聊天室
        """
        try:
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

            if room_id:
                # 发送到指定聊天室
                ws_group = f'chat_{room_id}'
                broadcast_data['message']['admin_username'] = room_id

                # 发送消息
                await channel_layer.group_send(
                    ws_group,
                    broadcast_data
                )

                logger.info(f"消息已发送到聊天室 {room_id}")
                await self.save_message(room_id, message)
            else:
                # 原有的广播到所有聊天室的逻辑
                admin_usernames = await self.get_all_admins()
                if not admin_usernames:
                    logger.warning("没有找到任何管理员聊天室")
                    return

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

    @database_sync_to_async
    def get_user_balance(self, user_id: str) -> Decimal:
        """获取用户余额"""
        try:
            user = User.objects.get(id=user_id)
            # 确保返回 Decimal 类型
            return Decimal(str(user.money))
        except User.DoesNotExist:
            logger.error(f"用户 {user_id} 不存在")
            return Decimal('0')
        except Exception as e:
            logger.error(f"获取用户余额时出错: {str(e)}")
            return Decimal('0')

    @database_sync_to_async
    def deduct_user_balance(self, user_id: str, amount: Decimal) -> bool:
        """扣除用户余额"""
        from django.db import transaction
        try:
            with transaction.atomic():
                user = User.objects.select_for_update().get(id=user_id)

                # 转换为 Decimal 进行比较，保留两位小数
                current_balance = Decimal(str(user.money)).quantize(Decimal('0.01'))
                amount = amount.quantize(Decimal('0.01'))

                if current_balance < amount:
                    return False

                # 记录旧余额
                old_money = current_balance

                # 计算新余额
                new_money = (current_balance - amount).quantize(Decimal('0.01'))

                # 更新用户余额（跳过信号处理）
                User.objects.filter(id=user_id).update(money=new_money)

                # 手动创建 ChangeMoney 记录
                ChangeMoney.objects.create(
                    user_id=user_id,
                    last_money=old_money,
                    money=-amount,  # 负数表示扣除
                    now_money=new_money,
                    change_type='下注扣除'
                )

                logger.info(f"用户 {user_id} 余额扣除成功: -{amount:.2f}, 当前余额: {new_money:.2f}")
                return True

        except User.DoesNotExist:
            logger.error(f"用户 {user_id} 不存在")
            return False
        except Exception as e:
            logger.error(f"扣除用户余额时出错: {str(e)}")
            return False

    @database_sync_to_async
    def check_admin_open_status(self, admin_username: str) -> bool:
        """检查管理员是否开盘"""
        from app.models import Admin
        try:
            admin = Admin.objects.get(username=admin_username)
            return admin.is_open
        except Admin.DoesNotExist:
            logger.error(f"管理员 {admin_username} 不存在")
            return False
        except Exception as e:
            logger.error(f"检查管理员开盘状态时出错: {str(e)}")
            return False

    @database_sync_to_async
    def get_admin_max_bet_amount(self, admin_username: str) -> Decimal:
        """获取管理员的最大下注金额"""
        from app.models import Admin
        try:
            admin = Admin.objects.get(username=admin_username)
            return admin.max_bet_amount
        except Admin.DoesNotExist:
            logger.error(f"管理员 {admin_username} 不存在")
            return Decimal('0')
        except Exception as e:
            logger.error(f"获取管理员最大下注金额时出错: {str(e)}")
            return Decimal('0')
