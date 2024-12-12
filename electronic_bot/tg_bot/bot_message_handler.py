from telebot import types

from .bot_config import bot
from .utlis import get_start_reply_markup, create_game_user, get_game_url, get_user_pgmoney
from .models import TgUser

commands = [
    types.BotCommand("start", "Start Bot"),
    types.BotCommand("help", "Help"),
    types.BotCommand("support", "Support")
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
    text = f"👩‍💻 Support<a href='https://t.me/trx066'>@易水寒能量租赁，转账一笔2trx</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.send_message(chat_id=message.chat.id, text=text,
                     reply_markup=markup, parse_mode="HTML")


@bot.message_handler(commands=['support'], func=lambda message: message.chat.type == 'private')
def start_message(message):
    text = f"👩‍💻 Support<a href='https://t.me/trx066'>@易水寒能量租赁，转账一笔2trx</a>"
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton("🏠主菜单", callback_data="return_start"))
    bot.send_message(chat_id=message.chat.id, text=text,
                     reply_markup=markup, parse_mode="HTML")
