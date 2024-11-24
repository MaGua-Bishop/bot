import logging
import time
import requests
import asyncio
import os
import django
from channels.db import database_sync_to_async
from datetime import datetime, timedelta
from collections import defaultdict
from decimal import Decimal
from django.db import transaction
from django.db.models import F
from django.conf import settings

# 设置 Django 环境
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'bot_data.settings')
django.setup()

from app.bot import ChatBot
from app.models import LotteryRecord, BetRecord, User, ChangeMoney, Admin

logger = logging.getLogger(__name__)


class LotteryMonitor:
    def __init__(self):
        self.last_draw_issue = None
        self.current_betting_issue = None
        self.bot = ChatBot()
        self.stop_betting_sent = False
        self.first_draw_time = "00:03:40"
        self.last_draw_time = "23:58:40"

    @database_sync_to_async
    def initialize_current_issue(self):
        """初始化当前期号"""
        try:
            # 获取最新一期记录
            latest_record = LotteryRecord.objects.order_by('-issue').first()
            if latest_record:
                if latest_record.status == 0:
                    # 如果最新记录未开奖，设置为当可下注期号
                    self.current_betting_issue = latest_record.issue
                else:
                    # 如果最新记录已开奖，设置为下一期
                    self.current_betting_issue = str(int(latest_record.issue) + 1)
                self.last_draw_issue = latest_record.issue
                logger.info(f"初始化期号成功: 当前可下注期号 {self.current_betting_issue}")
            return True
        except Exception as e:
            logger.error(f"初始化期号出错: {str(e)}")
            return False

    def is_valid_draw_time(self) -> bool:
        """检查当前是否在有效的开奖时间范围内"""
        current_time = datetime.now()
        current_str = current_time.strftime("%H:%M:%S")
        return self.first_draw_time <= current_str <= self.last_draw_time

    def is_near_draw_time(self) -> bool:
        """检查是否接近开奖时间（提前30秒）"""
        current_time = datetime.now()
        current_minute = current_time.minute
        current_second = current_time.second

        # 判断是否在每5分钟的第3分40秒
        is_draw_minute = current_minute % 5 == 3
        # 开奖前30秒发送停止下注通知
        is_near_draw = current_second >= 30 and current_second <= 40

        return is_draw_minute and is_near_draw

    def get_next_draw_time(self) -> str:
        current_time = datetime.now()
        current_minute = current_time.minute

        # 计算下一个开奖时间点
        next_draw_minute = ((current_minute // 5) * 5) + 3
        if current_minute % 5 >= 3:
            next_draw_minute += 5

        # 处理跨小时的情况
        if next_draw_minute >= 60:
            next_time = current_time.replace(hour=current_time.hour + 1, minute=next_draw_minute - 60, second=40)
        else:
            next_time = current_time.replace(minute=next_draw_minute, second=40)

        # 如果小时数超过23，需要调整到第二天
        if next_time.hour > 23:
            next_time = next_time.replace(day=next_time.day + 1, hour=0)

        return next_time.strftime("%H:%M:%S")

    def get_lottery_data(self) -> list:
        """获取开奖数据"""
        try:
            url = "https://api.api168168.com/klsf/getHistoryLotteryInfo.do?date=&lotCode=10011"
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
            response = requests.get(url, headers=headers, timeout=10)
            data = response.json()
            return data.get('result', {}).get('data', [])
        except Exception as e:
            logger.error(f"获取开奖数据时出错: {str(e)}")
            return []

    async def check_lottery_updates(self):
        """检查开奖更新"""
        while True:
            try:
                # 检查是否在有效时间范围内
                if not self.is_valid_draw_time():
                    logger.info("当前不在开奖时间范围内")
                    await asyncio.sleep(30)
                    continue

                # 输出当前下注期号
                logger.info(f"执行了")

                data = self.get_lottery_data()
                if not data:
                    logger.warning("未获取到开奖数据")
                    await asyncio.sleep(1)
                    continue

                current_draw_issue = data[0].get('preDrawIssue')
                if not current_draw_issue:
                    logger.warning("获取到的开奖期号为空")
                    await asyncio.sleep(1)
                    continue

                next_draw_issue = str(int(current_draw_issue) + 1)

                # 检查是否有新开奖数据需要处理
                if current_draw_issue != self.last_draw_issue:
                    logger.info(f"检测到新开奖数据: {current_draw_issue}")
                    try:
                        lottery_data = data[0]
                        # 获取或创建开奖记录
                        record, created = await self.get_or_create_record(
                            issue=current_draw_issue,
                            code=lottery_data.get('preDrawCode', ''),
                            time=lottery_data.get('preDrawTime', ''),
                            sum_num=lottery_data.get('sumNum', 0),
                            sum_single_double=lottery_data.get('sumSingleDouble', ''),
                            sum_big_small=lottery_data.get('sumBigSmall', ''),
                            last_big_small=lottery_data.get('lastBigSmall', ''),
                            first_dragon_tiger=lottery_data.get('firstDragonTiger', ''),
                            second_dragon_tiger=lottery_data.get('secondDragonTiger', ''),
                            third_dragon_tiger=lottery_data.get('thirdDragonTiger', ''),
                            fourth_dragon_tiger=lottery_data.get('fourthDragonTiger', '')
                        )

                        if not record:
                            logger.error(f"创建期号 {current_draw_issue} 记录失败")
                            continue

                        # 如果有开奖号码，发送开奖消息
                        if record.code:
                            logger.info(f"准备发送开奖通知: {current_draw_issue}")
                            # 构造开奖通知消息
                            draw_message = (
                                f"🎉 开奖通知\n"
                                f"期号: {record.issue}\n"
                                f"开奖号码: {record.code}\n"
                                f"开奖时间: {record.time}"
                                f"\n\n"
                                f"总和: {record.sum_num}\n"
                                f"总和单双: {record.get_sum_single_double_display()}\n"
                                f"总和大小: {record.get_sum_big_small_display()}\n"
                                f"尾大小: {record.get_last_big_small_display()}\n"
                                f"龙虎: {record.get_first_dragon_tiger_display()} "
                                f"{record.get_second_dragon_tiger_display()} "
                                f"{record.get_third_dragon_tiger_display()} "
                                f"{record.get_fourth_dragon_tiger_display()}"
                            )
                            
                            # 使用HTTP请求调用views中的send_broadcast
                            response = requests.post(
                                'http://localhost:8000/app/send_broadcast/',  # 修改为实际的URL
                                json={'message': draw_message}
                            )
                            if response.status_code == 200:
                                logger.info(f"开奖通知已发送: {current_draw_issue}")
                            else:
                                logger.error(f"发送开奖通知失败: {response.text}")

                            # 处理下注记录并发送中奖通知
                            messages_to_send = await self.process_bet_records(record)
                            logger.info(f"获取到需要发送的中奖消息: {len(messages_to_send)} 条")

                            for room_id, message in messages_to_send:
                                try:
                                    response = requests.post(
                                        'http://localhost:8000/app/send_broadcast/',  # 修改为实际的URL
                                        json={'message': message, 'room_id': room_id}
                                    )
                                    if response.status_code == 200:
                                        logger.info(f"中奖通知已发送到聊天室 {room_id}")
                                    else:
                                        logger.error(f"发送中奖通知到聊天室 {room_id} 失败: {response.text}")
                                except Exception as e:
                                    logger.error(f"发送消息到聊天室 {room_id} 时出错: {str(e)}")

                            await self.update_record_status(record, 2)

                            # 更新最新开奖期号
                            self.last_draw_issue = current_draw_issue

                            # 开奖通知发送后，再处理下一期
                            await self.handle_next_issue(next_draw_issue)
                            self.stop_betting_sent = False

                    except Exception as e:
                        logger.error(f"处理开奖数据时出错: {str(e)}")
                        continue

                # 检查是否接近开奖时间，处理停止下注
                if self.is_near_draw_time() and not self.stop_betting_sent:
                    if self.current_betting_issue:
                        # 发送停止下注通知
                        stop_message = (
                            f"⚠️ 停止下注通知\n"
                            f"期号: {self.current_betting_issue}\n"
                            f"本期已停止下注，请等待开奖"
                        )
                        response = requests.post(
                            'http://localhost:8000/app/send_broadcast/',  # 修改为实际的URL
                            json={'message': stop_message}
                        )
                        if response.status_code == 200:
                            logger.info("停止下注通知已发送")
                        else:
                            logger.error(f"发送停止下注通知失败: {response.text}")

                        # 更新当前期号状态为停止下注(1)
                        current_record = await self.get_record_by_issue(self.current_betting_issue)
                        if current_record:
                            await self.update_record_status(current_record, 1)
                        self.stop_betting_sent = True

            except Exception as e:
                logger.error(f"检查开奖更新时出错: {str(e)}")

            await asyncio.sleep(1)

    async def handle_next_issue(self, next_draw_issue):
        """处理下一期"""
        try:
            # 预创建下一期记录(状态为0，可下注)
            next_record, next_created = await self.get_or_create_record(
                issue=next_draw_issue,
                code='',
                time='',
                sum_num=0,
                sum_single_double='',
                last_big_small='',
                sum_big_small='',
                first_dragon_tiger='',
                second_dragon_tiger='',
                third_dragon_tiger='',
                fourth_dragon_tiger=''
            )

            if next_record:
                # 确保新记录状态为可下注(0)
                await self.update_record_status(next_record, 0)

                # 获取下一期开奖时间
                next_draw_time = self.get_next_draw_time()

                # 构造下一期通知消息
                next_message = (
                    f"🔥 下一期开始\n"
                    f"期号: {next_draw_issue}\n"
                    # f"开奖时间: {next_draw_time}\n"
                    f"请各位玩家下注"
                )

                # 使用HTTP请求调用views中的send_broadcast
                response = requests.post(
                    'http://localhost:8000/app/send_broadcast/',  # 修改为实际的URL
                    json={'message': next_message}
                )
                if response.status_code == 200:
                    logger.info(f"下一期通知已发送: {next_draw_issue}")
                else:
                    logger.error(f"发送下一期通知失败: {response.text}")

                # 更新当前可下注期号
                self.current_betting_issue = next_draw_issue
                logger.info(f"创建下一期记录: {next_draw_issue}, created: {next_created}")
        except Exception as e:
            logger.error(f"处理下一期时出错: {str(e)}")

    @database_sync_to_async
    def get_or_create_record(self, issue, code, time, sum_num, sum_single_double, last_big_small,
                             sum_big_small, first_dragon_tiger, second_dragon_tiger,
                             third_dragon_tiger, fourth_dragon_tiger):
        """获取或创建开奖记录"""
        # 准备记录数据
        record_data = {
            'code': code,
            'time': time,
            'sum_num': int(sum_num) if sum_num != '' else 0,
            'sum_single_double': int(sum_single_double) if sum_single_double != '' else 0,  # 0-单,1-双
            'sum_big_small': int(sum_big_small) if sum_big_small != '' else 0,  # 0-大,1-小
            'last_big_small': int(last_big_small) if last_big_small != '' else 0,  # 0-尾大,1-尾小
            'first_dragon_tiger': int(first_dragon_tiger) if first_dragon_tiger != '' else 0,  # 0-龙,1-虎
            'second_dragon_tiger': int(second_dragon_tiger) if second_dragon_tiger != '' else 0,
            'third_dragon_tiger': int(third_dragon_tiger) if third_dragon_tiger != '' else 0,
            'fourth_dragon_tiger': int(fourth_dragon_tiger) if fourth_dragon_tiger != '' else 0
        }

        try:
            # 尝试获取现有记录
            record = LotteryRecord.objects.get(issue=issue)

            # 如果有新的开奖数据，更新所有字段
            if code:  # 只要有开奖号码就更新
                logger.info(f"更新期号 {issue} 的开奖数据: {record_data}")
                for key, value in record_data.items():
                    setattr(record, key, value)
                record.save()
                logger.info(f"期号 {issue} 的开奖数据更新完成")
            created = False

        except LotteryRecord.DoesNotExist:
            # 创建新记录
            record_data['issue'] = issue
            record_data['status'] = 0
            record = LotteryRecord.objects.create(**record_data)
            created = True
            logger.info(f"创建新期号 {issue} 记录")

        return record, created

    @database_sync_to_async
    def update_record_status(self, record, status):
        """更新记录状态
        status: 0-可下注, 1-停止下注, 2-已开奖
        """
        if status == 2:
            print(record)
        record.status = status
        record.save()
        status_map = {0: "可下注", 1: "停止下注", 2: "已开奖"}
        logger.info(f"期号 {record.issue} 状态更新为{status_map.get(status)}")

    @database_sync_to_async
    def get_record_by_issue(self, issue):
        """根据期号获取记录"""
        try:
            return LotteryRecord.objects.get(issue=issue)
        except LotteryRecord.DoesNotExist:
            return None

    @database_sync_to_async
    def process_bet_records(self, lottery_record):
        """处理该期所有下注记录"""
        from decimal import Decimal
        from django.db import transaction

        try:
            messages_to_send = []

            # 使用事务确保数据一致性
            with transaction.atomic():
                # 获取该期所有未结算的下注记录
                bet_records = BetRecord.objects.filter(
                    issue=lottery_record.issue,
                    status=0
                ).select_for_update()

                # 添加日志来检查查询条件
                logger.info(f"查询条件 - 期号: {lottery_record.issue}")
                logger.info(f"查询条件 - 状态: 0")
                logger.info(f"找到 {bet_records.count()} 条待处理的下注记录")

                # 如果没有找到记录检查是否有任何下注记录（不考虑状态）
                all_records = BetRecord.objects.filter(issue=lottery_record.issue)
                logger.info(f"该期总共有 {all_records.count()} 条下注记录")
                logger.info(
                    f"各状态下注记录数量: {[(status, BetRecord.objects.filter(issue=lottery_record.issue, status=status).count()) for status in [0, 1, 2]]}")

                # 用于存储每个聊天室的中奖消息
                room_messages = defaultdict(list)

                for bet in bet_records:
                    win = False
                    # 根据不同的下注类型判断是否中奖
                    if bet.bet_type == '总和单' and lottery_record.sum_single_double == 0:
                        win = True
                    elif bet.bet_type == '总和双' and lottery_record.sum_single_double == 1:
                        win = True
                    elif bet.bet_type == '总和大' and lottery_record.sum_big_small == 0:
                        win = True
                    elif bet.bet_type == '总和小' and lottery_record.sum_big_small == 1:
                        win = True
                    elif bet.bet_type == '尾大' and lottery_record.last_big_small == 0:
                        win = True
                    elif bet.bet_type == '尾小' and lottery_record.last_big_small == 1:
                        win = True
                    # elif bet.bet_type == '1龙' and lottery_record.first_dragon_tiger == 0:
                    #     win = True
                    # elif bet.bet_type == '1虎' and lottery_record.first_dragon_tiger == 1:
                    #     win = True

                    logger.info(f"处理下注记录 ID: {bet.id}, 用户: {bet.user_id}, 类型: {bet.bet_type}, 中奖: {win}")

                    # 更新下注记录状态
                    bet.status = 1  # 已结算
                    bet.win = win
                    if win:
                        # 获取对应管理员的赔率设置并计算中奖金额
                        admin = Admin.objects.get(username=bet.admin_username)
                        bet.win_amount = bet.amount * admin.odds  # 直接使用odds值
                        logger.info(f"中奖计算 - 管理员: {bet.admin_username}, 赔率: {admin.odds}, 下注金额: {bet.amount}, 中奖金额: {bet.win_amount}")

                        try:
                            user = User.objects.select_for_update().get(id=bet.user_id)
                            User.objects.filter(id=bet.user_id).update(money=F('money') + bet.win_amount)
                            ChangeMoney.objects.create(
                                user_id=bet.user_id,
                                last_money=user.money,  # 使用更新前的余额
                                money=bet.win_amount,  # 中奖金额
                                now_money=user.money + bet.win_amount,  # 更新后的余额
                                change_type='中奖增加'
                            )
                            logger.info(f"用户 {bet.user_id} 余额已更新 - 原余额: {user.money}, 中奖金额: {bet.win_amount}, 新余额: {user.money + bet.win_amount}")

                            # 将中奖消息添加到对应聊天室的列表中
                            win_message = (
                                f"🎊 用户{bet.user_name} 中奖\n"
                                # f"玩法: {bet.bet_type}\n"
                                # f"下注金额: {bet.amount:.2f}\n"
                                f"中奖金额: {bet.win_amount:.2f}"
                            )
                            room_messages[bet.admin_username].append(win_message)
                            logger.info(f"添加中奖消息到聊天室 {bet.admin_username}")

                        except User.DoesNotExist:
                            logger.error(f"用户不存在: {bet.user_id}")
                    else:
                        bet.win_amount = -bet.amount

                    bet.save()
                    logger.info(f"下注记录 {bet.id} 已更新")

                # 准备发送的消息
                for room_id, messages in room_messages.items():
                    if messages:
                        combined_message = (
                            f"🎊 中奖通知\n"
                            f"期号: {lottery_record.issue}\n"
                            f"------------------------\n"
                            f"{chr(10).join(messages)}"
                        )
                        messages_to_send.append((room_id, combined_message))
                        logger.info(f"准备发送消息到聊天室 {room_id}: {combined_message}")

            logger.info(f"所有下注记录处理完成，准备发送 {len(messages_to_send)} 条中奖通知")
            return messages_to_send

        except Exception as e:
            logger.error(f"处理下注记录时出错: {str(e)}")
            return []


async def start_monitoring():
    """启动监控"""
    monitor = LotteryMonitor()
    # 初始化当前期号
    initialized = await monitor.initialize_current_issue()
    if initialized:
        await monitor.check_lottery_updates()
    else:
        logger.error("初始化期号失败，程序退出")


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(levelname)s - %(message)s'
    )
    asyncio.run(start_monitoring())
