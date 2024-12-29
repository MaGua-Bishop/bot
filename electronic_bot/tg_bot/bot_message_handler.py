from time import localtime

from telebot import types
from django.db.models import Sum, Value, DecimalField
from django.db.models.functions import Coalesce
from django.utils import timezone
from datetime import timedelta
from django.utils.timezone import localtime

from .bot_config import bot
from .utlis import get_start_reply_markup, create_game_user, get_game_url, get_user_pgmoney, set_work_group_id, \
    get_work_group_id
from .models import TgUser, AmountChange, GameHistory, TgRecharge

commands = [
    types.BotCommand("start", "🏠启动机器人"),
    types.BotCommand("help", "❔︎帮助"),
    types.BotCommand("support", "🙋客服支持")
]
bot.set_my_commands(commands, scope=types.BotCommandScopeAllPrivateChats())


@bot.message_handler(commands=['start'], func=lambda message: message.chat.type == 'private')
def start_message(message):
    user_id = message.from_user.id
    full_name = message.from_user.full_name
    user, created = TgUser.objects.get_or_create(tg_id=user_id)
    if created:
        user.money = 0
        is_pg_success = create_game_user(tg_id=user_id, type=0)
        is_jdb_success = create_game_user(tg_id=user_id, type=1)
        if is_pg_success and is_jdb_success:
            user.pg_player_id = "tg" + str(user_id)[:9]
            user.jdb_player_id = "tg" + str(user_id)[:9]
            user.save()
        else:
            bot.send_message(message.chat.id, "❌ 创建游戏用户失败，请重新/start")
            return
        try:
            if message.text.startswith('/start') and len(message.text.split()) > 1:
                invite_tg_id = int(message.text.split()[1])
                user.invite_tg_id = invite_tg_id
                print(invite_tg_id)
        except (ValueError, AttributeError):
            user.invite_tg_id = None
        user.save()
    text = f"👋Hi,<a href='https://t.me/{user_id}'>{full_name}</a> 🆔<code> {user_id}</code>\n-----------------------------------------\n💰钱包余额: {user.money} "
    try:
        bot.send_message(message.chat.id, text, parse_mode="HTML", reply_markup=get_start_reply_markup())
    except Exception as e:
        print(e)


@bot.message_handler(commands=['help'], func=lambda message: message.chat.type == 'private')
def start_message(message):
    text = f"👩‍💻客服支持<a href='https://t.me/dhkf9'>@鼎豪客服 阿伟</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.send_message(chat_id=message.chat.id, text=text,
                     reply_markup=markup, parse_mode="HTML")


@bot.message_handler(commands=['support'], func=lambda message: message.chat.type == 'private')
def start_message(message):
    text = f"👩‍💻客服支持<a href='https://t.me/dhkf9'>@鼎豪客服 阿伟</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.send_message(chat_id=message.chat.id, text=text,
                     reply_markup=markup, parse_mode="HTML")


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


import re


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
            f"钱包余额: <b>{user.money:.2f}</b>"
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
        before_amount = user.money
        if operation == "+":
            user.money += amount
            change_type = "加分"
        elif operation == "-":
            user.money -= amount
            change_type = "减分"

        user.save()

        # 记录操作
        AmountChange.objects.create(
            user=user,
            change_type=operation,
            name='福利',
            change_amount=amount,
            before_amount=before_amount,
            after_amount=user.money
        )

        # 回复成功消息
        text = (
            f"{change_type}成功:\n"
            f"用户ID: <code>{user.tg_id}</code>\n"
            f"用户名: <a href='tg://user?id={user.tg_id}'>@{full_name}</a>\n"
            f"{change_type}前余额: <b>{before_amount:.2f}</b>\n"
            f"{change_type}金额: <b>{amount:.2f}</b>\n"
            f"钱包余额: <b>{user.money:.2f}</b>"
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
    handle_money_change(message, "-")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"充值记录 \d{5,15}", message.text) and message.chat.type in ["group",
                                                                                                    "supergroup"]
)
def handle_recharge_record(message):
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

    tgid = message.text.split()[1]
    try:
        recharge_records = TgRecharge.objects.filter(tg_id=tgid, status=1).order_by('-create_time')
        if not recharge_records:
            bot.reply_to(message, f"用户ID {tgid} 没有充值记录。")
            return

        # 统计总金额
        total_amount = sum(record.money for record in recharge_records)

        text = "\n".join(
            [
                f"金额:<b>{record.money:.2f} </b>\n充值时间:<b>{localtime(record.update_time).strftime('%Y-%m-%d %H:%M:%S')}</b>\n"
                for record in recharge_records
            ]
        )

        bot.reply_to(message, f"用户ID {tgid} 的充值记录:\n{text}\n总充值金额: <b>{total_amount:.2f} USDT</b>",
                     parse_mode="HTML")
    except Exception as e:
        bot.reply_to(message, f"查询充值记录时发生错误: {e}")
        return


