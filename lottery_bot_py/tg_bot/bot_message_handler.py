import random
import uuid
from datetime import datetime

from telebot import types
from .bot_config import bot
import re

from .models import TgUser, TgLottery, TgLotteryInfo, TgPrizePool
from .utils import set_work_group_id, get_work_group_id, get_start_markup, get_random_money, query_winning_message

commands = [
    types.BotCommand("start", "启动机器人"),
    types.BotCommand("view", "创建抽奖者查看中奖用户"),
    types.BotCommand("exchange", "创建抽奖者核销中奖者奖励"),
    types.BotCommand("help", "帮助")
]
bot.set_my_commands(commands, scope=types.BotCommandScopeAllPrivateChats())


def get_user(tg_user):
    tg_id = tg_user.id
    tg_fullname = tg_user.first_name + (tg_user.last_name or "")
    tg_username = tg_user.username
    user, _ = TgUser.objects.get_or_create(
        tg_id=tg_id,
        defaults={
            'tg_name': tg_fullname,
            'tg_username': tg_username
        }
    )
    return user


@bot.message_handler(func=lambda message: message.text == "设置工作群" and message.chat.type in ["group", "supergroup"])
def set_work_group_handler(message):
    chat_id = message.chat.id
    user_id = message.from_user.id
    user = TgUser.objects.get(tg_id=user_id)
    if not user.is_admin:
        return
    try:
        set_work_group_id(str(chat_id))
        bot.reply_to(message, f"工作群已设置为当前群聊 (ID: {chat_id})")
    except Exception as e:
        bot.reply_to(message, f"设置工作群时发生错误: {e}")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"\d{5,15}", message.text) and message.chat.type in ["group", "supergroup"]
)
def work_group_query_user(message):
    chat_id = message.chat.id
    admin_id = message.from_user.id
    work_group_id = get_work_group_id()
    if work_group_id != str(chat_id):
        return
    try:
        admin = TgUser.objects.get(tg_id=admin_id)
        if not admin.is_admin:
            return
        user_id = int(message.text)
        user = TgUser.objects.get(tg_id=user_id)
        user_name = bot.get_chat(user_id)
        full_name = f"{user_name.first_name} {user_name.last_name if user_name.last_name else ''}".strip()
        text = (
            f"用户信息:\n"
            f"用户ID:<code> {user.tg_id}</code>\n"
            f"用户名: <a href='tg://user?id={user.tg_id}'>@{full_name}</a>\n"
            f"用户余额: <b>{user.money:.2f}</b>"
        )
        bot.reply_to(message, text, parse_mode="HTML")
    except TgUser.DoesNotExist:
        bot.reply_to(message, "未找到对应的用户信息，请检查输入的用户ID。")
        return

    except ValueError:
        bot.reply_to(message, "输入的用户ID无效，请确保输入的是正确的数字格式。")
        return
    except Exception as e:
        bot.reply_to(message, f"查询时发生未知错误: {e}")
        return


from decimal import Decimal, InvalidOperation


