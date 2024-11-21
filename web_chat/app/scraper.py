import logging
import time
import requests
import asyncio
import os
import django
from channels.db import database_sync_to_async
from datetime import datetime, timedelta

# 设置 Django 环境
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'bot_data.settings')
django.setup()

from app.bot import ChatBot
from app.models import LotteryRecord

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
                    # 如果最新记录未开奖，设置为当前可下注期号
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
                            await self.bot.broadcast_message(draw_message)
                            logger.info(f"开奖通知已发送: {current_draw_issue}")

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
                        await self.bot.broadcast_message(stop_message)

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
                    f"开奖时间: {next_draw_time}\n"
                    f"请各位玩家下注"
                )
                await self.bot.broadcast_message(next_message)

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
            'sum_big_small': int(sum_big_small) if sum_big_small != '' else 0,             # 0-大,1-小
            'last_big_small': int(last_big_small) if last_big_small != '' else 0,          # 0-尾大,1-尾小
            'first_dragon_tiger': int(first_dragon_tiger) if first_dragon_tiger != '' else 0,   # 0-龙,1-虎
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