@bot.message_handler(func=lambda message: message.text == "流水" and (
        message.chat.type == "private" or message.chat.id == -1002288238505))
def user_query_history(message):
    user_id = message.from_user.id
    full_name = message.from_user.full_name
    player_id = "tg" + str(user_id)[:9]

    try:
        user = TgUser.objects.get(tg_id=user_id)
    except TgUser.DoesNotExist:
        bot.reply_to(message, "用户信息未找到，请确保您已注册。")
        return
    except Exception as e:
        return

    today = timezone.now().date()
    yesterday = today - timedelta(days=1)

    try:
        # 查询今天的历史记录
        history_today = GameHistory.objects.filter(player_id=player_id, bet_time__date=today)
        totals_today = history_today.aggregate(
            total_settled_amount=Coalesce(Sum('settled_amount', output_field=DecimalField()),
                                          Value(0, output_field=DecimalField())),
            total_valid_amount=Coalesce(Sum('valid_amount', output_field=DecimalField()),
                                        Value(0, output_field=DecimalField()))
        )
        total_settled_amount_today = totals_today['total_settled_amount']
        total_valid_amount_today = totals_today['total_valid_amount']

        # 查询前一天的历史记录
        history_yesterday = GameHistory.objects.filter(player_id=player_id, bet_time__date=yesterday)
        totals_yesterday = history_yesterday.aggregate(
            total_valid_amount=Coalesce(Sum('valid_amount', output_field=DecimalField()),
                                        Value(0, output_field=DecimalField()))
        )
        total_valid_amount_yesterday = totals_yesterday['total_valid_amount']
        text = (
            f"Hi ,<a href='https://t.me/{user_id}'>{full_name}</a>ID: <code>{user_id}</code>\n"
            f"💵余额 :{user.money:.2f} \n"
            f"(如果余额在游戏平台,需要转回钱包才可以显示哦~)\n"
            f"🔸今日老虎机流水：{total_valid_amount_today:.2f}\n"
            f"🔹昨日老虎机流水：{total_valid_amount_yesterday:.2f}\n"
            f"(💡流水更新大约有十分钟延迟哦~)\n"
            f"🔸今日输赢：{total_settled_amount_today}\n"
            f"🔹注册时间：{localtime(user.create_time).strftime('%Y-%m-%d %H:%M:%S')}"
        )
        markup = types.InlineKeyboardMarkup()
        url = f"https://t.me/{bot.get_me().username}?start"
        markup.add(types.InlineKeyboardButton("🎰立即开玩", url=url))
        markup.add(types.InlineKeyboardButton("分享给好友获得Ta的下注奖励", switch_inline_query="Invite"))
        bot.reply_to(message, text, parse_mode="HTML", reply_markup=markup)
    except Exception as e:
        bot.reply_to(message, f"查询失败，请重试")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"玩家流水 \d{5,15}", message.text) and message.chat.type in ["group",
                                                                                                    "supergroup"]
)
def admin_query_user_history(message):
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

    tg_id = message.text.split()[1]
    # 获取用户名
    try:
        user_name = bot.get_chat(tg_id)
        full_name = f"{user_name.first_name} {user_name.last_name if user_name.last_name else ''}".strip()
    except Exception:
        full_name = "未知用户"
    user_id = tg_id
    player_id = "tg" + str(user_id)[:9]

    try:
        user = TgUser.objects.get(tg_id=user_id)
    except TgUser.DoesNotExist:
        bot.reply_to(message, f"用户信息未找到，请确认用户ID是否正确。")
        return
    except Exception as e:
        return

    today = timezone.now().date()
    yesterday = today - timedelta(days=1)

    try:
        # 查询今天的历史记录
        history_today = GameHistory.objects.filter(player_id=player_id, bet_time__date=today)
        totals_today = history_today.aggregate(
            total_settled_amount=Coalesce(Sum('settled_amount', output_field=DecimalField()),
                                          Value(0, output_field=DecimalField())),
            total_valid_amount=Coalesce(Sum('valid_amount', output_field=DecimalField()),
                                        Value(0, output_field=DecimalField()))
        )
        total_settled_amount_today = totals_today['total_settled_amount']
        total_valid_amount_today = totals_today['total_valid_amount']

        # 查询前一天的历史记录
        history_yesterday = GameHistory.objects.filter(player_id=player_id, bet_time__date=yesterday)
        totals_yesterday = history_yesterday.aggregate(
            total_valid_amount=Coalesce(Sum('valid_amount', output_field=DecimalField()),
                                        Value(0, output_field=DecimalField()))
        )
        total_valid_amount_yesterday = totals_yesterday['total_valid_amount']
        text = (
            f"<a href='tg://user?id={user_id}'>@{full_name}</a>ID: <code>{user_id}</code>\n"
            f"💵余额 :{user.money:.2f} \n"
            f"(如果余额在游戏平台,需要转回钱包才可以显示哦~)\n"
            f"🔸今日老虎机流水：{total_valid_amount_today:.2f}\n"
            f"🔹昨日老虎机流水：{total_valid_amount_yesterday:.2f}\n"
            f"(💡流水更新大约有十分钟延迟哦~)\n"
            f"🔸今日输赢：{total_settled_amount_today}\n"
            f"🔹注册时间：{localtime(user.create_time).strftime('%Y-%m-%d %H:%M:%S')}"
        )
        bot.reply_to(message, text, parse_mode='html')
    except Exception as e:
        bot.reply_to(message, f"发生错误: {str(e)}")