def handle_money_change(message, operation):
    """
    通用函数处理加分或减分操作。
    operation: "+" 表示加分, "-" 表示减分
    """
    chat_id = message.chat.id
    admin_id = message.from_user.id

    # 验证是否为工作群
    work_group_id = get_work_group_id()
    if work_group_id != str(chat_id):
        return

    # 验证管理员权限
    try:
        admin = TgUser.objects.get(tg_id=admin_id)
        if not admin.is_admin:
            return
    except TgUser.DoesNotExist:
        return

    try:
        # 使用正则提取 tg_id 和金额
        match = re.fullmatch(r"(加分|减分) (\d{5,15}) (\d+(\.\d{1,2})?)", message.text)
        if not match:
            bot.reply_to(message, "输入格式错误，请确保格式为: 加分/减分 tg_id 金额")
            return

        tg_id = int(match.group(2))  # 提取 tg_id
        amount_str = match.group(3)  # 提取金额字符串

        # 转换金额为 Decimal 类型并验证
        try:
            amount = Decimal(amount_str)
            if amount <= 0:
                bot.reply_to(message, "金额必须大于 0，请重新输入正确的金额。")
                return
        except InvalidOperation:
            bot.reply_to(message, "金额格式错误，请确保输入一个有效的数字金额。")
            return

        # 查询目标用户信息
        try:
            user = TgUser.objects.get(tg_id=tg_id)
        except TgUser.DoesNotExist:
            bot.reply_to(message, f"用户ID {tg_id} 不存在，请检查输入的 tg_id 是否正确。")
            return

        # 获取 Telegram 用户名
        try:
            user_name = bot.get_chat(tg_id)
            full_name = f"{user_name.first_name} {user_name.last_name if user_name.last_name else ''}".strip()
        except Exception:
            full_name = "未知用户"

        # 更新用户余额
        if operation == "+":
            user.money += amount
            change_type = "加分"
        elif operation == "-":
            user.money -= amount
            change_type = "减分"

        user.save()
        # 回复成功消息
        text = (
            f"{change_type}成功:\n"
            f"用户ID: <code>{user.tg_id}</code>\n"
            f"用户名: <a href='tg://user?id={user.tg_id}'>@{full_name}</a>\n"
            f"用户余额: <b>{user.money:.2f}</b>"
        )
        bot.reply_to(message, text, parse_mode="HTML")

    except Exception as e:
        bot.reply_to(message, f"处理 {change_type} 命令时发生未知错误: {e}")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"加分 \d{5,15} \d+(\.\d{1,2})?", message.text) and message.chat.type in ["group",
                                                                                                                "supergroup"]
)
def handle_bonus_command(message):
    handle_money_change(message, "+")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"减分 \d{5,15} \d+(\.\d{1,2})?", message.text) and message.chat.type in ["group",
                                                                                                                "supergroup"]
)
def handle_deduction_command(message):
    '''
    工作群管理员给用户加减分
    '''
    handle_money_change(message, "-")


@bot.message_handler(func=lambda message: message.text == '茶社大群' and message.chat.type == 'private')
def get_teahouse(message):
    url = 'https://t.me/chashe666666'
    name = "@" + url.split("/")[-1]
    text = f"<a href='{url}'>{name}</a>"
    bot.reply_to(message, text, parse_mode="HTML")


@bot.message_handler(func=lambda message: message.text == '供需发布' and message.chat.type == 'private')
def get_release(message):
    url = "https://t.me/chashe1_Bot"
    text = f"<a href='{url}'>@chashe1_Bot</a>"
    bot.reply_to(message, text, parse_mode="HTML")


@bot.message_handler(func=lambda message: message.text == '供需频道' and message.chat.type == 'private')
def get_gxpd(message):
    url = "https://t.me/chashe0"
    text = f"<a href='{url}'>@chashe0</a>"
    bot.reply_to(message, text, parse_mode="HTML")


@bot.message_handler(func=lambda message: message.text == 'TRX兑换' and message.chat.type == 'private')
def get_trx(message):
    url = "https://t.me/AutoTronTRXbot"
    text = f"<a href='{url}'>@AutoTronTRXbot</a>"
    bot.reply_to(message, text, parse_mode="HTML")


@bot.message_handler(func=lambda message: message.text == '每日抽奖' and message.chat.type == 'private')
def get_lottery(message):
    user = get_user(message.from_user)
    text = (
        "<b>参与每日抽奖规则：</b>\n\n"
        "1. 每天可使用 <b>10积分</b> 参与抽奖。\n"
        "2. 当奖池达到 <b>100积分</b> 或以上时，将在每天晚上 <b>22:00</b> 公布中奖信息。\n"
        "3. 每次抽奖将随机选出 <b>1位中奖者</b>，中奖者将获得奖池中的积分。\n"
        "4. 如果当天奖池未达到 <b>100积分</b>，则当天不开奖，所有参与用户会自动进入下次开奖。\n"
        "5. <b>中奖积分自动发放</b>\n\n"
        "点击下方<b>参与抽奖</b>按钮，即可参与"
    )

    markup = types.InlineKeyboardMarkup()
    button = types.InlineKeyboardButton(text="参与抽奖", callback_data="userDailyDraw")
    markup.add(button)

    bot.send_message(message.chat.id, text, parse_mode="HTML", reply_markup=markup)


