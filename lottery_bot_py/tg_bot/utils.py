import os
import json
from telebot import types
import numpy as np
from decimal import Decimal, ROUND_DOWN
import random
from datetime import datetime
from tg_bot.models import TgPrizePool, TgLottery  # 替换为实际的数据库模型
from tg_bot.bot_config import bot  # 替换为实际的 bot 实例


def get_work_group_id():
    file_path = "tg_bot/work_group.json"

    # Check if the file exists
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"The file {file_path} does not exist.")

    # Read the JSON file
    with open(file_path, "r", encoding="utf-8") as file:
        try:
            data = json.load(file)
        except json.JSONDecodeError as e:
            raise ValueError(f"Invalid JSON in {file_path}: {e}")
    chat_id = data.get("chat_id")
    if chat_id == "":
        return None

    return chat_id


def set_work_group_id(chat_id):
    if not isinstance(chat_id, str):
        raise ValueError("chat_id must be a string.")
    file_path = "tg_bot/work_group.json"
    data = {"chat_id": chat_id}
    with open(file_path, "w", encoding="utf-8") as file:
        json.dump(data, file, indent=4, ensure_ascii=False)


def get_start_markup() -> types.ReplyKeyboardMarkup:
    buttons = [
        "茶社大群",
        "供需发布",
        "供需频道",
        "TRX兑换",
        "每日抽奖",
        "充值积分",
        "个人中心"
    ]
    markup = types.ReplyKeyboardMarkup(resize_keyboard=True)
    markup.add(*[types.KeyboardButton(button) for button in buttons])
    return markup


def get_return_markup():
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton(text="返回", callback_data="return"))
    return markup


def divide_red_packet(total_amount, count):
    """
    将总金额 total_amount 分为 count 个随机且金额差距适中的红包。

    :param total_amount: Decimal, 总金额
    :param count: int, 红包个数
    :return: List[Decimal], 每个红包的金额列表
    """
    if count <= 0:
        raise ValueError("红包个数必须大于 0")
    if total_amount <= 0:
        raise ValueError("总金额必须大于 0")

    # 将总金额转换为分（避免浮点数精度问题）
    total_cents = int((total_amount * 100).to_integral_value(rounding=ROUND_DOWN))
    if count > total_cents:
        raise ValueError("红包个数不能大于总金额的分单位数")

    # 初始化红包列表
    red_packets = []
    for i in range(count):
        # 确保每个红包的金额在合理范围内，例如 0.5x 到 1.5x 的平均值
        min_cents = max(1, int(0.5 * (total_cents / (count - i))))
        max_cents = min(total_cents - (count - i - 1), int(1.5 * (total_cents / (count - i))))
        amount = random.randint(min_cents, max_cents)
        red_packets.append(amount)
        total_cents -= amount

    # 转换为 Decimal 类型并四舍五入到两位小数
    red_packets = [Decimal(amount) / 100 for amount in red_packets]

    # 调整误差以确保总和等于 total_amount
    difference = total_amount - sum(red_packets)
    if difference != 0:
        red_packets[0] += difference

    return red_packets


def create_lottery_message(amount, uuid, type, url):
    if type == 1:
        n = url.split("/")[-1]
        text = f"抢红包条件: <b>加入群聊</b> <a href=\"{url}\">@{n}</a>"
    elif type == 2:
        n = url.split("/")[-1]
        text = f"抢红包条件: <b>订阅频道</b> <a href=\"{url}\">@{n}</a>"
    else:
        text = ""
    return (
        "🎉 茶社抽奖开始了！ 🎉\n"
        "点击下面的链接参与抽奖并领取现金奖！💰\n"
        f"\n🎁 <b>积分: {amount}</b> 🎁\n\n"
        "不要错过机会，现在就加入，赢取大奖！\n"
        "\n"
        "🔔 关注茶社频道获取独家奖励！\n"
        "敬请关注随机赠品和特殊奖金-不要错过这些令人兴奋的福利! 🎁\n"
        f"<code>{uuid}</code>\n\n{text}"
    )


def get_random_money(lottery_id):
    # 从数据库获取随机奖金
    money_list = TgPrizePool.objects.filter(lottery_id=lottery_id, status=0)

    # 没有金额了更新状态
    if not money_list:
        # 更新状态
        lottery = TgLottery.objects.get(lottery_id=lottery_id)
        lottery.status = 1
        lottery.update_time = datetime.now()
        lottery.save()

        # 发送结束消息
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton(text="已结束", callback_data="null"))
        edit_message_reply_markup = {
            'chat_id': lottery.chat_id,
            'message_id': lottery.message_id,
            'reply_markup': markup
        }
        send_message = {
            'chat_id': lottery.chat_id,
            'text': f"抽奖id:<code>{lottery.lottery_id}</code>\n抽奖已结束\n请发送\n<b><code>/view {lottery.lottery_id}</code></b>\n命令查看中奖者名单",
            'parse_mode': 'HTML'
        }

        try:
            bot.edit_message_reply_markup(**edit_message_reply_markup)  # 假设这个方法存在
            bot.send_message(**send_message)  # 假设这个方法存在
        except Exception as e:
            raise RuntimeError(e)

        return None

    # 随机选择一个奖金
    prize_pool_ids = [prize_pool.prize_pool_id for prize_pool in money_list]
    index = random.randint(0, len(prize_pool_ids) - 1)

    return prize_pool_ids[index]


def query_winning_message(lottery, lottery_info_list):
    str_builder = []  # 使用列表来构建消息字符串
    i = 1
    code = "\U0001F9B7"  # 这个是 Unicode 表情符号（人形）

    if not lottery_info_list:
        str_builder.append("还没有人参与\n")
    else:
        for lottery_info in lottery_info_list:
            # 根据排名选择不同的奖牌表情
            if i == 1:
                code = "🥇"
            elif i == 2:
                code = "🥈"
            elif i == 3:
                code = "🥉"
            else:
                code = "\U0001F9B7"  # 默认为人形表情

            t = f"{code}\t{lottery_info.money}\t<b>{lottery_info.tg_name}</b>\n"
            str_builder.append(t)
            i += 1

    # 拼接整个消息字符串
    message = (
        f"感谢大家对本次活动的热情参与!在激动人心的抽奖之后，我们很高兴地宣布下面的幸运获奖者名单。获奖者，请注意:\n"
        f"\n"
        f"在指定时间内联系<a href=\"tg://user?id={lottery.tg_id}\">@{lottery.tg_name}</a>领取您的奖品。\n"
        f"提供所需的验证(中奖id)，您的奖金将立即记入您的帐户。\n"
        f"如果您有任何问题，请随时联系我们的客户服务团队。\n"
        f"再次感谢您的参与!我们将在未来举办更精彩的活动，我们期待您的继续支持和参与!\n"
        f"\n"
        f"\n"
        f"\U0001F381{lottery.lottery_id}\U0001F381\n"
        f"\n"
        f"\U0001F4CD获奖用户:\n"
        f"描述: 排名|积分|用户名\n"
        f"\n"
        f"{''.join(str_builder)}"  # 将所有字符串拼接起来
    )

    return message