@bot.message_handler(
    func=lambda message: re.fullmatch(r"玩家邀请 \d{5,15}", message.text) and message.chat.type in ["group",
                                                                                                    "supergroup"]
)
def admin_query_user_invite(message):
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

    tg_id = message.text.split()[1]
    user_id = tg_id
    try:
        user = TgUser.objects.get(tg_id=user_id)
    except TgUser.DoesNotExist:
        bot.reply_to(message, "用户信息未找到，请确认用户ID是否正确。")
        return
    except Exception as e:
        bot.reply_to(message, f"发生错误: {str(e)}")
        return

    # 获取被邀请用户的列表
    invited_users = TgUser.objects.filter(invite_tg_id=user_id)
    invited_users_text = []

    for invited_user in invited_users:
        try:
            # 使用 get_chat 获取用户信息
            user_info = bot.get_chat(invited_user.tg_id)
            full_name = f"{user_info.first_name} {user_info.last_name if user_info.last_name else ''}".strip()
            # 创建可点击的链接
            invited_users_text.append(f"<a href='tg://user?id={invited_user.tg_id}'>@{full_name}</a>\t")
        except Exception as e:
            print(f"获取用户 {invited_user.tg_id} 的信息失败: {e}")

    invited_users_text = "\n".join(invited_users_text) if invited_users_text else "没有邀请任何用户"
    try:
        user_name = bot.get_chat(tg_id)
        full_name = f"{user_name.first_name} {user_name.last_name if user_name.last_name else ''}".strip()
    except Exception:
        full_name = "未知用户"
    text = (
        f"<a href='tg://user?id={user_id}'>@{full_name}</a>ID: <code>{user_id}</code>\n"
        f"👥 已邀请人数 : {len(invited_users)}\n"  # 使用 len(invited_users) 获取已邀请人数
        f"👥 已邀请用户 : \n{invited_users_text}\n"
    )

    bot.reply_to(message, text, parse_mode='HTML')


@bot.message_handler(func=lambda message: message.text == "反水" and message.chat.type == "private")
def user_betrayal(message):
    user_id = message.from_user.id
    player_id = "tg" + str(user_id)[:9]

    try:
        user = TgUser.objects.get(tg_id=user_id)
    except TgUser.DoesNotExist:
        bot.send_message(user_id, "用户信息未找到，请确保您已注册。")
        return
    except Exception as e:
        return

    try:
        history_today = GameHistory.objects.filter(player_id=player_id, is_status=False)
        totals_today = history_today.aggregate(
            total_valid_amount=Coalesce(Sum('valid_amount', output_field=DecimalField()),
                                        Value(0, output_field=DecimalField()))
        )
        total_valid_amount_today = totals_today['total_valid_amount']

        rebate_percentage = Decimal('0.008')  # 设定返水比例为 0.8%
        rebate_amount = total_valid_amount_today * rebate_percentage
        before_amount = user.money
        user.money += rebate_amount
        user.save()

        history_today.update(is_status=True)

        AmountChange.objects.create(
            user=user,
            change_type="+",
            name=f'反水({total_valid_amount_today}|{rebate_amount})',
            change_amount=rebate_amount,
            before_amount=before_amount,
            after_amount=user.money
        )
        bot.send_message(user_id, f"有效金额: {total_valid_amount_today:.2f} \n"
                                  f"返水金额: {rebate_amount:.2f}\n已发送到您的钱包。")
    except Exception as e:
        bot.send_message(user_id, f"发生错误: {str(e)}")