@bot.message_handler(func=lambda message: message.text == '充值积分' and message.chat.type == 'private')
def get_recharge(message):
    markup = types.InlineKeyboardMarkup()
    buttons = [
        types.InlineKeyboardButton(text="100U", callback_data="user_recharge_100"),
        types.InlineKeyboardButton(text="200U", callback_data="user_recharge_200"),
        types.InlineKeyboardButton(text="300U", callback_data="user_recharge_300"),
        types.InlineKeyboardButton(text="500U", callback_data="user_recharge_500"),
        types.InlineKeyboardButton(text="1000U", callback_data="user_recharge_1000"),
        types.InlineKeyboardButton(text="2000U", callback_data="user_recharge_2000"),
    ]
    markup.add(*buttons)

    user = get_user(message.from_user)

    text = (
        f"TGID: <code>{user.tg_id}</code>\n"
        f"用户名: {user.tg_name}\n"
        f"用户积分: {user.money}\n"
        "请选择充值的金额:\n\n"
        "<b>100 500 1000</b> 该档位+百分之10代收费\n\n"
        "<b>2000 3000 5000</b> 该档位免收代收费\n\n"
        "<b>10000 20000</b> 该档位免代收费并加送百分之10充值积分\n\n"
        "充值USDT直接点击下方充值（充值免代收费,超过500USDT加送百分之10充值积分）"
    )

    bot.send_message(message.chat.id, text, reply_markup=markup, parse_mode="HTML", disable_web_page_preview=True)


@bot.message_handler(func=lambda message: message.text == '个人中心' and message.chat.type == 'private')
def get_personal_center(message):
    tg_id = message.from_user.id
    username = message.from_user.first_name + (message.from_user.last_name or "")

    user = get_user(message.from_user)

    text = (
        "个人中心\n"
        f"用户名: <a href='tg://user?id={tg_id}'>{username}</a>\n"
        f"ID: <code>{tg_id}</code>\n"
        f"积分: {user.money}"
    )
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton(text="提现", callback_data="userTakeoutMoney"),
               types.InlineKeyboardButton(text="提现记录", callback_data="selectTakeoutMoney"))
    markup.add(types.InlineKeyboardButton(text="领取红包记录", callback_data="selectReceiveLottery"),
               types.InlineKeyboardButton(text="抽奖记录", callback_data="selectUserluckydrawInfo"))
    markup.add(types.InlineKeyboardButton(text="转账记录", callback_data="userTransferLog"),types.InlineKeyboardButton(text="收款记录", callback_data="userReceiptLog"))
    bot.send_message(message.chat.id, text, parse_mode="HTML", reply_markup=markup)


def parse_uuid(name, message):
    # 动态生成正则表达式，name 是命令名
    regex = r"^/" + re.escape(name) + r"\s+([a-f0-9\-]{36})$"

    # 使用正则表达式匹配消息
    match = re.match(regex, message)
    if match:
        uuid = match.group(1)  # 提取 UUID
        return uuid
    else:
        return None


def is_user_member(tg_id, link: str) -> bool:
    """
    检查用户是否是频道|群聊成员。
    """
    link = '@' + link.split("/")[-1]
    chat = bot.get_chat(chat_id=link)
    try:
        member = bot.get_chat_member(chat_id=chat.id, user_id=tg_id)
        if member.status != "left":
            return True
        else:
            markup = types.InlineKeyboardMarkup()
            markup.add(types.InlineKeyboardButton(text="加入", url=chat.invite_link))
            bot.send_message(chat_id=tg_id, text="请加入指定群聊/频道, 再抢红包", reply_markup=markup)
            return False
    except Exception as e:
        return False


