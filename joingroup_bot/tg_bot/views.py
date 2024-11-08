import telebot
from django.http import JsonResponse
from django.views import View
from .bot_config import bot
import json
import tg_bot.bot_message_handler
import tg_bot.bot_callback_handler
import tg_bot.database.Tables
from django.db import connection
from .database.Tables import TgRecharge, TgUser


def execute_custom_sql(query):
    with connection.cursor() as cursor:
        cursor.execute(query)
        result = cursor.fetchall()
    return result


class BotView(View):
    def post(self, request, *args, **kwargs):
        json_str = request.body.decode('UTF-8')
        update = telebot.types.Update.de_json(json_str)
        bot.process_new_updates([update])
        return JsonResponse({'status': 'ok'})


class RechargeView(View):
    def post(self, request):
        data = json.loads(request.body)
        from_account = data.get('from')
        to_account = data.get('to')
        amount = data.get('amount')
        transaction_time = data.get('transactionTime')
        tx_id = data.get('txId')

        print(data)

        result = execute_custom_sql(
            f"SELECT * FROM tg_recharge WHERE create_time >= NOW() - INTERVAL '15 minutes' and money = {amount} and status = 0")
        results = []
        for row in result:
            recharge = TgRecharge(
                recharge_id=row[0],
                tg_id=row[1],
                money=row[2],
                status=row[3],
                create_time=row[4],
                update_time=row[5],
            )
            results.append(recharge)
        if results:
            des = '充值成功'
            data = results[0]
            user = TgUser.objects.get(tg_id=data.tg_id)
            with connection.cursor() as cursor:
                cursor.execute(f"update tg_recharge set status = 1 where recharge_id = {data.recharge_id}")
            user.money += data.money
            user.save()
            bot.send_message(data.tg_id, f'充值成功\n当前余额:<b>{user.money}</b>', parse_mode='html')
        else:
            des = '没有找到对应的充值记录'
        return JsonResponse({'status': 'ok', 'des': des})