@bot.message_handler(commands=['start'], func=lambda message: message.chat.type == 'private')
def get_start(message):
    text = message.text
    user = get_user(message.from_user)
    if text == "/start":
        bot.send_message(message.chat.id, "hello", reply_markup=get_start_markup())
        return
    lottery_id = parse_uuid('start', text)
    if uuid is None:
        return
    lottery = TgLottery.objects.filter(lottery_id=lottery_id, status=0).first()
    if lottery is None:
        bot.send_message(message.chat.id, "抽奖已结束")
        return
    # 判断是否有条件
    if lottery.link_type == 1:
        if not is_user_member(message.from_user.id, lottery.link):
            return
    elif lottery.link_type == 2:
        if not is_user_member(message.from_user.id, lottery.link):
            return
    # 判断用户是否抽过了
    if TgLotteryInfo.objects.filter(tg_id=message.from_user.id, lottery_id=lottery_id).exists():
        bot.send_message(message.chat.id, "你已经参加了这个抽奖")
        return
    # 随机true或false
    result = random.choice([True, False])

    lottery_info = TgLotteryInfo(
        lottery_info_id=str(uuid.uuid4()),
        lottery_id=lottery_id,
        lottery_create_tg_id=lottery.tg_id,
        tg_id=message.from_user.id,
        tg_name=message.from_user.first_name + (message.from_user.last_name or ""),
    )
    if result:
        prize_pool_id = get_random_money(lottery_id)
        if prize_pool_id is None:
            lottery_info.status = -1
            bot.send_message(message.chat.id, "抽奖已结束")
        else:
            prize_pool = TgPrizePool.objects.filter(prize_pool_id=prize_pool_id, status=0).first()
            if prize_pool:
                prize_pool.update_time = datetime.now()
                prize_pool.status = 1
                prize_pool.save()
                lottery_info.prize_pool_id = prize_pool_id
                lottery_info.money = prize_pool.money
                message_text = f"恭喜你，你得到了 <b>{prize_pool.money}</b>!\n保存抽奖 id:\n<code>{lottery_info.lottery_info_id}</code>\n\n请联系<a href=\"tg://user?id={lottery.tg_id}\">@{lottery.tg_name}</a>并发送您的中奖ID以索取奖励。"
                bot.send_message(message.chat.id, message_text, parse_mode="HTML")
        lottery_info.save()
    else:
        lottery_info.status = -1
        lottery_info.save()
        bot.send_message(message.chat.id, "很遗憾，你没有中奖")


@bot.message_handler(commands=['view'], func=lambda message: message.chat.type == 'private')
def get_view(message):
    text = message.text
    lottery_id = parse_uuid('view', text)
    if lottery_id is None:
        return
    lottery = TgLottery.objects.filter(lottery_id=lottery_id, tg_id=message.from_user.id).first()
    if lottery is None:
        bot.send_message(message.chat.id, "此抽奖不是您创建的")
        return
    lottery_info_list = TgLotteryInfo.objects.filter(lottery_id=lottery_id, status__in=[0, 1]).order_by('-money')
    message_text = query_winning_message(lottery, lottery_info_list)
    bot.send_message(message.chat.id, message_text, parse_mode="HTML")


@bot.message_handler(commands=['exchange'], func=lambda message: message.chat.type == 'private')
def get_exchange(message):
    text = message.text
    lottery_info_id = parse_uuid('exchange', text)
    if lottery_info_id is None:
        return
    lottery_info = TgLotteryInfo.objects.filter(lottery_info_id=lottery_info_id,
                                                lottery_create_tg_id=message.from_user.id).first()
    if lottery_info is None:
        bot.send_message(message.chat.id, "此中奖记录不是您创建的")
        return
    if lottery_info.status == 1:
        bot.send_message(message.chat.id, "此中奖记录已兑换了")
        return
    # 给中奖用户兑换积分
    lottery_info.status = 1
    lottery_info.update_time = datetime.now()
    lottery_info.save()

    user = TgUser.objects.filter(tg_id=lottery_info.tg_id).first()
    user.money += lottery_info.money
    user.save()
    url = f'<a href="tg://user?id={lottery_info.tg_id}">@{lottery_info.tg_name}</a>'
    message_text = (
        f"用户id: {lottery_info.tg_id}\n"
        f"用户名: {url}\n"
        f"抽中: <b>{lottery_info.money}</b>\n\n"
        f"核销成功"
    )
    bot.send_message(message.chat.id, message_text, parse_mode="HTML")


@bot.message_handler(commands=['help'], func=lambda message: message.chat.type == 'private')
def get_help(message):
    text = (
        "\U0001F916 欢迎使用茶社抽奖Bot，命令说明\n"
        "/start 抽奖id -开始抽奖\n"
        "/view 抽奖id -创建抽奖者查看中奖用户\n"
        "/exchange 中奖id -创建抽奖者核销中奖者奖励\n\n"
        "功能:\n"
        "<b>创建抽奖者</b>\n"
        "1. 发送<b>gift 积分 个数</b> 创建积分红包抽奖\n"
        "2. 创建红包抽奖后可选择抽奖条件(必须加入群聊|订阅频道)才能抽奖\n"
        "<b>抽奖条件注意:</b>\n"
        "<b>⚠️ 选择加入群聊 需把机器人拉到群聊并设置管理员,群聊必须设置公开群聊.否则抽奖无效</b>\n"
        "<b>⚠️ 选择订阅频道 需把机器人拉到频道并设置管理员.否则抽奖无效</b>\n"
        "3. 将抽奖信息转发到<b>频道|群聊</b>成员进行抽奖\n"
        "4. 抽奖结束后，抽奖者可查看中奖用户，并核销奖励\n\n"
        "<b>中奖用户</b>\n"
        "1. 保存中奖id并及时找<b>创建抽奖者</b>核销,核销成功后转成积分\n"
        "2. 可在内置键盘中的个人中心<b>提现</b>和查看中奖记录\n"
    )
    bot.send_message(message.chat.id, text, parse_mode="HTML")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"^gift\s+(\d+(\.\d+)?)\s+(\d+)", message.text) and message.chat.type == 'private'
)
def get_gift(message):
    match = re.fullmatch(r"^gift\s+(\d+(\.\d+)?)\s+(\d+)", message.text)
    user = get_user(message.from_user)
    if match:
        # 将积分转换为 Decimal 类型，确保支持整数和浮动数字
        money = Decimal(match.group(1)).quantize(Decimal('0.01'))  # 保留两位小数
        quantity = int(match.group(3))  # 获取个数

        # 检查用户是否有足够的金额
        if user.money < money:
            bot.send_message(message.chat.id,
                             f'创建抽奖失败\n您当前积分: <b>{user.money}</b>\n积分不足\n请充值后使用',
                             parse_mode="HTML")
            return

        # 处理发送红包的逻辑
        user.money -= money
        user.save()

        lottery = TgLottery(
            lottery_id=str(uuid.uuid4()),
            tg_id=message.from_user.id,
            tg_name=f"{message.from_user.first_name} {message.from_user.last_name or ''}",
            chat_id=message.chat.id,
            money=money,
            number=quantity
        )

        lottery.save()
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton(text="无需条件", callback_data=f"none:{lottery.lottery_id}"))
        markup.add(types.InlineKeyboardButton(text="加入群聊", callback_data=f"group:{lottery.lottery_id}"))
        markup.add(types.InlineKeyboardButton(text="订阅频道", callback_data=f"cancel:{lottery.lottery_id}"))
        bot.send_message(message.chat.id,
                         "请选择需要设置抢红包的条件",
                         parse_mode="HTML", reply_markup=markup)
    else:
        bot.send_message(message.chat.id,
                         "创建抽奖失败\n请输入正确的格式\n格式: gift 积分 个数",
                         parse_mode="HTML")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"^adminGift\s+(\d+)\s+(\d+)\s+(\d+)$",
                                      message.text) and message.chat.type == 'private'
)
def get_gift(message):
    match = re.fullmatch(r"^adminGift\s+(\d+)\s+(\d+)\s+(\d+)$", message.text)
    user = get_user(message.from_user)
    if not user.is_admin:
        return
    if match:
        # 将积分转换为 Decimal 类型，确保支持整数和浮动数字
        money = Decimal(match.group(1)).quantize(Decimal('0.01'))  # 保留两位小数
        virtua_money = match.group(2)
        quantity = int(match.group(3))  # 获取个数

        # 检查用户是否有足够的金额
        if user.money < money:
            bot.send_message(message.chat.id,
                             f'创建抽奖失败\n您当前积分: <b>{user.money}</b>\n积分不足\n请充值后使用',
                             parse_mode="HTML")
            return

        # 处理发送红包的逻辑
        user.money -= money
        user.save()

        lottery = TgLottery(
            lottery_id=str(uuid.uuid4()),
            tg_id=message.from_user.id,
            tg_name=f"{message.from_user.first_name} {message.from_user.last_name or ''}",
            chat_id=message.chat.id,
            money=money,
            virtua_money=virtua_money,
            number=quantity
        )

        lottery.save()
        markup = types.InlineKeyboardMarkup()
        markup.add(types.InlineKeyboardButton(text="无需条件", callback_data=f"none:{lottery.lottery_id}"))
        markup.add(types.InlineKeyboardButton(text="加入群聊", callback_data=f"group:{lottery.lottery_id}"))
        markup.add(types.InlineKeyboardButton(text="订阅频道", callback_data=f"cancel:{lottery.lottery_id}"))
        bot.send_message(message.chat.id,
                         "请选择需要设置抢红包的条件",
                         parse_mode="HTML", reply_markup=markup)
    else:
        bot.send_message(message.chat.id,
                         "创建抽奖失败\n请输入正确的格式\n格式: gift 积分 个数",
                         parse_mode="HTML")
